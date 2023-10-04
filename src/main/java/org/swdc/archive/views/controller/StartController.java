package org.swdc.archive.views.controller;

import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.swdc.archive.service.FileUIService;
import org.swdc.archive.views.HelpView;
import org.swdc.archive.views.PreferenceView;
import org.swdc.archive.views.StartView;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class StartController implements Initializable {

    @Inject
    private StartView startView;

    @Inject
    private PreferenceView preferenceView;

    @Inject
    private FileUIService fileUIService;

    @Inject
    private HelpView helpView;

    @FXML
    private MenuButton createBtn;

    @FXML
    public void onOpen() {
        if(fileUIService.openFile(startView)) {
            startView.hide();
        }
    }

    @FXML
    public void onPreference() {
        preferenceView.show();
    }

    @FXML
    public void onAbout() {
        fileUIService.showAbout(startView);
    }

    @FXML
    public void onHelp() {
        helpView.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        List<MenuItem> archives = createBtn.getItems();
        archives.clear();
        archives.addAll(fileUIService.buildArchiveCreationMenus(startView));
    }
}
