package org.swdc.archive.core;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class GZipArchiver extends SteamBasedArchiver {

    public GZipArchiver(FXResources resources,File zipFile, ArchiveView archiveView, CommonService commonService) {
        super(resources,zipFile,archiveView,commonService);
    }

    @Override
    protected String subfix() {
        return ".gz";
    }

    @Override
    protected InputStream createCompressInputstream(FileInputStream fileInputStream) throws Exception {
        return new GzipCompressorInputStream(fileInputStream);
    }


}
