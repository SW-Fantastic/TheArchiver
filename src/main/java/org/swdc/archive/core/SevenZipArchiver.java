package org.swdc.archive.core;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import net.sf.sevenzipjbinding.impl.VolumedArchiveInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.PasswordDialogView;
import org.swdc.archive.views.ProgressView;
import org.swdc.fx.FXResources;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SevenZipArchiver implements Archive<Integer,ArchiveEntry<Integer>> {

    private class ExtractPasswordCallback extends ExtractCallback implements ICryptoGetTextPassword {

        private String password;

        public ExtractPasswordCallback(Map<Integer, ArchiveEntry<Integer>> extIds, File target, BiConsumer<String, Double> progressCallback) {
            super(extIds, target, progressCallback);
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String cryptoGetTextPassword() throws SevenZipException {
            return password;
        }
    }

    private class ExtractCallback implements IArchiveExtractCallback {

        private Map<Integer,ArchiveEntry<Integer>> extIds;

        private Map<Integer, FileOutputStream> streamHashMap = new HashMap<>();

        private BiConsumer<String, Double> progressCallback;

        private File target;

        private int idx = -1;
        private int hash;
        private int size;

        private double resolved  = 0;


        public ExtractCallback(Map<Integer,ArchiveEntry<Integer>> extIds, File target,BiConsumer<String, Double> progressCallback) {
            this.extIds = extIds;
            this.target = target;
            this.progressCallback = progressCallback;
        }

        @Override
        public ISequentialOutStream getStream(int i, ExtractAskMode extractAskMode) throws SevenZipException {
            this.idx = i;
            if (extractAskMode != ExtractAskMode.EXTRACT) {
                return null;
            }
            return  data -> {
                FileOutputStream fout = null;
                if (streamHashMap.containsKey(idx)) {
                    fout = streamHashMap.get(idx);
                } else {
                    try {
                        File targetFile = getExtractTargetFile(extIds.get(idx),target);
                        fout = new FileOutputStream(targetFile);
                        streamHashMap.put(idx, fout);
                    } catch (Exception e) {
                        logger.error("failed to open output stream.", e);
                    }
                }
                try {
                    if (fout != null) {
                        fout.write(data);
                    }
                } catch (Exception e) {
                    logger.error("failed to write data to target file", e);
                }
                hash ^= Arrays.hashCode(data);
                size += data.length;
                return data.length; // Return amount of proceed data
            };
        }

        @Override
        public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {

        }

        @Override
        public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
            if (extractOperationResult != ExtractOperationResult.OK) {
                System.err.println("Extraction error");
            } else {
                try {
                    if (streamHashMap.containsKey(idx)) {
                        streamHashMap.remove(idx).close();
                    }
                } catch (Exception e) {
                    logger.error("failed to release outputstream.");
                }
                progressCallback.accept(archive.getProperty(idx,PropID.PATH).toString(),resolved / extIds.size());
                hash = 0;
                size = 0;
                resolved ++;
            }
        }

        @Override
        public void setTotal(long total) throws SevenZipException {

        }

        @Override
        public void setCompleted(long complete) throws SevenZipException {

        }
    }

    private Logger logger  = LoggerFactory.getLogger(SevenZipArchiver.class);

    private File sevenZipFile = null;

    private CommonService commonService = null;

    private ArchiveView archiveView;

    private boolean isMultipleVolume;

    // SevenZip API Definitions

    private IInArchive archive = null;
    private ISimpleInArchive simpleInArchive = null;
    private RandomAccessFile randomAccessFile = null;
    private RandomAccessFileInStream rin = null;
    private VolumeCallback volumeCallback = null;

    // Seven Zip API Definitions End

    private FXResources resources;

    private static class VolumeCallback implements IArchiveOpenVolumeCallback, Closeable {

        private Map<String,RandomAccessFile> pathVolumeFileMap = new HashMap<>();

        @Override
        public Object getProperty(PropID propID) throws SevenZipException {
            return null;
        }

        @Override
        public IInStream getStream(String path) throws SevenZipException {
            try {
                if (pathVolumeFileMap.containsKey(path)) {
                    RandomAccessFile file = pathVolumeFileMap.get(path);
                    file.seek(0);
                    return new RandomAccessFileInStream(file);
                } else {
                    File target = new File(path);
                    if (target.exists()) {
                        RandomAccessFile file = new RandomAccessFile(path, "rw");
                        pathVolumeFileMap.put(path,file);
                        return new RandomAccessFileInStream(file);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        public void close() throws IOException {
            for (RandomAccessFile file : pathVolumeFileMap.values()) {
                file.close();
            }
            pathVolumeFileMap.clear();
        }
    }

    public SevenZipArchiver(FXResources resources, File file, CommonService commonService, ArchiveView view) {
        this.sevenZipFile = file;
        this.commonService = commonService;
        this.archiveView = view;
        this.resources = resources;

        Pattern pattern = Pattern.compile("[\\s\\S]+\\.7z\\.[0-9]+$");
        Matcher matcher = pattern.matcher(sevenZipFile.getName());
        if (matcher.find()) {
            isMultipleVolume = true;
        }
        archiveView.archiver(this);
    }

    @Override
    public File getArchiveFile() {
        return sevenZipFile;
    }

    private ArchiveEntry<Integer> buildTree() {

        ResourceBundle bundle = resources.getResourceBundle();
        return openArchive((simpleInArchive, archive) -> {

            ProgressView progressView = archiveView.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                    bundle.getString(ArchiveLangConstants.LangArchivePreparingParse),
                    0.4
            );
            progressView.show();

            ArchiveEntry<Integer> archiveEntry = new ArchiveEntry<>(null);
            archiveEntry.name(e -> sevenZipFile.getName());
            Function<ISimpleInArchiveItem,ArchiveEntry<Integer>> resolveFolder = (ISimpleInArchiveItem entry) -> {
                try {
                    String[] parts = entry.getPath().split("\\\\");


                    ArchiveEntry<Integer> parent = archiveEntry;
                    ArchiveEntry<Integer> current = null;

                    for (int idx = 0; idx < parts.length - 1 ; idx ++) {

                        current = parent.getFolder(parts[idx]);
                        if (current == null && idx + 1 < parts.length ) {

                            final int finalIdx = idx;
                            current = new ArchiveEntry<>(-1);
                            current.setParent(parent);
                            current.name(p -> {
                                return parts[finalIdx];
                            });
                            parent.addFolder(parts[idx],current);
                            parent = current;

                        } else if (current != null){
                            parent = current;
                        }
                    }
                    return parent;
                } catch (Exception e) {
                    logger.error("error while open sevenzip file, caused by", e);
                    return null;
                }
            };


            Consumer<ISimpleInArchiveItem> resolveFile = (entry) -> {

                try {

                    ArchiveEntry<Integer> finalParent = resolveFolder.apply(entry);

                    String parentPath = UIUtils.generateEntryInArchivePath(finalParent,"7z");

                    ArchiveEntry<Integer> target = new ArchiveEntry<>(entry.getItemIndex());
                    String path = entry.getPath();
                    String absParent = parentPath;
                    target.name(p -> getEntryName(absParent,path,finalParent));
                    target.size(p -> {
                        try {
                            return entry.getSize();
                        } catch (SevenZipException e) {
                            return 0L;
                        }
                    });
                    target.setParent(finalParent);
                    if (entry.isFolder()) {
                        finalParent.addFolder(target.name(),target);
                    } else {
                        finalParent.addFile(target.name(),target);
                    }

                } catch (Exception e) {
                    logger.error("error while open a seven-zip file, caused by", e);
                }
            };

            try {
                double curr = 0;
                ISimpleInArchiveItem[] archiveItems = simpleInArchive.getArchiveItems();
                for (ISimpleInArchiveItem item : archiveItems) {
                    resolveFile.accept(item);
                    curr ++;
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveFileOpening),
                            bundle.getString(ArchiveLangConstants.LangArchiveResolving) + "：" + item.getPath(),
                            curr / archiveItems.length
                    );
                }
                progressView.hide();
                return archiveEntry;
            } catch (SevenZipException e) {
               logger.error("error while open a seven zip file, caused by" , e);
            }
            return null;
        });

    }

    public String getEntryName(String absoluteParetPath, String p, ArchiveEntry<Integer> parent)  {

        if (parent == null || parent.getEntry() == null) {
            return p;
        }

        String parentName = absoluteParetPath.replace("\\","/");
        String entryName = p.replace("\\","/");

        String path = Path
                .of(parentName)
                .resolve(Path.of(entryName))
                .getFileName().toString();
        try {
            return path.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return p;
        }
    }

    private void createTree(TreeItem<ArchiveEntry<Integer>> parent, ArchiveEntry<Integer> value) {

        List<ArchiveEntry<Integer>> folders = value.getChildrenFolder();

        for (ArchiveEntry<Integer> folder: folders) {
            TreeItem<ArchiveEntry<Integer>> item = new TreeItem<>();
            item.setValue(folder);
            parent.getChildren().add(item);
            if (folder.getChildrenFolder().size() > 0) {
                createTree(item,folder);
            }
        }

    }

    @Override
    public void getDictionaryTree(Consumer<TreeItem<ArchiveEntry<Integer>>> consumer) {

        TreeItem<ArchiveEntry<Integer>> root = new TreeItem<>();

        commonService.submit(() -> {
            ArchiveEntry<Integer> tree = buildTree();
            if (tree == null) {
                throw new RuntimeException("can not open file.");
            }
            Platform.runLater(() -> {
                root.setValue(tree);
                createTree(root,tree);
                consumer.accept(root);
            });
        });

    }


    private <R> R openArchive(BiFunction<ISimpleInArchive, IInArchive,R> res) {

        ResourceBundle bundle = resources.getResourceBundle();

        try {

            if (simpleInArchive != null && archive != null) {
                return res.apply(simpleInArchive,archive);
            } else if (isMultipleVolume) {
                int vol = Integer.parseInt(sevenZipFile.getName().substring(sevenZipFile.getName().lastIndexOf(".") + 1));

                if (vol > 1) {
                    File firstVol = new File(sevenZipFile.getParentFile().getAbsolutePath() + File.separator + sevenZipFile.getName().substring(0,sevenZipFile.getName().lastIndexOf(".") ) + ".001");
                    if (!firstVol.exists()) {
                        // 第一个7z不存在。
                        Alert alert = archiveView.alert(
                                bundle.getString(ArchiveLangConstants.LangArchiveErrorTitle),
                                bundle.getString(ArchiveLangConstants.LangSevenZipMissingFirstVolume),
                                Alert.AlertType.ERROR
                        );
                        alert.showAndWait();
                        archiveView
                                .getStage()
                                .close();
                        return null;
                    } else {
                        sevenZipFile = firstVol;
                    }
                }
                archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP,
                        new VolumedArchiveInStream(
                                sevenZipFile.getAbsolutePath(),
                                new VolumeCallback()
                        )
                );
                simpleInArchive = archive.getSimpleInterface();
                return res.apply(simpleInArchive,archive);
            } else {
                randomAccessFile = new RandomAccessFile(sevenZipFile, "rw");
                rin = new RandomAccessFileInStream(randomAccessFile);
                archive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, rin);
                simpleInArchive = archive.getSimpleInterface();
                return res.apply(simpleInArchive,archive);
            }

        } catch (Exception ex) {
            logger.error("error while loading seven zip file, caused by ", ex);
        }
        return null;
    }

    private Map<Integer,ArchiveEntry<Integer>> getAllExtractions(List<ArchiveEntry<Integer>> extract, Map<Integer,ArchiveEntry<Integer>> extractions) {
        Map<Integer,ArchiveEntry<Integer>> target = extractions == null ? new HashMap<>(): extractions;
        for (int idx = 0; idx < extract.size(); idx ++) {
            ArchiveEntry<Integer> ent = extract.get(idx);
            if (ent.getEntry() != null) {
                target.put(ent.getEntry(),ent);
            }
            if (ent.getFiles() != null) {
                getAllExtractions(ent.getFiles(), target);
            }
            if (ent.getChildrenFolder() != null) {
                getAllExtractions(ent.getChildrenFolder(), target);
            }
        }
        return target;
    }



    @Override
    public void extract(List<ArchiveEntry<Integer>> extract, File target, BiConsumer<String, Double> progressCallback) {
        Map<Integer, FileOutputStream> streamHashMap = new HashMap<>();
        Map<Integer,ArchiveEntry<Integer>> extIds = getAllExtractions(extract,null);
        openArchive((simpleInArchive, archive) -> {
            try {

                boolean hasPassword = extIds.values().stream().filter(v -> v.getEntry() != null).map(i -> {
                    try {
                        return simpleInArchive.getArchiveItem(i.getEntry());
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull).anyMatch(e -> {
                    try {
                        return e.isEncrypted();
                    } catch (SevenZipException ex) {
                        return false;
                    }
                });

                if (hasPassword) {
                    Platform.runLater(() -> {
                        PasswordDialogView passwordDialogView = archiveView.getView(PasswordDialogView.class);
                        passwordDialogView.show();
                        String pwd = passwordDialogView.getText();
                        if (pwd.isBlank()) {
                            ResourceBundle bundle = resources.getResourceBundle();
                            Alert alert = archiveView.alert(
                                    bundle.getString(ArchiveLangConstants.LangArchiveDialogFail),
                                    bundle.getString(ArchiveLangConstants.LangArchiveEmptyPassword),
                                    Alert.AlertType.ERROR
                            );
                            alert.showAndWait();
                            return;
                        }
                        ExtractPasswordCallback pwdCallback = new ExtractPasswordCallback(extIds,target,progressCallback);
                        pwdCallback.setPassword(pwd);
                        resources.getExecutor().execute(() -> {
                            try {
                                archive.extract(
                                        extIds.keySet().stream()
                                                .mapToInt(Integer::intValue)
                                                .toArray(), false, pwdCallback
                                );
                            } catch (SevenZipException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    });

                } else {
                    ExtractCallback callback = new ExtractCallback(extIds,target,progressCallback);
                    archive.extract(extIds.keySet().stream().mapToInt(Integer::intValue).toArray(), false, callback);
                }

            } catch (SevenZipException e) {
                e.printStackTrace();
            }
            return null;
        });

    }

    @Override
    public void addEntry(ArchiveEntry<Integer> targetFolderEntry, File item) {

        String path = UIUtils.generateFileInArchivePath(targetFolderEntry,"7z",item);
        ResourceBundle bundle = resources.getResourceBundle();
        commonService.submit(() -> {

            ProgressView progressView = archiveView.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                    bundle.getString(ArchiveLangConstants.LangArchivePrepareAdding),
                    0.2
            );
            progressView.show();

            try {

                RandomAccessFile outFile = new RandomAccessFile(sevenZipFile.getAbsolutePath() + ".tmp", "rw");
                IOutUpdateArchive<IOutItemAllFormats> outUpdateArchive = archive.getConnectedOutArchive();
                archiveView.getView().setDisable(true);
                outUpdateArchive.updateItems(
                        new RandomAccessFileOutStream(outFile),
                        archive.getNumberOfItems(),
                        new IOutCreateCallback<>() {

                            private double total;

                            @Override
                            public void setOperationResult(boolean b) throws SevenZipException {
                            }

                            @Override
                            public IOutItemAllFormats getItemInformation(int i, OutItemFactory<IOutItemAllFormats> outItemFactory) throws SevenZipException {
                                if (i == archive.getNumberOfItems() - 1) {
                                    IOutItemAllFormats outItem = outItemFactory.createOutItem();
                                    outItem.setDataSize(item.length());
                                    outItem.setPropertyPath(path.toString());
                                    return outItem;
                                }
                                return outItemFactory.createOutItem(i);
                            }

                            @Override
                            public ISequentialInStream getStream(int i) throws SevenZipException {
                                if (i == archive.getNumberOfItems() - 1) {
                                    try {
                                        return new RandomAccessFileInStream(
                                                new RandomAccessFile(item,"r")
                                        );
                                    } catch (FileNotFoundException e) {
                                        logger.error("can not open target file: " + item.getAbsolutePath(), e);
                                    }
                                }
                                return null;
                            }

                            @Override
                            public void setTotal(long l) throws SevenZipException {
                                this.total = l;
                            }

                            @Override
                            public void setCompleted(long l) throws SevenZipException {
                                progressView.update(
                                        bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                        bundle.getString(ArchiveLangConstants.LangArchiveResolving),
                                        l / total
                                );
                            }
                        });
                progressView.hide();
                outFile.close();
                this.close();

                File newFile = new File(sevenZipFile.getAbsolutePath() + ".tmp");
                sevenZipFile.delete();
                newFile.renameTo(sevenZipFile);
                sevenZipFile = new File(sevenZipFile.getAbsolutePath());
                Platform.runLater(() -> {
                    archiveView.reload();
                    archiveView.getView().setDisable(false);
                });
            } catch (Exception e) {
               logger.error("error on adding file.", e);
               Platform.runLater(() -> {
                   progressView.hide();
                   archiveView.getView().setDisable(false);
                   archiveView.alert(
                           bundle.getString(ArchiveLangConstants.LangArchiveErrorTitle),
                           bundle.getString(ArchiveLangConstants.LangArchiveCannotAddFile)
                                   .replace("#file",item.getAbsolutePath()),
                           Alert.AlertType.ERROR
                           ).showAndWait();
               });
            }
        });

    }

    @Override
    public InputStream getInputStream(ArchiveEntry<Integer> entry) {
        if (entry == null || entry.getEntry() < 0) {
            return null;
        }
        return openArchive((simpleInArchive, archive) -> {
            try {
                ByteArrayOutputStream bot = new ByteArrayOutputStream();
                ISimpleInArchiveItem item = simpleInArchive.getArchiveItem(entry.getEntry());
                if (item.isEncrypted()) {
                    PasswordDialogView passwordDialogView = archiveView.getView(PasswordDialogView.class);
                    passwordDialogView.show();
                    String pwd = passwordDialogView.getText();
                    if (pwd.isBlank()) {
                        ResourceBundle bundle = resources.getResourceBundle();
                        Alert alert = archiveView.alert(
                                bundle.getString(ArchiveLangConstants.LangArchiveDialogFail),
                                bundle.getString(ArchiveLangConstants.LangArchiveEmptyPassword),
                                Alert.AlertType.ERROR
                        );
                        alert.showAndWait();
                        return null;
                    } else {
                        item.extractSlow( dt -> {
                            try {
                                bot.write(dt);
                                return dt.length;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }, pwd);
                    }
                } else {
                    item.extractSlow( dt -> {
                        try {
                            bot.write(dt);
                            return dt.length;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                return new ByteArrayInputStream(bot.toByteArray());
            } catch (Exception ex ) {
                return null;
            }
        });
    }

    @Override
    public void removeEntry(List<ArchiveEntry<Integer>> entries) {

        List<Integer> removingIndexes = entries.stream()
                .filter(e -> e.getEntry() != null && e.getEntry() > -1)
                .map(ArchiveEntry::getEntry)
                .collect(Collectors.toList());

        List<String> removingFolders = entries.stream()
                .filter(e -> e.getEntry() != null && e.getEntry() == -1)
                .map(e -> UIUtils.generateEntryInArchivePath(e,"7z").replace(File.separator, "\\"))
                .collect(Collectors.toList());

        ResourceBundle bundle = resources.getResourceBundle();

        commonService.submit(() -> {

            archiveView.getView().setDisable(true);
            ProgressView progressView = archiveView.getView(ProgressView.class);
            progressView.update(
                    bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                    bundle.getString(ArchiveLangConstants.LangArchivePrepareRemoving),
                    0.2
            );
            progressView.show();


            try {

                ISimpleInArchiveItem[] items = archive.getSimpleInterface().getArchiveItems();
                progressView.update(bundle.getString(
                        ArchiveLangConstants.LangArchiveUpdatingFile),
                        bundle.getString(ArchiveLangConstants.LangArchiveIndexing),
                        0.3
                );
                for (ISimpleInArchiveItem item : items) {
                    if (item.isFolder() && removingFolders.contains(item.getPath())) {
                        removingIndexes.add(item.getItemIndex());
                    }
                }

                RandomAccessFile outFile = new RandomAccessFile(sevenZipFile.getAbsolutePath() + ".tmp", "rw");
                IOutUpdateArchive<IOutItemAllFormats> outUpdateArchive = archive.getConnectedOutArchive();
                outUpdateArchive.updateItems(new RandomAccessFileOutStream(outFile), archive.getNumberOfItems() - removingIndexes.size(), new IOutCreateCallback<>() {

                    private int offsets = 0;
                    private double total;
                    private List<Integer> removedIndex = new ArrayList<>();

                    @Override
                    public void setOperationResult(boolean b) throws SevenZipException {

                    }

                    @Override
                    public IOutItemAllFormats getItemInformation(int i, OutItemFactory<IOutItemAllFormats> outItemFactory) throws SevenZipException {
                        if (removingIndexes.contains(i + offsets) && !removedIndex.contains(i + offsets)) {
                            removedIndex.add(i);
                            offsets ++;
                            while (removingIndexes.contains(i + offsets)) {
                                removedIndex.add(i + offsets);
                                offsets ++;
                            }
                        }
                        return outItemFactory.createOutItem(i + offsets);
                    }

                    @Override
                    public ISequentialInStream getStream(int i) throws SevenZipException {
                        return null;
                    }

                    @Override
                    public void setTotal(long l) throws SevenZipException {
                        this.total = l;
                    }

                    @Override
                    public void setCompleted(long l) throws SevenZipException {
                        progressView.update(
                                bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                bundle.getString(ArchiveLangConstants.LangArchiveResolving),
                                l / total
                        );
                    }
                });
                progressView.hide();
                outFile.close();
                this.close();

                File newFile = new File(sevenZipFile.getAbsolutePath() + ".tmp");
                sevenZipFile.delete();
                newFile.renameTo(sevenZipFile);
                sevenZipFile = new File(sevenZipFile.getAbsolutePath());
                Platform.runLater(() -> {
                    archiveView.reload();
                    archiveView.getView().setDisable(false);
                });
            } catch (Exception e) {
                logger.error("failed to remove file: ", e);
                progressView.hide();
                archiveView.getView().setDisable(false);
            }

        });
    }

    @Override
    public boolean editable() {
        return !isMultipleVolume;
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
    public void close() {
        try {
            if (simpleInArchive != null) {
                simpleInArchive.close();
                simpleInArchive = null;
            }
            if (archive != null) {
                archive.close();
                archive = null;
            }
            if (rin != null) {
                rin.close();
                rin = null;
            }
            if (randomAccessFile != null) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
            if (volumeCallback != null) {
                volumeCallback.close();
                volumeCallback = null;
            }
        } catch (Exception ex) {
        }
    }

}
