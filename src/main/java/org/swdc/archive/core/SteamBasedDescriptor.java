package org.swdc.archive.core;

import jakarta.inject.Inject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.ProgressView;
import org.swdc.fx.FXResources;

import java.io.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public abstract class SteamBasedDescriptor implements ArchiveDescriptor {

    @Inject
    protected Logger logger;

    @Inject
    private FXResources resources;


    protected abstract OutputStream createCompressStream(FileOutputStream fos) throws Exception;

    public abstract String subfix();

    @Override
    public boolean creatable() {
        return true;
    }

    @Override
    public void createArchive(CompressView view) {
        view.show();
        if (!view.isCanceled()) {

            File target = view.getTargetFolder();
            File name = view.getFileName();
            List<File> sources = view.getCompressSource();

            if (sources.isEmpty()) {
                return;
            }

            ProgressView progressView = view.getView(ProgressView.class);
            ResourceBundle bundle = resources.getResourceBundle();

            resources.getExecutor().execute(() -> {
                if (sources.size() > 1 || sources.get(0).isDirectory()) {

                    // do tar first.
                    StringBuilder targetFilePath = new StringBuilder(target.getAbsolutePath());
                    targetFilePath
                            .append(File.separator)
                            .append(name)
                            .append(".tar")
                            .append(subfix());

                    File targetFile = new File(targetFilePath.toString());

                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                            targetFile.getAbsolutePath(),
                            0.0
                    );
                    progressView.show();

                    try(
                            FileOutputStream fos = new FileOutputStream(targetFile)
                    ) {

                        OutputStream cpos = createCompressStream(fos);
                        TarArchiveOutputStream tos = new TarArchiveOutputStream(cpos);

                        List<ArchiveSource> files = sources.stream().map(ArchiveSource::new).collect(Collectors.toList());
                        int count = files.stream()
                                .mapToInt(f -> f.getFiles().size())
                                .reduce(Integer::sum)
                                .getAsInt();

                        double hasArchived = 0;

                        for (ArchiveSource item: files) {
                            try {
                                for (File itemFile: item.getFiles()) {
                                    if (itemFile.isDirectory()) {
                                        hasArchived ++;
                                        continue;
                                    }
                                    String path = item.getParent().getParentFile()
                                            .toPath().toAbsolutePath()
                                            .relativize(itemFile.toPath())
                                            .normalize()
                                            .toString();
                                    if (path.startsWith("..")) {
                                        path = path.replace("..", "");
                                    }
                                    ArchiveEntry entry = tos.createArchiveEntry(itemFile,path);
                                    tos.putArchiveEntry(entry);
                                    FileInputStream fin = new FileInputStream(itemFile);
                                    tos.write(fin.readAllBytes());
                                    tos.closeArchiveEntry();
                                    fin.close();
                                    hasArchived ++;
                                    progressView.update(
                                            bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                            path,
                                            hasArchived / count
                                    );
                                }

                            } catch (Exception e) {
                                logger.error("failed to add xz file", e);
                            }
                        }

                        tos.close();
                        cpos.close();
                        progressView.hide();

                    } catch (Exception e) {
                        logger.error("failed to create xz file");
                    }

                } else {

                    File source = sources.get(0);
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                            source.getAbsolutePath(),
                            0.0
                    );
                    progressView.show();

                    // just compress the one file
                    StringBuilder targetFilePath = new StringBuilder(target.getAbsolutePath() + File.separator);

                    if (source.getName().indexOf(".") > 0) {
                        targetFilePath
                                .append(name)
                                .append(".")
                                .append(source.getName().substring(source.getName().lastIndexOf(".") + 1));
                    }
                    targetFilePath.append(subfix());
                    File targetFile = new File(targetFilePath.toString());

                    try(
                            FileOutputStream fos = new FileOutputStream(targetFile);
                            FileInputStream fin = new FileInputStream(source);
                    ) {
                        OutputStream cpos = createCompressStream(fos);
                        double total = source.length();
                        double proceed = 0;
                        byte[] buffer = new byte[1024 * 1024 * 2];
                        int readed = 0;
                        while ((readed = fin.read(buffer)) > -1) {
                            cpos.write(buffer,0,readed);
                            proceed = proceed + readed;
                            progressView.update(
                                    bundle.getString(ArchiveLangConstants.LangArchiveInProgress),
                                    source.getAbsolutePath(),
                                    total / proceed
                            );
                        }
                        cpos.flush();
                        cpos.close();
                        progressView.hide();
                    } catch (Exception e) {
                        logger.error("failed to write data", e);
                    }

                }
            });

        }
    }

}
