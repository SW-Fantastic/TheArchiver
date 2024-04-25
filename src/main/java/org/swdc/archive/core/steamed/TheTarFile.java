package org.swdc.archive.core.steamed;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TheTarFile implements Closeable {

    protected List<TarArchiveEntry> entries = new ArrayList<>();

    protected File file;

    public TheTarFile(File file) {
        try {

            this.file = file;
            reloadFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reloadFile() throws IOException {
        TarArchiveInputStream inputStream = new TarArchiveInputStream(new FileInputStream(file));
        TarArchiveEntry entry = null;
        while ((entry = inputStream.getNextTarEntry()) != null) {
            entries.add(entry);
        }
        inputStream.close();
    }

    public List<TarArchiveEntry> getEntries() {
        return entries;
    }

    protected InputStream openEntry(TarArchiveEntry entry) throws IOException {
        TarArchiveInputStream inputStream = new TarArchiveInputStream(new FileInputStream(this.file));
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
                return new ByteArrayInputStream(bot.toByteArray());
            }
        }
        return null;
    }

    public InputStream createInputStream(TarArchiveEntry entry) {
        try {
            return openEntry(entry);
        } catch (Exception e) {
            return null;
        }
    }

    public TarArchiveOutputStream createOutputStream(File target) throws IOException {
        return new TarArchiveOutputStream(new FileOutputStream(target));
    }

    @Override
    public void close() throws IOException {

    }
}
