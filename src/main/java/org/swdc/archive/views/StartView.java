package org.swdc.archive.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.FontawsomeService;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

@View(viewLocation = "views/main/StartView.fxml",title = "%stage.startView.title",resizeable = false)
public class StartView extends AbstractView {

    @Inject
    private FontawsomeService fontawsomeService;

    @PostConstruct
    public void onInit() {
        Stage stage = this.getStage();
        stage.setMinWidth(640);
        stage.setMinHeight(400);

        Button open = findById("open");
        setupButton("folder_open", open, FontSize.MIDDLE_LARGE);

        MenuButton create = findById("create");
        setupButton("plus",create,FontSize.MIDDLE_LARGE);
    }

    private void setupButton(String iconName, ButtonBase button, FontSize size) {
        button.setFont(fontawsomeService.getFont(size));
        button.setText(fontawsomeService.getFontIcon(iconName));
    }

}
