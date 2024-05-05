package org.swdc.archive.views.controller;

import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.cells.FileTreeItem;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.ViewController;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class CompressCreateController extends ViewController<CompressView> {

    @FXML
    private TextField targetFilePath;

    @FXML
    private TextField fileName;

    @FXML
    private TreeView<File> sourceFiles;

    @Inject
    private FXResources resources;

    private TreeItem<File> rootNode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.rootNode = new TreeItem<>();
        this.sourceFiles.setShowRoot(false);
        this.sourceFiles.setRoot(rootNode);
        this.sourceFiles.setCellFactory(t -> new FileTreeItem());
    }

    @FXML
    public void onTreeClicked() {
        CompressView view = getView();
        TreeItem<File> item = this.sourceFiles.getSelectionModel()
                .getSelectedItem();
        if (item == null) {
            view.removable(false);
            return;
        }
        if (item.getValue() != null && item.getValue().isDirectory()) {
            ObservableList<TreeItem<File>> child = item.getChildren();
            child.clear();
            File[] children = item.getValue().listFiles();
            if (children == null || children.length == 0) {
                return;
            }
            for (File fileItem : children) {
                item.getChildren().add(new TreeItem<>(fileItem));
            }
        }
        view.removable(
                rootNode.getChildren().contains(item)
        );
    }

    @FXML
    public void removeItem() {
        TreeItem<File> item = this.sourceFiles.getSelectionModel()
                .getSelectedItem();
        if (item == null) {
            return;
        }
        rootNode.getChildren().remove(item);
        CompressView view = getView();
        view.removable(false);
    }

    @FXML
    public void addFolder(){
        ResourceBundle bundle = resources.getResourceBundle();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveInto) + "...");
        File file = directoryChooser.showDialog(getView().getStage());
        if (file != null) {
            TreeItem<File> exist = revSearchTree(rootNode,file);
            File existFile = exist.getValue();
            if (existFile == null || !existFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                TreeItem<File> item = new TreeItem<>(file);
                exist.getChildren().add(item);
            }
            if (targetFilePath.getText().isBlank()) {
                targetFilePath.setText(file.getParentFile().getAbsolutePath());
            }
            if (fileName.getText().isBlank()) {
                fileName.setText(file.getName().split("[.]")[0]);
            }
        }
    }

    @FXML
    public void addFiles() {
        ResourceBundle resourceBundle = resources.getResourceBundle();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(resourceBundle.getString(ArchiveLangConstants.LangArchiveSourceFile));
        List<File> sources = chooser.showOpenMultipleDialog(getView().getStage());
        if (sources != null && !sources.isEmpty()) {
            for (File file : sources) {
                TreeItem<File> exist = revSearchTree(rootNode,file);
                File existFile = exist.getValue();
                if (existFile == null || !existFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                    TreeItem<File> item = new TreeItem<>(file);
                    exist.getChildren().add(item);
                }
                if (targetFilePath.getText().isBlank()) {
                    targetFilePath.setText(file.getParentFile().getAbsolutePath());
                }
                if (fileName.getText().isBlank()) {
                    fileName.setText(file.getName().split("[.]")[0]);
                }
            }
        }
    }


    private TreeItem<File> revSearchTree(TreeItem<File> item, File file) {
        for (TreeItem<File> childItem : item.getChildren()) {
            File itemFile = childItem.getValue();
            if (itemFile.isDirectory() ) {
                if (itemFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                    return childItem;
                } else if (file.getAbsolutePath().startsWith(itemFile.getAbsolutePath())) {
                    return revSearchTree(childItem,file);
                }
            } else if (itemFile.isFile() && file.isFile()) {
                if (itemFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                    return childItem;
                }
            }
        }
        return item;
    }


    public List<File> getCompressFile() {
        return rootNode.getChildren()
                .stream()
                .map(TreeItem::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @FXML
    public void openTargetFolder(){
        ResourceBundle bundle = resources.getResourceBundle();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveInto) + "...");
        File file = directoryChooser.showDialog(getView().getStage());
        if (file != null) {
            targetFilePath.setText(file.getAbsolutePath());
        }
    }


    @FXML
    public void apply() {
        if (getCompressFile().isEmpty() || targetFilePath.getText().isEmpty() || fileName.getText().isEmpty()) {
            resetAndClose();
            return;
        }
        CompressView view = getView();
        view.setCanceled(false,fileName.getText(),targetFilePath.getText());
        view.hide();
    }

    @FXML
    public void resetAndClose() {
        rootNode.getChildren().clear();
        CompressView view = getView();
        view.setCanceled(true,null,null);
        view.hide();
    }

}
