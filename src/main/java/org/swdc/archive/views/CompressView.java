package org.swdc.archive.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBase;
import javafx.stage.Stage;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

import java.io.File;

@View(viewLocation = "views/main/CompressView.fxml", title = "%stage.compress.title",multiple = true,dialog = true)
public class CompressView extends AbstractView {

    @Inject
    private Fontawsome5Service fontawsomeService;

    private boolean canceled = true;

    private String fileName;

    private String targetFolder;

    private String sourcePath;

    public void setCanceled(boolean canceled, String fileName, String targetFolder, String sourcePath) {
        this.canceled = canceled;
        this.fileName = fileName;
        this.targetFolder = targetFolder;
        this.sourcePath = sourcePath;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public File getSourcePath() {
        return new File(sourcePath);
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
        stage.setMinHeight(560);
        stage.setMinWidth(540);

        setupIcon(findById("open"),"file",FontSize.MIDDLE_SMALL);
        setupIcon(findById("openFolder"),"folder-open",FontSize.MIDDLE_SMALL);
        setupIcon(findById("openTarget"), "folder-open", FontSize.MIDDLE_SMALL);
    }

    private void setupIcon(ButtonBase button, String iconName, FontSize iconSize){
        button.setFont(fontawsomeService.getRegularFont(iconSize));
        button.setText(fontawsomeService.getFontIcon(iconName));
        button.setPadding(new Insets(4,4,4,4));
    }

}
