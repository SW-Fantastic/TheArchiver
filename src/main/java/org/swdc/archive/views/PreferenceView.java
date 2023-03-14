package org.swdc.archive.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.PropertySheet;
import org.swdc.archive.ArchiverConfig;
import org.swdc.fx.FXResources;
import org.swdc.fx.config.ConfigViews;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

@View(
        viewLocation = "views/main/PreferenceView.fxml",
        dialog = true,
        resizeable = false,
        title = "%stage.pref.title"
)
public class PreferenceView extends AbstractView {

    @Inject
    private ArchiverConfig archiverConfig;

    @Inject
    private FXResources resources;

    @PostConstruct
    public void initView() {
        BorderPane root = findById("root");

        PropertySheet propertySheet = new PropertySheet();
        propertySheet.setPropertyEditorFactory(ConfigViews.factory(resources));
        propertySheet.getItems().addAll(ConfigViews.parseConfigs(resources,archiverConfig));
        propertySheet.setSearchBoxVisible(false);
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.getStyleClass().add("prop-sheet");

        root.setCenter(propertySheet);
    }

}
