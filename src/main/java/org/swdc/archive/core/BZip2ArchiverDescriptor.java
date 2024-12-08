package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.stage.FileChooser;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.swdc.archive.core.steamed.TBZipArchiver;
import org.swdc.archive.core.steamed.TxzArchiver;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ResourceBundle;

@MultipleImplement(ArchiveDescriptor.class)
public class BZip2ArchiverDescriptor extends SteamBasedDescriptor {

    private FileChooser.ExtensionFilter filter = null;

    @Inject
    private CommonService commonService;

    @Inject
    private FXResources resources;

    @Override
    public String name() {
        ResourceBundle bundle = resources.getResourceBundle();
        return bundle.getString(
                ArchiveLangConstants.LangBZip2DisplayName
        );
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (filter == null) {
            ResourceBundle bundle = resources.getResourceBundle();
            filter = new FileChooser.ExtensionFilter(bundle.getString(
                    ArchiveLangConstants.LangBZip2DisplayName),
                    "*.bz2","*.tar.bz2","*.tbz2"
            );
        }
        return filter;
    }

    @Override
    public boolean support(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith("bz2") ||
                name.endsWith(".tar.bz2") ||
                name.endsWith("tbz2");
    }

    @Override
    public Archive open(ArchiveView view, File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".tar.bz2") || name.endsWith(".tbz2")) {
            return new TBZipArchiver(resources,file,view,commonService);
        } else {
            return new BZip2Archiver(resources,file,view,commonService);
        }
    }

    @Override
    protected OutputStream createCompressStream(FileOutputStream fos) throws Exception {
        return new BZip2CompressorOutputStream(fos);
    }

    @Override
    public String subfix() {
        return ".bz2";
    }

    @Override
    public boolean creatable() {
        return true;
    }
}
