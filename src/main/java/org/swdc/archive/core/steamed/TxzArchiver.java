package org.swdc.archive.core.steamed;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.swdc.archive.core.TarArchiver;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class TxzArchiver extends TarArchiver {

    public TxzArchiver(FXResources resources, File zipFile, ArchiveView archiveView, CommonService commonService) {

        super(resources, archiveView, zipFile,commonService);

    }

    @Override
    protected String getExtension() {
        String name = tarFile.getName().toLowerCase();
        if (name.endsWith("txz")) {
            return "txz";
        } else if (name.endsWith("tar.gz")) {
            return "tar.xz";
        }
        throw new RuntimeException("unknown ext name");
    }

    @Override
    protected TheTarFile getTarFile(File file) throws IOException {
        return new TheTxzFile(file);
    }

    @Override
    protected TarArchiveOutputStream createOutputStream(File file) throws IOException {
        return new TarArchiveOutputStream(new XZCompressorOutputStream(new FileOutputStream(file)));
    }

}
