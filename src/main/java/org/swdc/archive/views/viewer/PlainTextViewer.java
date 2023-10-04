package org.swdc.archive.views.viewer;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.swdc.archive.core.Archive;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.service.FileUIService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.viewer.views.PlainTextViewerView;
import org.swdc.dependency.annotations.Aware;
import org.swdc.dependency.annotations.MultipleImplement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

@MultipleImplement(StreamViewer.class)
public class PlainTextViewer implements StreamViewer {

    private static final List<String> extensions = Arrays.asList(
            "txt","py","c","cpp","h","java","ini","conf","cnf",
            "java","rb","json","yaml","yml","html","htm","css",
            "less","sass","scss","js","mjs","ts","go","xml",
            "md"
    );

    @Inject
    private Logger logger;

    @Inject
    private CommonService commonService;

    @Override
    public boolean support(ArchiveEntry entry) {
        if (entry == null || entry.name() == null || entry.name().isBlank()) {
            return false;
        }
        String name = entry.name().toLowerCase().trim();
        String[] parts = name.split("[.]");
        String subfix = parts[parts.length - 1];
        return extensions.contains(subfix);
    }

    @Override
    public void showPreview(ArchiveView view, Archive archive, ArchiveEntry entry) {
        InputStream inputStream = archive.getInputStream(entry);
        if (inputStream == null) {
            return;
        }
        try {

            byte[] data = inputStream.readAllBytes();
            inputStream.close();

            ByteArrayInputStream bin = new ByteArrayInputStream(data);
            Charset charset = commonService.getCharset(bin,bin.available());

            String text = new String(data,charset);

            PlainTextViewerView viewerView = view.getView(PlainTextViewerView.class);
            viewerView.showPreview(entry.name(),text);

        } catch (Exception e) {
            logger.warn("failed to open preview", e);
        }
    }


}
