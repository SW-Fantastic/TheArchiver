package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Path;
import javafx.stage.FileChooser;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.ProgressView;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;
import org.swdc.fx.config.ConfigViews;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@MultipleImplement(ArchiveDescriptor.class)
public class SevenZipArchiverDescriptor implements ArchiveDescriptor {

    private FileChooser.ExtensionFilter filter = null;

    public static class SevenZipCompressPasswordCallback extends SevenZipCompressCallback implements ICryptoGetTextPassword {

        private String password;

        public SevenZipCompressPasswordCallback(ResourceBundle resourceBundle, List<ArchiveSource> files, BiConsumer<String, Double> callback) {
            super(resourceBundle, files, callback);
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String cryptoGetTextPassword() throws SevenZipException {
            return password;
        }

    }

    public static class SevenZipCompressCallback implements IOutCreateCallback<IOutItem7z> {

        private List<ArchiveSource> files;
        private BiConsumer<String,Double> progressCb;

        private long total = 0;
        private String curr;
        private ResourceBundle bundle;

        public SevenZipCompressCallback(ResourceBundle resourceBundle ,List<ArchiveSource> files, BiConsumer<String, Double> callback) {
            this.files = files;
            this.progressCb = callback;
            this.bundle = resourceBundle;
        }

        @Override
        public void setOperationResult(boolean b) throws SevenZipException {

        }

        private ArchiveSource getSource(int itemIndex) {
            int curr = 0;
            for (int idx = 0; idx < files.size(); idx ++) {
                ArchiveSource source = files.get(idx);
                curr = curr + source.getFiles().size();
                if (curr > itemIndex) {
                    return source;
                }
            }
            return null;
        }

        private int getSourceIndex(int itemIndex) {
            int curr = 0;
            for (int idx = 0; idx < files.size(); idx ++) {
                ArchiveSource source = files.get(idx);
                if (itemIndex > curr) {
                    curr = curr + source.getFiles().size();
                } else {
                    return curr - itemIndex;
                }
            }
            return -1;
        }

        @Override
        public IOutItem7z getItemInformation(int i, OutItemFactory<IOutItem7z> outItemFactory) throws SevenZipException {
            ArchiveSource source = getSource(i);
            if (source == null) {
                return null;
            }
            int index = getSourceIndex(i);
            if (index < 0) {
                return null;
            }
            File file = source.getFiles().get(index);
            IOutItem7z item = outItemFactory.createOutItem();
            item.setPropertyIsDir(file.isDirectory());
            String path = source.getParent()
                    .getParentFile()
                    .toPath()
                    .toAbsolutePath()
                    .relativize(file.toPath())
                    .normalize()
                    .toString();
            if (path.startsWith("..")) {
                path = path.replace("..", "");
            }
            item.setPropertyPath(path);
            item.setDataSize(file.length());
            return item;
        }

