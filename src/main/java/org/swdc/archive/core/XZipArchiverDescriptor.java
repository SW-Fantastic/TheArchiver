package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.stage.FileChooser;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.slf4j.Logger;
import org.swdc.archive.core.steamed.TxzArchiver;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;

import java.io.*;
import java.util.ResourceBundle;

@MultipleImplement(ArchiveDescriptor.class)
public class XZipArchiverDescriptor extends SteamBasedDescriptor {

    private FileChooser.ExtensionFilter filter = null;

    @Inject
    private CommonService commonService;

    @Inject
    private FXResources resources;

    @Override
    public String name() {
        ResourceBundle bundle = resources.getResourceBundle();
        return bundle.getString(
                ArchiveLangConstants.LangXZArchiveDisplayName
        );
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (filter == null) {
            ResourceBundle bundle = resources.getResourceBundle();
            filter = new FileChooser.ExtensionFilter(bundle.getString(
                    ArchiveLangConstants.LangXZArchiveDisplayName),
                    "*.xz"
            );
        }
        return filter;
    }

    @Override
    public boolean support(File file) {
        return file.getName().toLowerCase().endsWith("xz");
    }

    @Override
    public boolean creatable() {
        return true;
    }

    @Override
    public Archive open(ArchiveView view, File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".tar.xz") || name.endsWith(".txz")) {
            return new TxzArchiver(resources,file,view,commonService);
        } else {
            return new XZipArchiver(resources,file,view,commonService);
        }
    }

    @Override
    protected OutputStream createCompressStream(FileOutputStream fos) throws IOException {
        return new XZCompressorOutputStream(fos);
    }

    @Override
    public String subfix() {
        return ".xz";
    }

}
