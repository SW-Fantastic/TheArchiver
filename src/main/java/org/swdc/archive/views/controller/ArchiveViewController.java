package org.swdc.archive.views.controller;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.swdc.archive.ArchiverApplication;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.archive.core.UIUtils;
import org.swdc.archive.service.FileUIService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.PreferenceView;
import org.swdc.archive.views.ProgressView;
import org.swdc.archive.views.viewer.StreamViewer;
import org.swdc.dependency.annotations.Prototype;
import org.swdc.fx.ApplicationHolder;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.ViewController;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Prototype
public class ArchiveViewController extends ViewController<ArchiveView> {

    @Inject
    private FileUIService fileUIService;

    @Inject
    private ThreadPoolExecutor executor;

    @Inject
    private FXResources resources;

    @Inject
    private PreferenceView preferenceView;

    @Inject
    private List<StreamViewer> preViewers;

    @FXML
    private TableView<ArchiveEntry> fileTable;

    @FXML
    private TreeView<ArchiveEntry> archiveTree;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }


    @FXML
    public void openFile() {
        fileUIService.openFile(this.getView());
    }

    @FXML
    public void extractTreeFolder() {

        ArchiveView view = getView();
        ResourceBundle bundle = resources.getResourceBundle();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveViewDecompressInto));
        File target = directoryChooser.showDialog(view.getStage());
        if (target == null || !target.exists()) {
            return;
        }
        List<ArchiveEntry> selected = null;
        List<TreeItem<ArchiveEntry>> treeItems = archiveTree.getSelectionModel().getSelectedItems();
        if (treeItems.size() == 0) {
            selected = Arrays.asList(archiveTree.getRoot().getValue());
        } else {
            selected = treeItems
                        .stream()
                        .map(TreeItem::getValue)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        }

        if (selected.size() > 0) {
            ProgressView progressView = view.getView(ProgressView.class);

            final List<ArchiveEntry> sel = selected;
            executor.execute(() -> {
                progressView.show();
                BiConsumer<String,Double> prog = (str, val) -> {
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveDecompressing),
                            str,
                            val
                    );
                };
                view.getArchive().extract(sel,target,prog);
                Platform.runLater(progressView::hide);
            });
        }

    }

    @FXML
    public void extractFiles() {

        ArchiveView view = getView();
        ResourceBundle bundle = resources.getResourceBundle();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveViewDecompressInto));
        File target = directoryChooser.showDialog(view.getStage());
        if (target == null || !target.exists()) {
            return;
        }
        List<ArchiveEntry> selected = fileTable.getSelectionModel().getSelectedItems();
        if (selected.size() != 0) {
            ProgressView progressView = view.getView(ProgressView.class);

            final List<ArchiveEntry> sel = selected;
            executor.execute(() -> {
                progressView.show();
                BiConsumer<String,Double> prog = (str, val) -> {
                    progressView.update(
                            bundle.getString(ArchiveLangConstants.LangArchiveViewDecompressInto),
                            str,
                            val
                    );
                };
                view.getArchive().extract(sel,target,prog);
                Platform.runLater(progressView::hide);
            });
        }
    }

    @FXML
    public void showPreferenceView() {
        preferenceView.show();
    }

    @FXML
    public void extractAllFile(){
        ArchiveView view = getView();
        ResourceBundle bundle = resources.getResourceBundle();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveViewDecompressInto));
        File target = directoryChooser.showDialog(view.getStage());
        if (target == null || !target.exists()) {
            return;
        }

        List<ArchiveEntry> selected = Arrays.asList(archiveTree.getRoot().getValue());
        ProgressView progressView = view.getView(ProgressView.class);


        final List<ArchiveEntry> sel = selected;
        executor.execute(() -> {
            progressView.show();
            BiConsumer<String,Double> prog = (str, val) -> {
                progressView.update(bundle.getString(ArchiveLangConstants.LangArchiveDecompressing), str,val);
            };
            view.getArchive().extract(sel,target,prog);
            Platform.runLater(() -> {
                progressView.hide();
            });
        });

    }

    @FXML
    public void archiveTableClicked(MouseEvent event) {
        if (event.getClickCount() >= 2) {
            ArchiveEntry entry = fileTable.getSelectionModel().getSelectedItem();
            if (entry == null) {
                return;
            }
            for (StreamViewer viewer : preViewers) {
                if (viewer.support(entry)) {
                    viewer.showPreview(
                            this.getView(),
                            this.getView().getArchive(),
                            entry
                    );
                    return;
                }
            }
        }
    }

    @FXML
    public void addFile() {

        ArchiveView view = getView();
        ResourceBundle bundle = resources.getResourceBundle();
        if (!view.getArchive().editable()) {
            view.alert(
                    bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle),
                    bundle.getString(ArchiveLangConstants.LangArchiveAddingIsNotSupport),
                    Alert.AlertType.ERROR
            ).showAndWait();
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString(ArchiveLangConstants.LangArchiveAddFile));
        fileChooser.setSelectedExtensionFilter(new FileChooser
                .ExtensionFilter(bundle.getString(ArchiveLangConstants.LangArchiveAnyFile),"*.*"));
        File file = fileChooser.showOpenDialog(view.getStage());
        if (file == null || !file.exists()) {
            return;
        }
        TreeItem<ArchiveEntry> item = archiveTree
                .getSelectionModel()
                .getSelectedItem();
        if (item == null) {
            item = archiveTree.getRoot();
        }
        view.getArchive().addEntry(
                item.getValue(),
                file
        );
    }

    @FXML
    public void showAbout(){
        fileUIService.showAbout(getView());
    }

    @FXML
    public void quit() throws Exception {
        ArchiverApplication application = ApplicationHolder.getApplication(ArchiverApplication.class);
        application.stop();
    }

    @FXML
    public void deleteArchiveEntry() {

        ArchiveView view = getView();

        ResourceBundle bundle = resources.getResourceBundle();
        if (!view.getArchive().editable()) {
            view.alert(
                    bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle),
                    bundle.getString(ArchiveLangConstants.LangArchiveRemovingIsNotSupport),
                    Alert.AlertType.ERROR
            ).showAndWait();
            return;
        }

        List<ArchiveEntry> items = new ArrayList<>();
        List<ArchiveEntry> folders = new ArrayList<>();

        items.addAll(fileTable.getSelectionModel().getSelectedItems());
        if (items.isEmpty()) {
            folders = archiveTree.getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .map(TreeItem::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (folders.size() > 0) {
                for (ArchiveEntry entry : folders) {
                    List<ArchiveEntry> files = UIUtils.expandAllFolders(entry);
                    items.addAll(files);
                }
            }
        }

        if (items.isEmpty() && folders.isEmpty()) {
            return;
        }

        final List<ArchiveEntry> theRemoved = items;

        StringBuilder sb = new StringBuilder()
                .append(bundle.getString(ArchiveLangConstants.LangArchiveConflictRemovingFile))
                .append("\n\n");
        if (!folders.isEmpty()) {
            for (ArchiveEntry entry : folders) {
                sb.append(entry.name()).append("\n");
            }
        } else {
            int count = 0;
            for (ArchiveEntry entry : items) {
                if (count < 20) {
                    sb.append(entry.name()).append("\n");
                    count ++;
                } else {
                    sb.append(items.size() + bundle.getString(ArchiveLangConstants.LangArchiveFileTotals));
                    break;
                }
            }
        }

        theRemoved.addAll(folders);

        Alert alert = view.alert(bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle), sb.toString(), Alert.AlertType.CONFIRMATION);
        Optional<ButtonType> buttonType = alert.showAndWait();
        buttonType.ifPresent(typ -> {
            if (typ == ButtonType.OK) {
                if (!theRemoved.isEmpty()) {
                    view.getArchive().removeEntry(theRemoved);
                }
            }
        });
    }
}
