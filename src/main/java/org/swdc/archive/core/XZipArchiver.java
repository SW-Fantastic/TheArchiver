package org.swdc.archive.core;

import javafx.scene.control.TreeItem;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public class XZipArchiver extends SteamBasedArchiver {


    public XZipArchiver(FXResources resources, File zipFile, ArchiveView archiveView, CommonService commonService) {
        super(resources,zipFile,archiveView,commonService);
    }

    @Override
    protected String subfix() {
        return ".xz";
    }

    @Override
    protected InputStream createCompressInputstream(FileInputStream fileInputStream) throws Exception {
        return new XZCompressorInputStream(fileInputStream);
    }

}
