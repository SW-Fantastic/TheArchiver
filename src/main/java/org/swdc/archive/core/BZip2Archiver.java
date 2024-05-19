package org.swdc.archive.core;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class BZip2Archiver extends SteamBasedArchiver {

    public BZip2Archiver(FXResources resources, File zipFile, ArchiveView archiveView, CommonService commonService) {
        super(resources, zipFile, archiveView, commonService);
    }

    @Override
    protected String subfix() {
        return "bz2";
    }

    @Override
    protected InputStream createCompressInputstream(FileInputStream fileInputStream) throws Exception {
        return new BZip2CompressorInputStream(fileInputStream);
    }
}
