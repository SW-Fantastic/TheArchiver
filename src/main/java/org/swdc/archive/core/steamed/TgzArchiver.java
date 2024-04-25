package org.swdc.archive.core.steamed;

import javafx.scene.control.TreeItem;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.swdc.archive.core.Archive;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.TarArchiver;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.fx.FXResources;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 流式目录压缩：*.tar.gz | *.tgz
 * 尽量不要直接修改大型的tgz文件，性能非常差而且很占内存。
 */
public class TgzArchiver extends TarArchiver {

    public TgzArchiver(FXResources resources, File zipFile, ArchiveView archiveView, CommonService commonService) {

        super(resources, archiveView, zipFile,commonService);

    }

    @Override
    protected String getExtension() {
        String name = tarFile.getName().toLowerCase();
        if (name.endsWith("tgz")) {
            return "tgz";
        } else if (name.endsWith("tar.gz")) {
            return "tar.gz";
        }
        throw new RuntimeException("unknown ext name");
    }

    @Override
    protected TheTarFile getTarFile(File file) throws IOException {
        return new TheTgzFile(file);
    }

    @Override
    protected TarArchiveOutputStream createOutputStream(File file) throws IOException {
        return new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
    }


}
