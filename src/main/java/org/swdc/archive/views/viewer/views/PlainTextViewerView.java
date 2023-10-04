package org.swdc.archive.views.viewer.views;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

@View(
        viewLocation = "views/viewers/PlainTextViewerView.fxml",
        title = "%archive.viewers.preview",
        multiple = true
)
public class PlainTextViewerView extends AbstractView {

    @Inject
    private FXResources resources;

    public void showPreview(String title, String text) {
        TextArea textArea = findById("content");
        textArea.setText(text);
        Stage stage = getStage();
        stage.setTitle(
                resources
                        .getResourceBundle()
                        .getString(ArchiveLangConstants.LangPreview) + ":" + title
        );
        show();
    }

}