        @Override
        public ISequentialInStream getStream(int i) throws SevenZipException {
            ArchiveSource source = getSource(i);
            if (source == null) {
                return null;
            }
            int index = getSourceIndex(i);
            if (index < 0) {
                return null;
            }
            File file = source.getFiles().get(index);
            String path = source
                    .getParent()
                    .getParentFile()
                    .toPath()
                    .toAbsolutePath()
                    .relativize(file.toPath())
                    .normalize()
                    .toString();
            if (path.startsWith("..")) {
                path = path.replace("..", "");
            }
            curr = path;
            if (file.isDirectory()) {
                return null;
            } else {
                try {
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                    return new RandomAccessFileInStream(randomAccessFile);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        @Override
        public void setTotal(long l) throws SevenZipException {
            this.total = l;
        }

        @Override
        public void setCompleted(long l) throws SevenZipException {
            if (progressCb != null) {
                progressCb.accept(bundle.getString(ArchiveLangConstants.LangArchiveResolving) + "：" + curr , ((double)l) / total);
            }
        }
    }

    @Inject
    private CommonService commonService;

    @Inject
    private FXResources resources;

    @Inject
    private ThreadPoolExecutor executor;

    @Inject
    private Logger logger;

    @Override
    public String name() {
        return resources.getResourceBundle().getString(ArchiveLangConstants.LangSevenZipDisplayName);
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (filter == null) {
            filter = new FileChooser.ExtensionFilter(resources.getResourceBundle()
                    .getString(ArchiveLangConstants.LangSevenZipDisplayName),
                    "*.7z","*.7z.*"
            );
        }
        return filter;
    }

    @Override
    public boolean support(File file) {
        Pattern pattern = Pattern.compile("[\\s\\S]+\\.7z$|[\\s\\S]+\\.7z\\.[0-9]+$");
        Matcher matcher = pattern.matcher(file.getName());
        return matcher.find();
    }

    @Override
    public Archive open(ArchiveView view, File file) {
        SevenZipArchiver archiver = new SevenZipArchiver(resources,file,commonService,view);
        return archiver;
    }

    @Override
    public boolean creatable() {
        return true;
    }

    @Override
    public void createArchive(CompressView view) {

        ResourceBundle bundle = resources.getResourceBundle();

        SevenZipCompressConf compressConf = new SevenZipCompressConf();
        BorderPane compressView = (BorderPane) view.getView();

        ObservableList properties = ConfigViews.parseConfigs(resources,compressConf);
        PropertySheet sheet = new PropertySheet(properties);
        sheet.setPropertyEditorFactory(ConfigViews.factory(resources));
        sheet.setSearchBoxVisible(false);
        sheet.setModeSwitcherVisible(false);
        sheet.getStyleClass().add("prop-sheet");

        BorderPane content = (BorderPane) compressView.getCenter();
        content.setCenter(sheet);

        view.show();
        if (!view.isCanceled()) {
            ProgressView progressView = view.getView(ProgressView.class);
            File targetFile = new File(view.getTargetFolder() + File.separator + view.getFileName() + ".7z");
            List<File> sources = view.getCompressSource();
            if (targetFile.exists()) {
                Alert alert = view.alert(
                        bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle),
                        bundle.getString(ArchiveLangConstants.LangArchiveFileOverride),
                        Alert.AlertType.CONFIRMATION
                );
                Optional<ButtonType> type = alert.showAndWait();
                if (type.isEmpty() || !type.get().equals(ButtonType.OK)) {
                    return;
                }
                targetFile.delete();
            }
            executor.execute(() -> {

                List<ArchiveSource> files = sources.stream().map(ArchiveSource::new)
                        .collect(Collectors.toList());

                int count = files.stream()
                        .mapToInt(f -> f.getFiles().size())
                        .reduce(Integer::sum)
                        .getAsInt();

                progressView.show();
                RandomAccessFile rout = null;
                IOutCreateArchive7z createArchive7z = null;
                try {
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                            bundle.getString(ArchiveLangConstants.LangArchiveIndexing),
                            0
                    );
                   // List<File> files = UIUtils.indexFolders(source);


                    rout = new RandomAccessFile(targetFile, "rw");
                    createArchive7z = SevenZip.openOutArchive7z();
                    createArchive7z.setLevel(compressConf.getCompressLevel());
                    createArchive7z.setSolid(compressConf.getSolid());

                    String password = compressConf.getPassword();
                    SevenZipCompressCallback callback = null;
                    if (!password.isBlank()) {
                        createArchive7z.setHeaderEncryption(true);
                        SevenZipCompressPasswordCallback pwdCallback = new SevenZipCompressPasswordCallback(bundle, files, (path, percent) -> {
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                    path,
                                    percent
                            );
                        });
                        pwdCallback.setPassword(password);
                        callback = pwdCallback;
                    } else {
                        callback = new SevenZipCompressCallback(bundle,files, (path, percent) -> {
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                    path,
                                    percent
                            );
                        });
                    }

                    createArchive7z.createArchive(new RandomAccessFileOutStream(rout),count,callback);
                } catch (Exception e) {
                    logger.error("failed to create seven-zip file", e);
                } finally {
                    try {
                        Platform.runLater(progressView::hide);
                        if (createArchive7z != null) {
                            createArchive7z.close();
                        }
                        if (rout != null) {
                            rout.close();
                        }
                    } catch (Exception e) {
                        logger.error("failed to create seven-zip file", e);
                    }
                }
            });
        }
    }


}
