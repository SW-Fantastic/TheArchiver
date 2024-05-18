package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.swdc.archive.core.steamed.TgzArchiver;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.ProgressView;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@MultipleImplement(ArchiveDescriptor.class)
public class GZipArchiverDescriptor extends SteamBasedDescriptor {

    private FileChooser.ExtensionFilter filter = null;

    @Inject
    private CommonService commonService;

    @Inject
    private FXResources resources;

    @Inject
    private Logger logger;

    @Override
    protected OutputStream createCompressStream(FileOutputStream fos) throws Exception {
        return new GzipCompressorOutputStream(fos);
    }

    @Override
    public String subfix() {
        return ".gz";
    }

    @Override
    public boolean creatable() {
        return true;
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
                    "*.gz","*.tgz"
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
        String name = file.getName().toLowerCase();
        if (name.endsWith("tar.gz") || name.endsWith("tgz")) {
            return new TgzArchiver(resources,file,view,commonService);
        }
        return new GZipArchiver(resources,file,view,commonService);
    }

}
