package org.swdc.archive.core;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.archive.core.steamed.TheTarFile;
import org.swdc.archive.core.steamed.TheTgzFile;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.ProgressView;
import org.swdc.fx.FXResources;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TarArchiver implements Archive<TarArchiveEntry,ArchiveEntry<TarArchiveEntry>> {

    protected File tarFile = null;

    protected TheTarFile tar = null;

    protected ArchiveView view;

    protected CommonService commonService;

    protected FXResources resources;

    protected Logger logger = LoggerFactory.getLogger(TarArchiver.class);

    public TarArchiver(FXResources resources,ArchiveView view,File file,CommonService commonService) {

        this.resources = resources;
        this.view = view;
        this.tarFile = file;
        this.commonService = commonService;

        try {
            tar = getTarFile(file);
            view.archiver(this);
        } catch (Exception e) {
            this.close();
            ResourceBundle bundle = resources.getResourceBundle();
            Platform.runLater(() -> {
                Alert alert = view.alert(
                        bundle.getString(ArchiveLangConstants.LangArchiveErrorTitle),
                        bundle.getString(ArchiveLangConstants.LangZipArchiveOpenFailed),
                        Alert.AlertType.ERROR);
                alert.showAndWait();
                view.getStage().close();
            });
            logger.error("failed to open tar archive file", e);
        }

    }

    protected TheTarFile getTarFile(File file) throws IOException {
        return new TheTarFile(file);
    }

    protected TarArchiveOutputStream createOutputStream(File file) throws IOException {
        return new TarArchiveOutputStream(new FileOutputStream(file));
    }

    protected String getExtension() {
        return "tar";
    }

    @Override
    public File getArchiveFile() {
        return tarFile;
    }

    @Override
    public void getDictionaryTree(Consumer<TreeItem<ArchiveEntry<TarArchiveEntry>>> consumer) {

        List<TarArchiveEntry> entries = tar.getEntries();

        ArchiveEntry<TarArchiveEntry> rootEntry = new ArchiveEntry<>(null);
        rootEntry.name(e -> tarFile.getName());

        Function<String,ArchiveEntry<TarArchiveEntry>> funcFolderResolve = (path) -> {
            if (path == null) {
                return rootEntry;
            }
            String[] parts = path.split("/");

            ArchiveEntry<TarArchiveEntry> target = rootEntry;
            for (String part: parts) {
                ArchiveEntry<TarArchiveEntry> folderPart = target.getFolder(part);
                if (folderPart == null) {
                    folderPart = new ArchiveEntry<>(null);
                    folderPart.name(e -> part);
                    folderPart.setParent(target);
                }
                target.addFolder(part,folderPart);
                target = folderPart;
            }
            return target;
        };

        for (TarArchiveEntry entry : entries) {

            if (entry.isDirectory()) {

                funcFolderResolve.apply(entry.getName());

            } else {

                String filePath = entry.getName();
                int last = filePath.lastIndexOf("/");
                String folderPath = last > -1 ? filePath.substring(0,last) : null;
                String fileName = last > -1 ? filePath.substring(last + 1): filePath;

                ArchiveEntry<TarArchiveEntry> folder = funcFolderResolve.apply(folderPath);
                ArchiveEntry<TarArchiveEntry> target = new ArchiveEntry<>(entry);
                target.name(e -> fileName);
                target.size(TarArchiveEntry::getSize);
                folder.addFile(fileName,target);
                target.setParent(folder);

            }

        }

        TreeItem<ArchiveEntry<TarArchiveEntry>> root = new TreeItem<>(rootEntry);
        UIUtils.createTree(root,rootEntry);
        consumer.accept(root);
    }

    @Override
    public void extract(List<ArchiveEntry<TarArchiveEntry>> extract, File target, BiConsumer<String, Double> progressCallback) {
        for (int idx = 0; idx < extract.size(); idx ++) {
            ArchiveEntry<TarArchiveEntry> entry = extract.get(idx);
            TarArchiveEntry tarArchiveEntry = entry.getEntry();
            try {
                if (tarArchiveEntry != null) {
                    File outFile = getExtractTargetFile(entry,target);
                    progressCallback.accept(outFile.getAbsolutePath(), ((double)idx) / extract.size());
                    InputStream inputStream = tar.createInputStream(tarArchiveEntry);
                    FileOutputStream outputStream = new FileOutputStream(outFile);
                    byte[] buf = new byte[1024 * 1024];
                    double len = tarArchiveEntry.getSize();
                    double read = 0;
                    double curr = 0;
                    while ((read = inputStream.read(buf)) != -1) {
                        outputStream.write(buf);
                        curr = curr + read;
                        progressCallback.accept(entry.name(),curr / len);
                    }
                    inputStream.close();
                    outputStream.close();
                }
            } catch (Exception e) {
               logger.error("error on extract file", e);
            }
            if (entry.getFiles() != null && !entry.getFiles().isEmpty()) {
                extract(entry.getFiles(),target,progressCallback);
            }
            if (entry.getChildrenFolder() != null && !entry.getChildrenFolder().isEmpty()) {
                extract(entry.getChildrenFolder(),target,progressCallback);
            }
        }
    }

    @Override
    public void addEntry(ArchiveEntry<TarArchiveEntry> targetFolderEntry, File item) {

        view.getView().setDisable(true);
        ResourceBundle bundle = resources.getResourceBundle();

        commonService.submit(() -> {
            ProgressView progressView = view.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                    bundle.getString(ArchiveLangConstants.LangArchivePrepareAdding),
                    0
            );
            progressView.show();

            try {
                File out = new File(tarFile.getAbsolutePath() + ".tmp");
                TarArchiveOutputStream tos = createOutputStream(out);
                List<TarArchiveEntry> entries = tar.getEntries();

                double size = entries.size();
                double curr = 0;

                for (TarArchiveEntry e: entries) {
                    tos.putArchiveEntry(e);
                    tos.write(tar.createInputStream(e).readAllBytes());
                    tos.closeArchiveEntry();
                    curr ++;
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                            e.getName(),
                            curr / size
                    );
                }
                String path = UIUtils.generateFileInArchivePath(targetFolderEntry,getExtension(),item);
                FileInputStream fileInputStream = new FileInputStream(item);

                var entry = tos.createArchiveEntry(item,path);
                tos.putArchiveEntry(entry);
                tos.write(fileInputStream.readAllBytes());
                tos.closeArchiveEntry();
                tos.close();

                fileInputStream.close();

                this.close();
                tarFile.delete();
                out.renameTo(tarFile);
                tar = new TheTarFile(tarFile);

                Platform.runLater(() -> {
                    view.getView().setDisable(false);
                    view.reload();
                });
            } catch (Exception e) {
                logger.error("error on add new archive entry",e);
            } finally {
                Platform.runLater(() -> {
                    view.getView().setDisable(false);
                });
                progressView.hide();
            }
        });


    }


    @Override
    public void removeEntry(List<ArchiveEntry<TarArchiveEntry>> removeEntities) {

        ResourceBundle bundle = resources.getResourceBundle();
        view.getView().setDisable(true);
        commonService.submit(() -> {

            ProgressView progressView = view.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                    bundle.getString(ArchiveLangConstants.LangArchivePrepareRemoving),
                    0
            );
            progressView.show();


            List<String> archiveEntries = removeEntities.stream()
                    .map(e -> e.getEntry() != null ? e.getEntry().getName() :
                            UIUtils.generateEntryInArchivePath(e,getExtension()))
                    .distinct()
                    .collect(Collectors.toList());

            try {
                File out = new File(tarFile.getAbsolutePath() + ".tmp");
                TarArchiveOutputStream tos = createOutputStream(out);
                List<TarArchiveEntry> entries = tar.getEntries();

                double size = entries.size() - removeEntities.size();
                double curr = 0;

                for (TarArchiveEntry e: entries) {
                    if (archiveEntries.contains(e.getName())) {
                        continue;
                    }
                    tos.putArchiveEntry(e);
                    tos.write(tar.createInputStream(e).readAllBytes());
                    tos.closeArchiveEntry();
                    curr ++;
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                            e.getName(),
                            curr / size
                    );
                }

                tos.close();

                this.close();
                tarFile.delete();

                out.renameTo(tarFile);
                tar = getTarFile(tarFile);

                Platform.runLater(() -> {
                    view.getView().setDisable(false);
                    view.reload();
                });
            } catch (Exception e) {
                logger.error("error on add new archive entry",e);
            } finally {
                Platform.runLater(() -> {
                    view.getView().setDisable(false);
                });
                progressView.hide();
            }
        });

    }

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public boolean exist() {
        return false;
    }

    @Override
    public void saveAs(File file) {

    }

    @Override
    public void saveFile() {

    }

    @Override
    public InputStream getInputStream(ArchiveEntry<TarArchiveEntry> entry) {
        if (entry != null && entry.getEntry() != null) {
            try {
                return tar.createInputStream(entry.getEntry());
            } catch (Exception e){
                return null;
            }
        }
        return null;
    }

    @Override
    public void close() {
        try {
            if (this.tar != null) {
                this.tar.close();
            }
        } catch (Exception e) {
            logger.error("failed on closing tar file",e);
        }
    }

}
