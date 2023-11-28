package org.swdc.archive.views.controller;

import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebView;
import org.swdc.archive.ArchiverConfig;
import org.swdc.archive.views.HelpView;
import org.swdc.fx.view.ViewController;
import org.swdc.ours.common.StreamResources;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class HelpViewController extends ViewController<HelpView> {

    @FXML
    private TreeView<HelpItem> tocTree;

    @FXML
    private WebView helpBrowser;

    @Inject
    private ArchiverConfig archiverConfig;


    public TreeItem<HelpItem> generateItems(TreeItem<HelpItem> parent, List<HelpItem> items) {
        for (HelpItem item : items) {
            TreeItem<HelpItem> treeItem = new TreeItem<>(item);
            if (item.getChildren() != null && item.getChildren().size() > 0) {
                generateItems(treeItem,item.getChildren());
            }
            parent.getChildren().add(treeItem);
        }
        return parent;
    }

    private void to(String helpName) {
        try {
            InputStream in = getClass().getModule().getResourceAsStream("views/helps/" + helpName);
            String text = StreamResources.readStreamAsString(in);
            helpBrowser.getEngine().loadContent(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String language = archiverConfig.getLanguage();
        try {
            InputStream in = this.getClass().getModule().getResourceAsStream("views/helps/TOC.json");
            HelpToc toc = StreamResources.readStreamAs(HelpToc.class,in);
            List<HelpItem> items = toc.getLangs().get(language);
            HelpItem root = new HelpItem();
            root.setTitle("Help");
            TreeItem<HelpItem> helpRoot = generateItems(new TreeItem<>(root),items);
            helpRoot.setExpanded(true);
            tocTree.setRoot(helpRoot);
            tocTree.setShowRoot(false);
            to(items.get(0).getContent());

            tocTree.getSelectionModel().selectedItemProperty().addListener(c -> {
                TreeItem<HelpItem> item = tocTree.getSelectionModel().getSelectedItem();
                if (item == null || item.getValue() == null) {
                    return;
                }
                to(item.getValue().getContent());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
