package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.controlsfx.control.PropertySheet;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.ProgressView;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;
import org.swdc.fx.config.ConfigViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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


    @Override
    public String name() {
        return resources.getResourceBundle().getString(ArchiveLangConstants.LangZipArchiveDisplayName);
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (extensionFilter == null) {
            extensionFilter = new FileChooser.ExtensionFilter(
                    resources.getResourceBundle().getString(ArchiveLangConstants.LangZipArchiveDisplayName),
                    "*.zip"
            );
        }
        return extensionFilter;
    }

    @Override
    public boolean support(File file) {
        return file.getName().toLowerCase().endsWith("zip");
    }

    @Override
    public Archive open(ArchiveView view, File file) {
        ZipArchiver archiver = new ZipArchiver(file, view,commonService,resources);
        return archiver;
    }


    @Override
    public boolean readonly() {
        return false;
    }

    @Override
    public void createArchive(CompressView view) {

        ResourceBundle bundle = resources.getResourceBundle();

        ZipCompressConf compressConf = new ZipCompressConf();

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
                try(ZipArchiveOutputStream zot = new ZipArchiveOutputStream(targetFile)) {

                    zot.setUseZip64(Zip64Mode.AsNeeded);
                    zot.setLevel(compressConf.getLevel());
                    zot.setMethod(compressConf.getCompressMethod().equals("DEFLATED") ? ZipArchiveEntry.DEFLATED : ZipArchiveEntry.STORED);
                    zot.setEncoding("UTF8");
                    if (source.isFile()) {
                        System.err.println("is file");
                        writeFileIntoArchive(zot,source.getName(),source, p -> {
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                    source.getAbsolutePath(),
                                    p
                            );
                        });
                    } else {
                        System.err.println("is folder");
                        writeFolderIntoArchive(zot,source,source.getName(), (path,prog) -> {
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                    path,
                                    prog
                            );
                        });
                    }
                    zot.finish();
                    Platform.runLater(progressView::hide);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }


    private void writeFolderIntoArchive(ZipArchiveOutputStream zot,File folder, String name, BiConsumer<String, Double> progUpdater) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            System.err.println("no files, return.");
            return;
        }
        for (int idx = 0; idx < files.length; idx ++) {
            File file = files[idx];
            System.err.println(file.getName());
            if (file.isFile()) {
                writeFileIntoArchive(zot,name + File.separator + file.getName(),file, null);
                if (progUpdater != null) {
                    progUpdater.accept(name + File.separator + file.getName(), ((double)idx) / files.length);
                }
            } else {
                writeFolderIntoArchive(zot,file,name + File.separator + file.getName(),progUpdater);
                if (progUpdater != null) {
                    progUpdater.accept(name + File.separator + file.getName(), ((double)idx) / files.length);
                }
            }
        }
    }

    private void writeFileIntoArchive(ZipArchiveOutputStream zot, String name, File file, Consumer<Double> progUpdate) throws IOException {
        if (file.isFile()) {
            ArchiveEntry entry = zot.createArchiveEntry(file,name);
            zot.putArchiveEntry(entry);
            try(FileInputStream fin = new FileInputStream(file)) {
                byte[] buf = new byte[1024 * 4];
                double curr = 0.0;
                int len = -1;
                while ((len = fin.read(buf)) > -1) {
                    zot.write(buf,0,len);
                    curr = curr + len;
                    if (progUpdate != null) {
                        progUpdate.accept(curr / file.length());
                    }
                }
                zot.closeArchiveEntry();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
