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

@MultipleImplement(ArchiveDescriptor.class)
public class TarArchiverDescriptor implements ArchiveDescriptor{

    private FileChooser.ExtensionFilter filter = null;

    @Inject
    private Logger logger = null;

    @Inject
    private CommonService commonService;

    @Inject
    private FXResources resources;

    @Override
    public String name() {
        return resources.getResourceBundle().getString("archive.tar.display-name");
    }

    @Override
    public boolean creatable() {
        return false;
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (filter == null) {
            filter = new FileChooser.ExtensionFilter(
                    resources.getResourceBundle().getString(ArchiveLangConstants.LangTarArchiveDisplayName),
                    "*.tar"
            );
        }
        return filter;
    }

    @Override
    public boolean support(File file) {
        return file.getName().toLowerCase().endsWith("tar");
    }

    @Override
    public Archive open(ArchiveView view, File file) {

        try {
            return new TarArchiver(resources,view,file,commonService);
        } catch (Exception e) {
            logger.error("failed to open file: " + file);
            return null;
        }
    }

    @Override
    public void createArchive(CompressView view) {

        ResourceBundle bundle = resources.getResourceBundle();
        view.show();
        if (!view.isCanceled()) {
            File targetFile = new File(view.getTargetFolder() + File.separator + view.getFileName() + ".tar");
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
            commonService.submit(() -> {

                ProgressView progressView = view.getView(ProgressView.class);
                progressView.update(
                        bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                        bundle.getString(ArchiveLangConstants.LangArchiveIndexing),
                        0
                );
                progressView.show();

                try {
                    if (source.isDirectory()) {
                        List<File> files = UIUtils.indexFolders(source);

                        FileOutputStream fos = new FileOutputStream(targetFile);
                        TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(fos);
                        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                        double curr = 0;
                        for (File file: files) {
                            if (file.isDirectory()) {
                                curr ++;
                                continue;
                            }
                            try {
                                String path = Paths.get(source.getParent())
                                        .relativize(file.toPath())
                                        .normalize()
                                        .toString();
                                if (path.startsWith("..")) {
                                    path = path.replace("..", "");
                                }

                                FileInputStream fileInputStream = new FileInputStream(file);
                                ArchiveEntry entry = tarArchiveOutputStream.createArchiveEntry(file, path);
                                tarArchiveOutputStream.putArchiveEntry(entry);
                                tarArchiveOutputStream.write(fileInputStream.readAllBytes());
                                tarArchiveOutputStream.closeArchiveEntry();
                                fileInputStream.close();

                                curr ++;
                                progressView.update(
                                        bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                        path,
                                        curr / files.size()
                                );
                            } catch (Exception e) {
                                logger.error("error on process file: " + file.getAbsolutePath(), e);
                            }
                        }

                        tarArchiveOutputStream.close();
                        fos.close();

                    } else {
                        FileInputStream fin = new FileInputStream(source);
                        FileOutputStream fos = new FileOutputStream(targetFile);
                        TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(fos);
                        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                        ArchiveEntry entry = tarArchiveOutputStream.createArchiveEntry(source,source.getName());
                        tarArchiveOutputStream.putArchiveEntry(entry);
                        double size = source.length();
                        double curr = 0;
                        byte[] buf = new byte[1024 * 1024];
                        int readSize = 0;
                        while ((readSize = fin.read(buf)) != -1) {
                            tarArchiveOutputStream.write(buf);
                            curr = curr + readSize;
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                    source.getName(),
                                    curr / size
                            );
                        }
                        tarArchiveOutputStream.closeArchiveEntry();
                        tarArchiveOutputStream.close();
                        fos.close();
                        fin.close();
                    }
                } catch (Exception e) {
                    logger.error("error on creating tar file",e);
                } finally {
                    progressView.hide();
                }

            });
        }
    }
}
