package org.swdc.archive.core;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AbstractFileHeader;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.PasswordDialogView;
import org.swdc.archive.views.ProgressView;
import org.swdc.archive.views.viewer.StreamViewer;
import org.swdc.fx.FXResources;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ZipArchiver implements Archive<FileHeader,ArchiveEntry<FileHeader>> {

    private File file;

    public ArchiveView archiveView;

    private ZipFile zipFile;

    private CommonService commonService;

    private int counts = 0;

    private FXResources resources;

    private boolean multiple;

    private static Logger logger = LoggerFactory.getLogger(ZipArchiver.class);

    private boolean passwordReady;

    public ZipArchiver(
            File file,
            ArchiveView archiveView,
            CommonService commonService,
            FXResources resources
    ) {

        this.file = file;
        this.archiveView = archiveView;
        this.commonService = commonService;
        this.resources = resources;

        ResourceBundle bundle = resources.getResourceBundle();

        if (file != null) {
            try {

                this.zipFile = new ZipFile(file);
                this.zipFile.setCharset(commonService.getZipCharset(file));
                this.multiple = zipFile.isSplitArchive();
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

    private ArchiveEntry<FileHeader> buildTree() {

        ResourceBundle bundle = resources.getResourceBundle();
        counts = 0;

        ArchiveEntry<FileHeader> archiveEntry = new ArchiveEntry<>(null);
        archiveEntry.name(p -> file.getName());

        List<FileHeader> entries;
        try {
            entries = zipFile.getFileHeaders();
        } catch (Exception e) {
            archiveView.hide();
            logger.error("failed to read entries.",e);
            return null;
        }

        List<FileHeader> folders = new ArrayList<>();
        List<FileHeader> files = new ArrayList<>();

        ProgressView progressView = archiveView.getView(ProgressView.class);
        progressView.update(bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                bundle.getString(ArchiveLangConstants.LangArchivePreparingParse), 0.4);
        progressView.show();

        for (FileHeader header : entries) {
            if (header.isDirectory()) {
                folders.add(header);
            } else {
                files.add(header);
            }
            counts ++;
        }


        Function<FileHeader,ArchiveEntry<FileHeader>> resolveFolder = (FileHeader entry) -> {

            String path = utfName(entry);
            String[] parts = path.split("/");

            ArchiveEntry<FileHeader> parent = archiveEntry;
            ArchiveEntry<FileHeader> current = null;

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

        Consumer<FileHeader> resolveFile = (entry) -> {

            ArchiveEntry<FileHeader> finalParent = resolveFolder.apply(entry);
            ArchiveEntry<FileHeader> target = new ArchiveEntry<>(entry);
            target.name(p -> getEntryName(p,finalParent));
            target.size(FileHeader::getUncompressedSize);
            finalParent.addFile(target.name(),target);
            target.setParent(finalParent);

        };


        double curr = 0;
        for (FileHeader entry: folders) {
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                    utfName(entry),
                    curr / counts
            );
            resolveFolder.apply(entry);
            curr ++;
        }

        for (FileHeader entry: files) {
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                    utfName(entry),
                    curr / counts
            );
            resolveFile.accept(entry);
            curr ++;
        }

        progressView.hide();

        return archiveEntry;

    }


    public String getEntryName(FileHeader p, ArchiveEntry<FileHeader> parent) {

        String entryName = utfName(p);
        if (entryName.contains("/")) {
            return entryName.substring(entryName.lastIndexOf("/") + 1);
        } else {
            return entryName;
        }
    }

    @Override
    public InputStream getInputStream(ArchiveEntry<FileHeader> entry) {
        FileHeader header = entry.getEntry();
        if (header == null || header.isDirectory()) {
            return null;
        }
        if (header.isEncrypted()) {
            PasswordDialogView passwordDialogView = archiveView.getView(PasswordDialogView.class);
            passwordDialogView.show();
            String txt = passwordDialogView.getText();
            if (txt.isBlank()) {
                ResourceBundle bundle = resources.getResourceBundle();
                Alert alert = archiveView.alert(
                        bundle.getString(ArchiveLangConstants.LangArchiveDialogFail),
                        bundle.getString(ArchiveLangConstants.LangArchiveEmptyPassword),
                        Alert.AlertType.ERROR
                );
                alert.showAndWait();
                return null;
            }
            zipFile.setPassword(txt.toCharArray());
            this.passwordReady = true;
        }

        try {
            return zipFile.getInputStream(header);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void getDictionaryTree(Consumer<TreeItem<ArchiveEntry<FileHeader>>> consumer) {

        TreeItem<ArchiveEntry<FileHeader>> root = new TreeItem<>();
        archiveView.getView().setDisable(true);

        commonService.submit(() -> {
            ArchiveEntry<FileHeader> tree = buildTree();
            Platform.runLater(() -> {
                root.setValue(tree);
                UIUtils.createTree(root,tree);
                consumer.accept(root);
                archiveView.getView().setDisable(false);
            });
        });
    }

    @Override
    public void extract(List<ArchiveEntry<FileHeader>> extract, File target, BiConsumer<String, Double> callback) {
        boolean hasPwd = extract.stream().map(ArchiveEntry::getEntry)
                .filter(Objects::nonNull)
                .anyMatch(AbstractFileHeader::isEncrypted);

        if (hasPwd && !passwordReady) {
            Platform.runLater(() -> {
                PasswordDialogView passwordDialogView = archiveView.getView(PasswordDialogView.class);
                passwordDialogView.show();
                String txt = passwordDialogView.getText();
                if (txt.isBlank()) {
                    ResourceBundle bundle = resources.getResourceBundle();
                    Alert alert = archiveView.alert(
                            bundle.getString(ArchiveLangConstants.LangArchiveDialogFail),
                            bundle.getString(ArchiveLangConstants.LangArchiveEmptyPassword),
                            Alert.AlertType.ERROR
                    );
                    alert.showAndWait();
                    return;
                }
                zipFile.setPassword(txt.toCharArray());
                this.passwordReady = true;
                resources.getExecutor().execute(() -> {
                    this.extract(extract, target, callback);
                });
            });
            return;
        }

        for (int idx = 0; idx < extract.size(); idx++) {
            ArchiveEntry<FileHeader> ent = extract.get(idx);
            FileHeader entry = ent.getEntry();
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
    public void addEntry(ArchiveEntry<FileHeader> targetFolderEntry, File item) {

        ResourceBundle bundle = resources.getResourceBundle();

        try {

            archiveView.getView().setDisable(true);
            ProgressView progressView = archiveView.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                    bundle.getString(ArchiveLangConstants.LangArchivePrepareAdding),
                    0
            );

            commonService.submit(() -> {
                try {

                    ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                    ZipParameters parameters = new ZipParameters();
                    parameters.setFileNameInZip(
                            UIUtils.generateFileInArchivePath(targetFolderEntry,"zip",item)
                    );

                    progressView.show();
                    zipFile.setRunInThread(true);
                    FileInputStream in = new FileInputStream(item) {
                        @Override
                        public int read(byte[] b) throws IOException {
                            int readed =  super.read(b);
                            long proceed = item.length() - available();
                            double progress = (double) proceed / (double) item.length();
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                    bundle.getString(ArchiveLangConstants.LangArchiveWritingFile) + item.getName(),
                                    progress
                            );
                            return readed;
                        }

                    };
                    zipFile.addStream(in,parameters);

                    int prog = progressMonitor.getPercentDone();
                    while (progressMonitor.getState() != ProgressMonitor.State.READY) {
                        if (prog != progressMonitor.getPercentDone()) {
                            prog = progressMonitor.getPercentDone();
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                    bundle.getString(ArchiveLangConstants.LangArchiveWritingFile) + file.getName(),
                                    progressMonitor.getPercentDone() / 100.0
                            );
                        }
                    }

                    if (progressMonitor.getResult() == ProgressMonitor.Result.ERROR) {
                        logger.error("failed to write file", progressMonitor.getException());
                    }

                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                            bundle.getString(ArchiveLangConstants.LangArchiveWritingFile) + file.getName(),
                            1
                    );

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

    private String utfName(FileHeader name) {
        byte[] data = name.getFileName().getBytes(zipFile.getCharset());
        if (!name.isFileNameUTF8Encoded()) {
            return new String(data,Charset.forName(System.getProperty("sun.jnu.encoding")));
        }
        return name.getFileName();
    }

    @Override
    public void removeEntry(List<ArchiveEntry<FileHeader>> entries) {

        ResourceBundle bundle = resources.getResourceBundle();

        try {

            List<String> names = entries.stream()
                    .map(e -> e.getEntry() == null ?
                            UIUtils.generateEntryInArchivePath(e, "zip")
                                    .replace(File.separator,"/"):
                            utfName(e.getEntry())
                    )
                    .collect(Collectors.toList());

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

                    zipFile.removeFiles(names);

                    ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                    zipFile.setRunInThread(true);

                    while (progressMonitor.getState() != ProgressMonitor.State.READY) {
                        progressView.update(
                                bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                file.getName(),
                                progressMonitor.getPercentDone() / 100.0
                        );
                    }

                    progressView.update("正在更新","文件删除即将完成", 1);

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
        return !multiple;
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
