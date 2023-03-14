package org.swdc.archive.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.swdc.archive.core.Archive;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.archive.service.FileUIService;
import org.swdc.archive.views.cells.IconArchiveCell;
import org.swdc.archive.views.cells.LabeledArchiveCell;
import org.swdc.archive.views.controller.ArchiveViewController;
import org.swdc.fx.FXResources;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.font.FontawsomeService;
import org.swdc.fx.font.MaterialIconsService;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

import java.util.List;
import java.util.ResourceBundle;

@View(viewLocation = "views/main/ArchiveView.fxml", title = "%stage.archive.title",multiple = true)
public class ArchiveView extends AbstractView {

    private Archive archive;

    @Inject
    private MaterialIconsService materialIconsService;

    @Inject
    private FontawsomeService fontawsomeService;

    @Inject
    private Fontawsome5Service fontawsome5Service;

    @Inject
    private FileUIService fileUIService;

    @Inject
    private FXResources resources;

    private SimpleBooleanProperty archiveEditable = new SimpleBooleanProperty(false);

    @PostConstruct
    public void init() {

        ResourceBundle bundle = resources.getResourceBundle();

        Button openButton = findById("file-open");
        setupButton("folder",openButton,FontSize.MIDDLE_LARGE);

        MenuButton extract = findById("file-extract");
        setupButton("unarchive",extract,FontSize.MIDDLE_LARGE);

        MenuItem extractAll = new MenuItem();
        extractAll.setOnAction( e -> ((ArchiveViewController) getController()).extractAllFile());
        extractAll.setText(bundle.getString(ArchiveLangConstants.LangArchiveViewDecompressAll));

        MenuItem extractSelect = new MenuItem();
        extractSelect.setOnAction(e -> ((ArchiveViewController) getController()).extractFiles());
        extractSelect.setText(bundle.getString(ArchiveLangConstants.LangArchiveViewDecompressSelectedFiles));

        MenuItem extractFolder = new MenuItem();
        extractFolder.setOnAction(e -> ((ArchiveViewController) getController()).extractTreeFolder());
        extractFolder.setText(bundle.getString(ArchiveLangConstants.LangArchiveViewDecompressSelectedFolder));

        extract.getItems().addAll(extractAll,extractFolder,extractSelect);

        Button addButton = findById("button-add");
        setupButton("add_box",addButton,FontSize.MIDDLE_LARGE);
        addButton.disableProperty().bind(archiveEditable);

        Button removeButton = findById("button-remove");
        setupButton("delete",removeButton,FontSize.MIDDLE_LARGE);
        removeButton.disableProperty().bind(archiveEditable);

        MenuItem itemAdd = findById("menuAdd");
        itemAdd.disableProperty().bind(archiveEditable);
        MenuItem itemRemove = findById("menuRemove");
        itemRemove.disableProperty().bind(archiveEditable);

        TableColumn<ArchiveEntry,ArchiveEntry> fileName = findById("fileName");
        fileName.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        fileName.setCellFactory(col -> new LabeledArchiveCell(ArchiveEntry::name));

        TableColumn<ArchiveEntry,ArchiveEntry> fileIcon = findById("file-icon");
        fileIcon.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        fileIcon.setCellFactory(col -> new IconArchiveCell(fontawsome5Service));

        TableColumn<ArchiveEntry, ArchiveEntry> fileSize = findById("filesize");
        fileSize.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        fileSize.setCellFactory(col -> new LabeledArchiveCell(ArchiveEntry::readableSize));

        TreeView<ArchiveEntry> treeView = findById("folderTree");
        treeView.getSelectionModel().selectedItemProperty().addListener(observable -> {
            TreeItem<ArchiveEntry> entryTreeItem = treeView.getSelectionModel().getSelectedItem();
            if (entryTreeItem == null) {
                return;
            }
            this.loadFile(entryTreeItem.getValue());
        });

        getStage().setOnHiding(e -> fileUIService.archiveClosed());

    }

    private void loadFile(ArchiveEntry entry) {
        TableView<ArchiveEntry> fileTable = findById("file-table");
        fileTable.getItems().clear();
        if (entry == null) {
            return;
        }
        List<ArchiveEntry> entries = archive.getFiles(entry);
        fileTable.getItems().addAll(entries);
    }

    private void setupButton(String iconName, ButtonBase button, FontSize size) {
        button.setFont(materialIconsService.getFont(size));
        button.setText(materialIconsService.getFontIcon(iconName));
    }


    public void archiver(Archive archive) {
        this.archive = archive;
        TreeItem<ArchiveEntry> root = archive.getDictionaryTree();
        TreeView<ArchiveEntry> treeView = findById("folderTree");
        treeView.setRoot(root);

        Stage stage = this.getStage();
        stage.setOnHidden(e -> archive.close());
        stage.setTitle(
                resources.getResourceBundle()
                        .getString(ArchiveLangConstants.LangArchiveViewTitle) +
                        archive.getArchiveFile().getAbsolutePath()
        );

        this.archiveEditable.set(!archive.editable());
        this.loadFile(root.getValue());
    }

    public Archive getArchive() {
        return archive;
    }

    public void reload() {
        TreeItem<ArchiveEntry> root = archive.getDictionaryTree();
        TreeView<ArchiveEntry> treeView = findById("folderTree");
        treeView.setRoot(root);

        this.loadFile(root.getValue());
    }

}
