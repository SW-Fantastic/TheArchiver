package org.swdc.archive.views.cells;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;

import java.io.File;

public class FileTreeItem extends TreeCell<File> {

    private HBox root;

    private Label lblName;

    @Override
    protected void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            if (root == null) {
                root = new HBox();
                lblName = new Label();

                root.setPadding(new Insets(2,4,2,4));
                root.setAlignment(Pos.CENTER_LEFT);
                root.getChildren().add(lblName);
            }
            lblName.setText(item.getName());
            setGraphic(root);
        }
    }
}
