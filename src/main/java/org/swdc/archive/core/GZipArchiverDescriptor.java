package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.ProgressView;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.zip.GZIPOutputStream;

@MultipleImplement(ArchiveDescriptor.class)
public class GZipArchiverDescriptor implements ArchiveDescriptor {

    private FileChooser.ExtensionFilter filter = null;

    @Inject
    private CommonService commonService;

    @Inject
    private FXResources resources;

    @Inject
    private Logger logger;

    @Override
    public boolean creatable() {
        return false;
    }

    @Override
    public String name() {
        return resources.getResourceBundle().getString(ArchiveLangConstants.LangGZipArchiveDisplayName);
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (filter == null) {
            filter = new FileChooser.ExtensionFilter(
                    resources.getResourceBundle()
                            .getString(ArchiveLangConstants.LangGZipArchiveDisplayName),
                    "*.gz"
            );
        }
        return filter;
    }

    @Override
    public boolean support(File file) {
        return file.getName().toLowerCase().endsWith("gz");
    }

    @Override
    public Archive open(ArchiveView view, File file) {
        return new GZipArchiver(resources,file,view,commonService);
    }

    @Override
    public void createArchive(CompressView view) {
        view.show();
        ResourceBundle bundle = resources.getResourceBundle();
        if (!view.isCanceled()) {
            File source = view.getSourcePath();
            String[] names = source.getName().split("[.]");
            File targetFile = new File(view.getTargetFolder() +
                    File.separator + view.getFileName() +
                    (names.length > 1 ? "." + names[names.length - 1] : "") +
                    (source.isDirectory() ? ".tar.gz": ".gz"));
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

            if (source.isDirectory()) {

                commonService.submit(() -> {
                    ProgressView progressView = view.getView(ProgressView.class);
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                            bundle.getString(ArchiveLangConstants.LangArchiveIndexing),
                            0
                    );
                    progressView.show();
                    try {

                        List<File> files = UIUtils.indexFolders(source);
                        double hasArchived = 0;

                        FileOutputStream fos = new FileOutputStream(targetFile);
                        GZIPOutputStream gout = new GZIPOutputStream(fos);
                        TarArchiveOutputStream tout = new TarArchiveOutputStream(gout);
                        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                        for (File item: files) {
                           try {
                               if (item.isDirectory()) {
                                   hasArchived ++;
                                   continue;
                               }
                               String path = Paths.get(source.getParent())
                                       .relativize(item.toPath())
                                       .normalize()
                                       .toString();
                               if (path.startsWith("..")) {
                                   path = path.replace("..", "");
                               }
                               ArchiveEntry entry = tout.createArchiveEntry(item,path);
                               tout.putArchiveEntry(entry);
                               FileInputStream fin = new FileInputStream(item);
                               tout.write(fin.readAllBytes());
                               tout.closeArchiveEntry();
                               fin.close();
                               hasArchived ++;
                               progressView.update(
                                       bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                       path,
                                       hasArchived / files.size()
                               );
                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                        }

                        tout.close();
                        gout.close();
                        fos.close();

                    } catch (Exception e) {
                        logger.error("failed to create gzip file", e);
                    } finally {
                        progressView.hide();
                    }
                });


            } else {
                ProgressView progressView = view.getView(ProgressView.class);
                progressView.update(
                        bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                        bundle.getString(ArchiveLangConstants.LangArchiveResolving),
                        0.0
                );
                commonService.submit(() -> {
                    try {
                        progressView.show();
                        FileInputStream fin = new FileInputStream(source);
                        FileOutputStream fos = new FileOutputStream(targetFile);
                        GZIPOutputStream gout = new GZIPOutputStream(fos);

                        double size = source.length();
                        byte[] buf = new byte[1024 * 1024];
                        int bufRead = 0;
                        double curr = 0;
                        while ((bufRead = fin.read(buf)) != -1) {
                            gout.write(buf);
                            curr = curr + bufRead;
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                    bundle.getString(ArchiveLangConstants.LangArchiveWritingFile),
                                    curr / size
                            );
                        }

                        gout.close();
                        fos.close();
                        fin.close();
                    } catch (Exception e) {
                       logger.error("failed to create a gzip file", e);
                    } finally {
                        progressView.hide();
                    }
                });
            }
        }
    }
}
