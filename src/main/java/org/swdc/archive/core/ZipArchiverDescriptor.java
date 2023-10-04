package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.ProgressView;
import org.swdc.archive.views.viewer.StreamViewer;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;
import org.swdc.fx.config.ConfigViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@MultipleImplement(ArchiveDescriptor.class)
public class ZipArchiverDescriptor implements ArchiveDescriptor {

    private FileChooser.ExtensionFilter extensionFilter = null;

    @Inject
    private CommonService commonService;

    @Inject
    private FXResources resources;

    @Inject
    private ThreadPoolExecutor executor;

    @Inject
    private Logger logger;

    @Inject
    List<StreamViewer> viewers;

    @Override
    public String name() {
        return resources.getResourceBundle().getString(ArchiveLangConstants.LangZipArchiveDisplayName);
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (extensionFilter == null) {
            extensionFilter = new FileChooser.ExtensionFilter(
                    resources.getResourceBundle().getString(ArchiveLangConstants.LangZipArchiveDisplayName),
                    "*.zip","*.zip.*","*.z*"
            );
        }
        return extensionFilter;
    }

    @Override
    public boolean support(File file) {
        return file.getName().toLowerCase()
                .matches(".+\\.(z[0-9]+|zip\\.[0-9]+|zip)");
    }

    @Override
    public Archive open(ArchiveView view, File file) {
        return new ZipArchiver(
                file,
                view,
                commonService,
                resources
        );
    }


    @Override
    public boolean creatable() {
        return false;
    }

    @Override
    public void createArchive(CompressView view) {

        ResourceBundle bundle = resources.getResourceBundle();

        ZipCompressConf compressConf = new ZipCompressConf(bundle);

        BorderPane compressView = (BorderPane) view.getView();
        ObservableList properties = ConfigViews.parseConfigs(resources,compressConf);
        PropertySheet sheet = new PropertySheet(properties);
        sheet.setPropertyEditorFactory(ConfigViews.factory(resources));
        sheet.setSearchBoxVisible(false);
        sheet.setModeSwitcherVisible(false);
        sheet.getStyleClass().add("prop-sheet");
        compressView.setCenter(sheet);

        view.show();
        if (!view.isCanceled()) {
            ProgressView progressView = view.getView(ProgressView.class);
            File targetFile = new File(view.getTargetFolder() + File.separator + view.getFileName() + ".zip");
            File source = view.getSourcePath();
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
                progressView.show();
                // do compress
                ZipParameters parameters = new ZipParameters();
                CompressionLevel selLevel = CompressionLevel.NORMAL;
                String levelStr = compressConf.getLevel();
                String[] l18nLevelStr = bundle.getString("archive.zip.compress-levels").split(",");
                for (int idx = 0; idx < l18nLevelStr.length; idx ++) {
                    if (levelStr.equals(l18nLevelStr[idx])) {
                        selLevel = CompressionLevel.values()[idx];
                    }
                }
                parameters.setCompressionLevel(selLevel);
                parameters.setCompressionMethod(CompressionMethod.valueOf(compressConf.getCompressMethod()));

                ZipFile zipFile = new ZipFile(targetFile);
                zipFile.setCharset(StandardCharsets.UTF_8);

                try {
                    ProgressMonitor monitor = zipFile.getProgressMonitor();
                    zipFile.setRunInThread(true);

                    if (source.isFile()) {
                        zipFile.addFile(source,parameters);
                    } else {
                        zipFile.addFolder(source,parameters);
                    }

                    double prog = 0.0;
                    while (monitor.getState() != ProgressMonitor.State.READY) {
                        prog = (double) monitor.getWorkCompleted() / (double) monitor.getTotalWork();
                        progressView.update(
                                bundle.getString(ArchiveLangConstants.LangArchiveUpdatingFile),
                                bundle.getString(ArchiveLangConstants.LangArchiveWritingFile) + monitor.getFileName(),
                                prog
                        );
                    }
                    if (monitor.getResult() == ProgressMonitor.Result.ERROR) {
                        Alert alert = view.alert(
                                bundle.getString(ArchiveLangConstants.LangArchiveErrorTitle),
                                bundle.getString(ArchiveLangConstants.LangArchiveCannotCreateFile),
                                Alert.AlertType.ERROR
                        );
                        alert.showAndWait();
                        logger.error("failed to create archive,", monitor.getException());
                    }
                    Platform.runLater(progressView::hide);
                } catch (Exception e) {
                    logger.error("error on creating archive,", e);
                }
            });
        }
    }

}
