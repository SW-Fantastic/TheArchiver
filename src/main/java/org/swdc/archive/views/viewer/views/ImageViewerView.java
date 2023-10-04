package org.swdc.archive.views.viewer.views;

import jakarta.inject.Inject;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

@View(
        viewLocation = "views/viewers/ImageViewerView.fxml",
        title = "%archive.viewers.preview",
        multiple = true
)
public class ImageViewerView extends AbstractView {

    @Inject
    private FXResources resources;

    public void showPreview(String title, Image image) {
        Canvas view = findById("imageView");
        Stage stage = getStage();
        view.widthProperty().bind(stage.widthProperty());
        view.heightProperty().bind(stage.heightProperty());
        stage.widthProperty().addListener(w -> {
           repaint(view,stage,image);
        });
        stage.heightProperty().addListener(h -> {
            repaint(view,stage,image);
        });
        Slider slider = findById("sc");
        slider.valueProperty().addListener(v -> {
            repaint(view,stage,image);
        });
        stage.setWidth(800);
        stage.setHeight(600);
        repaint(view,stage,image);
        stage.setTitle(
                resources
                        .getResourceBundle()
                        .getString(ArchiveLangConstants.LangPreview) + ":" + title
        );
        show();
    }

    private void repaint(Canvas view,Stage stage, Image image) {
        GraphicsContext context = view.getGraphicsContext2D();
        context.clearRect(0,0,stage.getWidth(),stage.getHeight());
        double width = image.getWidth();
        double height = image.getHeight();
        Slider slider = findById("sc");
        width = width *(slider.getValue() / 50) ;
        height = height * (slider.getValue() / 50);
        context.drawImage(image,
                stage.getWidth() / 2.0 - width / 2.0,
                stage.getHeight() / 2.0 - height / 2.0,
                width,
                height
        );
    }


}
