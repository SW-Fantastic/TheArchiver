package org.swdc.archive.core;

import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GZipArchiver implements Archive<String,ArchiveEntry<String>> {

    private File gzFile;

    private ArchiveView view;

    private CommonService commonService;

    private FXResources resources;

    private Logger logger = LoggerFactory.getLogger(GZipArchiver.class);

    public GZipArchiver(FXResources resources,File zipFile, ArchiveView archiveView, CommonService commonService) {
        this.resources = resources;
        this.gzFile = zipFile;
        this.view = archiveView;
        this.commonService = commonService;

        view.archiver(this);
    }

    @Override
    public File getArchiveFile() {
        return gzFile;
    }

    @Override
    public void getDictionaryTree(Consumer<TreeItem<ArchiveEntry<String>>> consumer) {

        TreeItem root = new TreeItem();

        ArchiveEntry<String> entry = new ArchiveEntry<>(null);
        entry.name(p -> gzFile.getName());

        String fileName = gzFile.getName().replace(".gz","")
                .replace(".GZ","");

        ArchiveEntry<String> file = new ArchiveEntry<>(null);
        file.name(p -> fileName);

        entry.addFile(fileName,file);

        root.setValue(entry);

        consumer.accept(root);
    }

    @Override
    public void extract(List<ArchiveEntry<String>> extract, File target, BiConsumer<String,Double> progressCallback) {
        try {
            FileInputStream fin = new FileInputStream(gzFile);
            GzipCompressorInputStream gzin = new GzipCompressorInputStream(fin);
            String[] names = gzFile.getName().split("[.]");
            String extName = names.length > 2 ? names[names.length - 2] : "";

            FileOutputStream outputStream = new FileOutputStream(target.getAbsolutePath() + File.separator + names[0] + "." + extName);
            byte[] buf = new byte[1024 * 1024];

            double curr = 0;
            long size = gzin.getUncompressedCount();
            long readed = 0;
            while ((readed = gzin.read(buf)) != -1) {
                curr += readed;
                progressCallback.accept(names[0],  curr / size);
                outputStream.write(buf);
            }

            outputStream.close();
            gzin.close();

        } catch (Exception e) {
            logger.error("failed to extract file.", e);
        }
    }

    @Override
    public void addEntry(ArchiveEntry<String> targetFolderEntry, File item) {
        ResourceBundle bundle = resources.getResourceBundle();
        Alert alert = view.alert(
                bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle),
                bundle.getString(ArchiveLangConstants.LangGZipArchiveCannotAddFile),
                Alert.AlertType.ERROR
        );
        alert.showAndWait();
    }

    @Override
    public void removeEntry(List<ArchiveEntry<String>> entries) {
        ResourceBundle bundle = resources.getResourceBundle();
        Alert alert = view.alert(
                bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle),
                bundle.getString(ArchiveLangConstants.LangGZipArchiveCannotRemoveFile),
                Alert.AlertType.ERROR
        );
        alert.showAndWait();
    }

    @Override
    public boolean editable() {
        return false;
    }

    @Override
    public boolean exist() {
        return false;
    }

    @Override
    public void saveAs(File file) {

    }

    @Override
    public void saveFile() {

    }

    @Override
    public void close() {

    }

}
