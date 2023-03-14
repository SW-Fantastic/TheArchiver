package org.swdc.archive.core;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.ProgressView;
import org.swdc.fx.FXResources;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ZipArchiver implements Archive<ZipArchiveEntry,ArchiveEntry<ZipArchiveEntry>> {

    private File file;

    public ArchiveView archiveView;

    private ZipFile zipFile;

    private CommonService commonService;

    private int counts = 0;

    private FXResources resources;

    private static Logger logger = LoggerFactory.getLogger(ZipArchiver.class);

    public ZipArchiver(File file, ArchiveView archiveView, CommonService commonService, FXResources resources) {

        this.file = file;
        this.archiveView = archiveView;
        this.commonService = commonService;
        this.resources = resources;

        ResourceBundle bundle = resources.getResourceBundle();

        if (file != null) {
            try {

                this.zipFile = new ZipFile(file);

                UniversalDetector universalDetector = new UniversalDetector(null);
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] data = new byte[2048];
                int offset = 0;
                int readed = 0;
                while ((readed = fileInputStream.read(data)) > 0 && universalDetector.isDone()) {
                    universalDetector.handleData(data,0,readed);
                    offset = offset + readed;
                }

                universalDetector.dataEnd();


                this.archiveView.archiver(this);

            } catch (Exception e) {
                logger.error("failed to open zip archive file.", e);
                this.close();
                Platform.runLater(() -> {
                    archiveView.alert(
                            bundle.getString(ArchiveLangConstants.LangArchiveErrorTitle),
                            bundle.getString(ArchiveLangConstants.LangZipArchiveOpenFailed),
                            Alert.AlertType.ERROR
                    ).showAndWait();
                    archiveView.hide();
                });
            }
        }

    }

    public ZipArchiver(ArchiveView archiveView){
        this.archiveView = archiveView;
    }

    @Override
    public File getArchiveFile() {
        return file;
    }

    private ArchiveEntry<ZipArchiveEntry> buildTree() {

        ResourceBundle bundle = resources.getResourceBundle();

        counts = 0;

        ArchiveEntry<ZipArchiveEntry> archiveEntry = new ArchiveEntry<>(null);
        archiveEntry.name(p -> file.getName());

        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

        List<ZipArchiveEntry> folders = new ArrayList<>();
        List<ZipArchiveEntry> files = new ArrayList<>();

        ProgressView progressView = archiveView.getView(ProgressView.class);
        progressView.update(bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                bundle.getString(ArchiveLangConstants.LangArchivePreparingParse), 0.4);
        progressView.show();

        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                folders.add(entry);
            } else {
                files.add(entry);
            }
            counts ++;
        }

        Function<ZipArchiveEntry,ArchiveEntry<ZipArchiveEntry>> resolveFolder = (ZipArchiveEntry entry) -> {

            ByteArrayInputStream bin = new ByteArrayInputStream(entry.getRawName());
            Charset charset = commonService.getCharset(bin,bin.available());
            String path = new String(entry.getRawName(),charset);

            String[] parts = path.split("/");

            ArchiveEntry<ZipArchiveEntry> parent = archiveEntry;
            ArchiveEntry<ZipArchiveEntry> current = null;

            for (int idx = 0; idx < parts.length; idx ++) {

                current = parent.getFolder(parts[idx]);
                if (current == null && ( entry.isDirectory() || idx + 1 < parts.length) ) {

                    int partIdx = idx;
                    current = new ArchiveEntry<>(null);
                    current.name(p -> parts[partIdx]);
                    current.setParent(parent);

                    parent.addFolder(parts[idx],current);
                    parent = current;

                } else if (current != null){
                    parent = current;
                }
            }

            return parent;
        };

        Consumer<ZipArchiveEntry> resolveFile = (entry) -> {

            ArchiveEntry<ZipArchiveEntry> finalParent = resolveFolder.apply(entry);
            ArchiveEntry<ZipArchiveEntry> target = new ArchiveEntry<>(entry);
            target.name(p -> getEntryName(p,finalParent));
            target.size(ZipArchiveEntry::getSize);
            finalParent.addFile(target.name(),target);
            target.setParent(finalParent);

        };


        double curr = 0;
        for (ZipArchiveEntry entry: folders) {
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                    entry.getName(),
                    curr / counts
            );
            resolveFolder.apply(entry);
            curr ++;
        }

        for (ZipArchiveEntry entry: files) {
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                    entry.getName(),
                    curr / counts
            );
            resolveFile.accept(entry);
            curr ++;
        }

        progressView.hide();

        return archiveEntry;

    }


    public String getEntryName(ZipArchiveEntry p, ArchiveEntry<ZipArchiveEntry> parent) {

        Charset charset = null;
        ByteArrayInputStream bin = new ByteArrayInputStream(p.getRawName());
        charset = commonService.getCharset(bin,bin.available());

        byte[] entryNameBytes = p.getRawName();
        String entryName = new String(entryNameBytes,charset);

        if (entryName.contains("/")) {
            return entryName.substring(entryName.lastIndexOf("/") + 1);
        } else {
            return entryName;
        }
    }


    @Override
    public TreeItem<ArchiveEntry<ZipArchiveEntry>> getDictionaryTree() {

        TreeItem<ArchiveEntry<ZipArchiveEntry>> root = new TreeItem<>();
        archiveView.getView().setDisable(true);

        commonService.submit(() -> {
            ArchiveEntry<ZipArchiveEntry> tree = buildTree();
            Platform.runLater(() -> {
                root.setValue(tree);
                UIUtils.createTree(root,tree);
                archiveView.getView().setDisable(false);
            });
        });

        return root;
    }

    @Override
    public void extract(List<ArchiveEntry<ZipArchiveEntry>> extract, File target, BiConsumer<String, Double> callback) {
        for (int idx = 0; idx < extract.size(); idx++) {
            ArchiveEntry<ZipArchiveEntry> ent = extract.get(idx);
            ZipArchiveEntry entry = ent.getEntry();
            if (entry != null) {
                File targetFile = getExtractTargetFile(ent,target);
                callback.accept(targetFile.getParentFile().getName(), ((double)idx) / extract.size());
                try {
                    InputStream in = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    byte[] buf = new byte[1024 * 4];
                    int readBytes = -1;
                    long readed = 0;
                    callback.accept(file.getName(), 0.0);
                    while ((readBytes = in.read(buf)) > -1) {
                        fos.write(buf,0,readBytes);
                        readed = readed + readBytes;
                        callback.accept(file.getName(), ((double)readed) / file.length());
                    }
                    fos.close();
                    in.close();

                } catch (Exception e) {
                    logger.error("failed to extract a entry: ", e);
                }
            }
            if (ent.getFiles() != null){
                extract(ent.getFiles(),target,callback);
            }
            if (ent.getChildrenFolder() != null) {
                extract(ent.getChildrenFolder(),target,callback);
            }
        }
    }

    @Override
    public void addEntry(ArchiveEntry<ZipArchiveEntry> targetFolderEntry, File item) {

        ResourceBundle bundle = resources.getResourceBundle();

        try {

            File temp = new File(file.getAbsolutePath() + ".tmp");
            ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(temp);
            outputStream.setEncoding(zipFile.getEncoding());

            archiveView.getView().setDisable(true);
            ProgressView progressView = archiveView.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                    bundle.getString(ArchiveLangConstants.LangArchivePrepareAdding),
                    0
            );
            commonService.submit(() -> {
                try {
                    progressView.show();
                    double curr = 0;
                    Enumeration<ZipArchiveEntry> entryEnumeration = zipFile.getEntries();
                    while (entryEnumeration.hasMoreElements()) {

                        ZipArchiveEntry entry = entryEnumeration.nextElement();
                        progressView.update(
                                bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                entry.getName(),
                                curr / counts
                        );

                        outputStream.putArchiveEntry(entry);
                        outputStream.write(zipFile.getInputStream(entry).readAllBytes());
                        outputStream.closeArchiveEntry();

                        curr ++;
                    }

                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                            bundle.getString(ArchiveLangConstants.LangArchiveWritingFile) + file.getName(),
                            1
                    );

                    ZipArchiveEntry entry = new ZipArchiveEntry(item,UIUtils.generateFileInArchivePath(targetFolderEntry,"zip",item));
                    outputStream.putArchiveEntry(entry);
                    outputStream.write(Files.readAllBytes(item.toPath()));
                    outputStream.closeArchiveEntry();
                    outputStream.close();

                    zipFile.close();
                    file.delete();
                    temp.renameTo(file);

                    zipFile = new ZipFile(file);

                    Platform.runLater(() -> {
                        archiveView.reload();
                        progressView.hide();
                    });
                } catch (Exception e) {
                    logger.error("failed to add entry :", e);
                    progressView.hide();
                }
            });
        } catch (Exception e) {
            logger.error("failed to add file", e);
        }
    }

    @Override
    public void removeEntry(List<ArchiveEntry<ZipArchiveEntry>> entries) {

        ResourceBundle bundle = resources.getResourceBundle();

        try {

            List<String> names = entries.stream()
                    .map(e -> e.getEntry() == null ?
                            UIUtils.generateEntryInArchivePath(e, "zip")
                                    .replace(File.separator,"/"):
                            e.getEntry().getName()
                    )
                    .collect(Collectors.toList());

            File temp = new File(file.getAbsolutePath() + ".tmp");
            ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(temp);
            outputStream.setEncoding(zipFile.getEncoding());

            archiveView.getView().setDisable(true);
            ProgressView progressView = archiveView.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                    bundle.getString(ArchiveLangConstants.LangArchivePrepareRemoving),
                    0
            );
            commonService.submit(() -> {
                try {
                    progressView.show();
                    double curr = 0;
                    Enumeration<ZipArchiveEntry> entryEnumeration = zipFile.getEntries();
                    while (entryEnumeration.hasMoreElements()) {

                        ZipArchiveEntry entry = entryEnumeration.nextElement();
                        if (names.contains(entry.getName())) {
                            // skip the archive which will delete.
                            continue;
                        }
                        progressView.update(
                                bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                entry.getName(),
                                curr / (counts - entries.size())
                        );

                        outputStream.putArchiveEntry(entry);
                        outputStream.write(zipFile.getInputStream(entry).readAllBytes());
                        outputStream.closeArchiveEntry();

                        curr ++;
                    }

                    outputStream.close();
                    zipFile.close();

                    file.delete();
                    temp.renameTo(file);

                    progressView.update("正在更新","文件删除即将完成", 1);

                    zipFile = new ZipFile(file);

                    Platform.runLater(() -> {
                        archiveView.reload();
                        progressView.hide();
                    });
                } catch (Exception e) {

                    logger.error("failed to remove entry, ",e);
                    progressView.hide();

                }
            });
        } catch (Exception e) {
            logger.error("failed to remove entry", e);
        }
    }

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public boolean exist() {
        return file != null && file.exists();
    }

    @Override
    public void saveAs(File file) {
        if (this.file == null) {
            this.file = file;
        }
    }

    @Override
    public void saveFile() {
        if (file == null) {
            return;
        }
    }

    @Override
    public void close() {
        if (this.zipFile != null) {
            try {
                zipFile.close();
            } catch (Exception e) {
            }
        }
    }
}
