package org.swdc.archive.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.stage.Stage;
import org.swdc.archive.views.controller.CompressCreateController;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

import java.io.File;
import java.util.Collections;
import java.util.List;

@View(
        viewLocation = "views/main/CompressView.fxml",
        title = "%stage.compress.title",
        multiple = true,
        dialog = true
)
public class CompressView extends AbstractView {

    @Inject
    private Fontawsome5Service fontawsomeService;

    private boolean canceled = true;

    private String fileName;

    private String targetFolder;


    public void setCanceled(boolean canceled, String fileName, String targetFolder) {
        this.canceled = canceled;
        this.fileName = fileName;
        this.targetFolder = targetFolder;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public File getFileName() {
        return new File(fileName);
    }

    public File getTargetFolder() {
        return new File(targetFolder);
    }

    @PostConstruct
    public void init() {
        Stage stage = this.getStage();
        stage.setMinHeight(580);
        stage.setMinWidth(780);

        setupIcon(findById("addFiles"),"plus",FontSize.MIDDLE_SMALL);
        setupIcon(findById("addFolder"),"folder-open",FontSize.MIDDLE_SMALL);
        setupIcon(findById("openTarget"), "folder-open", FontSize.MIDDLE_SMALL);
        setupIcon(findById("removeFile"), "times",FontSize.MIDDLE_SMALL);

        Button removeFile = findById("removeFile");
        removeFile.setDisable(true);
    }

    public List<File> getCompressSource() {
        CompressCreateController compressCreateController = getController();
        return compressCreateController.getCompressFile();
    }

    private void setupIcon(ButtonBase button, String iconName, FontSize iconSize){
        button.setFont(fontawsomeService.getSolidFont(iconSize));
        button.setText(fontawsomeService.getFontIcon(iconName));
        button.setPadding(new Insets(4,4,4,4));
    }

    public void removable(boolean val) {
        Button removeFile = findById("removeFile");
        removeFile.setDisable(!val);
    }

}
