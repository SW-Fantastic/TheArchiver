package org.swdc.archive.views.controller;

import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.archive.views.CompressView;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.ViewController;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CompressCreateController implements ViewController<CompressView> {

    private CompressView view;

    @FXML
    private TextField targetFilePath;

    @FXML
    private TextField sourceFilePath;

    @FXML
    private TextField fileName;

    @Inject
    private FXResources resources;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    public void openTargetFolder(){
        ResourceBundle bundle = resources.getResourceBundle();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveInto) + "...");
        File file = directoryChooser.showDialog(view.getStage());
        if (file != null) {
            targetFilePath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    public void openFolder(){
        ResourceBundle bundle = resources.getResourceBundle();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveSourceFolder));
        File file = directoryChooser.showDialog(view.getStage());
        if (file != null) {
            sourceFilePath.setText(file.getAbsolutePath());
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent != null) {
                targetFilePath.setText(parent.getAbsolutePath());
            }
            fileName.setText(file.getName());
        }
    }

    @FXML
    public void openFile(){
        ResourceBundle resourceBundle = resources.getResourceBundle();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(resourceBundle.getString(ArchiveLangConstants.LangArchiveSourceFile));
        File source = chooser.showOpenDialog(view.getStage());
        if (source != null) {
            sourceFilePath.setText(source.getAbsolutePath());
            File parent = source.getAbsoluteFile().getParentFile();
            if (parent != null) {
                targetFilePath.setText(parent.getAbsolutePath());
            }
            fileName.setText(source.getName().split("[.]")[0]);
        }
    }

    @FXML
    public void apply() {
        if (sourceFilePath.getText().isEmpty() || targetFilePath.getText().isEmpty() || fileName.getText().isEmpty()) {
            resetAndClose();
            return;
        }
        view.setCanceled(false,fileName.getText(),targetFilePath.getText(),sourceFilePath.getText());
        view.hide();
    }

    @FXML
    public void resetAndClose() {
        view.setCanceled(true,null,null,null);
        view.hide();
    }

    @Override
    public void setView(CompressView view) {
        this.view = view;
    }

    @Override
    public CompressView getView() {
        return view;
    }

}
