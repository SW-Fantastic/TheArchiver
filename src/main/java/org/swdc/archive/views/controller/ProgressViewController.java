package org.swdc.archive.views.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.swdc.archive.views.ProgressView;
import org.swdc.dependency.annotations.Prototype;
import org.swdc.fx.view.ViewController;

import java.net.URL;
import java.util.ResourceBundle;

@Prototype
public class ProgressViewController extends ViewController<ProgressView> {


    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label text;

    public void update(String title, String text, double value) {
        Platform.runLater(() -> {
            getView().getStage().setTitle(title);
            this.text.setText(text);
            this.progressBar.setProgress(value);
        });
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

}
