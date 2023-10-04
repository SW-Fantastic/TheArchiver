package org.swdc.archive.views.viewer;

import jakarta.inject.Inject;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.swdc.archive.core.Archive;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.viewer.views.ImageViewerView;
import org.swdc.dependency.annotations.MultipleImplement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@MultipleImplement(StreamViewer.class)
public class ImageViewer implements StreamViewer {

    private static final List<String> extensions = Arrays.asList(
            "png","jpg","jpeg","bmp"
    );

    @Inject
    private Logger logger;

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
        if (archive == null || entry == null || entry.getEntry() == null) {
            return;
        }
        InputStream in = archive.getInputStream(entry);
        if (in == null) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(in);
            Image result = SwingFXUtils.toFXImage(image,null);
            ImageViewerView viewerView = view.getView(ImageViewerView.class);
            viewerView.showPreview(entry.name(),result);
        } catch (Exception e) {
            logger.warn("failed to read image",e);
        }
    }
}
