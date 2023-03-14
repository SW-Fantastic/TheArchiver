package org.swdc.archive.views;

import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.event.Event;
import org.swdc.archive.views.controller.ProgressViewController;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

@View(viewLocation = "views/main/ProgressView.fxml",multiple = true,title = "请稍候",resizeable = false,dialog = true)
public class ProgressView extends AbstractView {

    @PostConstruct
    public void init() {
        this.getStage().setOnCloseRequest(Event::consume);
    }

    public void update(String  title, String text, double value) {
        ProgressViewController ctrl = this.getController();
        ctrl.update(title,text,value);
    }

    @Override
    public void show() {
        if (Platform.isFxApplicationThread()) {
            super.show();
        } else {
            Platform.runLater(super::show);
        }
    }

    @Override
    public void hide() {
        if (Platform.isFxApplicationThread()) {
            super.hide();
        } else {
            Platform.runLater(super::hide);
        }
    }
}
