package org.swdc.archive.views.controller;

import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import org.swdc.archive.ArchiverApplication;
import org.swdc.archive.ArchiverConfig;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.archive.views.PreferenceView;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.ViewController;

import java.net.URL;
import java.util.ResourceBundle;

public class PreferenceController implements ViewController<PreferenceView> {

    @Inject
    private ArchiverConfig config;

    @Inject
    private FXResources resources;


    private PreferenceView view;

    @FXML
    public void saveConfigs() {
        ResourceBundle bundle = resources.getResourceBundle();
        Alert alert = view.alert(
                bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle),
                bundle.getString(ArchiveLangConstants.LangArchiveConfigNeedRestart),
                Alert.AlertType.INFORMATION
        );
        alert.showAndWait();
        config.save();
        view.hide();
    }

    @FXML
    public void cancel() {
        view.hide();
    }

    @Override
    public void setView(PreferenceView preferenceView) {
        view = preferenceView;
    }

    @Override
    public PreferenceView getView() {
        return view;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
