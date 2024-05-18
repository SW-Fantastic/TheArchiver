package org.swdc.archive.core.steamed;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.*;

public class TheTxzFile extends TheTarFile {

    public TheTxzFile(File file) {
        super(file);
    }

    @Override
    public void reloadFile() throws IOException {

        XZCompressorInputStream inputStream = new XZCompressorInputStream(new FileInputStream(file));
        TarArchiveInputStream tin = new TarArchiveInputStream(inputStream);
        TarArchiveEntry entry = null;
        while ((entry = tin.getNextTarEntry()) != null) {
            entries.add(entry);
        }
        tin.close();
        inputStream.close();

    }

    @Override
    protected InputStream openEntry(TarArchiveEntry entry) throws IOException {
        XZCompressorInputStream gzipInputStream = new XZCompressorInputStream(new FileInputStream(file));
        TarArchiveInputStream inputStream = new TarArchiveInputStream(gzipInputStream);
        TarArchiveEntry cur = null;
        while ((cur = inputStream.getNextTarEntry()) != null) {
            if (cur.getName().equals(entry.getName())) {
                ByteArrayOutputStream bot = new ByteArrayOutputStream();
                byte[] buf = new byte[1024 * 1024];
                int len = 0;
                while ((len = inputStream.read(buf)) > 0) {
                    bot.write(buf,0,len);
                }
                inputStream.close();
                gzipInputStream.close();
                return new ByteArrayInputStream(bot.toByteArray());
            }
        }
        return null;
    }

    public TarArchiveOutputStream createOutputStream(File target) throws IOException {
        return new TarArchiveOutputStream(new XZCompressorOutputStream(new FileOutputStream(target)));
    }

}
