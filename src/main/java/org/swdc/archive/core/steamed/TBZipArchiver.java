package org.swdc.archive.core.steamed;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.swdc.archive.core.TarArchiver;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TBZipArchiver extends TarArchiver {

    public TBZipArchiver(FXResources resources, File zipFile, ArchiveView archiveView, CommonService commonService) {

        super(resources, archiveView, zipFile,commonService);

    }

    @Override
    protected String getExtension() {
        String name = tarFile.getName().toLowerCase();
        if (name.endsWith("tbz2")) {
            return "tbz2";
        } else if (name.endsWith("tar.bz2")) {
            return "tar.bz2";
        }
        throw new RuntimeException("unknown ext name");
    }

    @Override
    protected TheTarFile getTarFile(File file) throws IOException {
        return new TheTBzip2File(file);
    }

    @Override
    protected TarArchiveOutputStream createOutputStream(File file) throws IOException {
        return new TarArchiveOutputStream(new BZip2CompressorOutputStream(new FileOutputStream(file)));
    }


}
