package org.swdc.archive.views.viewer;

import javafx.stage.Stage;
import org.swdc.archive.core.Archive;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.views.ArchiveView;
import org.swdc.dependency.annotations.ImplementBy;

@ImplementBy({
        PlainTextViewer.class,
        ImageViewer.class
})
public interface StreamViewer {

    boolean support(ArchiveEntry entry);

    void showPreview(ArchiveView view,Archive archive, ArchiveEntry entry);

}
