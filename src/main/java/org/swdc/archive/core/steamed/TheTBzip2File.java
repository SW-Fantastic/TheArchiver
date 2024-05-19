package org.swdc.archive.core.steamed;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.*;

public class TheTBzip2File extends TheTarFile {

    public TheTBzip2File(File file) {
        super(file);
    }

    @Override
    public void reloadFile() throws IOException {

        BZip2CompressorInputStream inputStream = new BZip2CompressorInputStream(new FileInputStream(file));
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
        BZip2CompressorInputStream bZip2CompressorInputStream = new BZip2CompressorInputStream(new FileInputStream(file));
        TarArchiveInputStream inputStream = new TarArchiveInputStream(bZip2CompressorInputStream);
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
                bZip2CompressorInputStream.close();
                return new ByteArrayInputStream(bot.toByteArray());
            }
        }
        return null;
    }

    public TarArchiveOutputStream createOutputStream(File target) throws IOException {
        return new TarArchiveOutputStream(new BZip2CompressorOutputStream(new FileOutputStream(target)));
    }

}
