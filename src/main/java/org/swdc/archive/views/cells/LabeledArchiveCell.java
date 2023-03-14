package org.swdc.archive.views.cells;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import org.swdc.archive.core.ArchiveEntry;

import java.util.function.Function;

public class LabeledArchiveCell extends TableCell<ArchiveEntry,ArchiveEntry> {

    private Function<ArchiveEntry,String> getter = null;

    public LabeledArchiveCell(Function<ArchiveEntry, String> getter) {
        this.getter = getter;
    }

    @Override
    protected void updateItem(ArchiveEntry item, boolean empty) {
        super.updateItem(item, empty);
        if(empty) {
            setGraphic(null);
        } else {
            HBox box = new HBox();
            box.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label();
            label.setText(getter.apply(item));

            box.getChildren().add(label);
            setGraphic(box);
        }
    }
}
