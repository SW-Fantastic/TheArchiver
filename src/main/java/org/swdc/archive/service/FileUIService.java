package org.swdc.archive.service;

import jakarta.inject.Inject;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.swdc.archive.core.Archive;
import org.swdc.archive.core.ArchiveDescriptor;
import org.swdc.archive.core.ArchiveLangConstants;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.archive.views.StartView;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.AbstractView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FileUIService {

    private int openFileCounts = 0;

    @Inject
    private List<ArchiveDescriptor> descriptors;

    @Inject
    private StartView startView;

    @Inject
    private FXResources resources;

    @Inject
    private Logger logger;

    public boolean openFile(AbstractView view) {

        ResourceBundle bundle = resources.getResourceBundle();

        List<FileChooser.ExtensionFilter> filters = new ArrayList<>();

        FileChooser.ExtensionFilter anyFilter = new FileChooser.ExtensionFilter(bundle.getString(ArchiveLangConstants.LangAllSupportedFormate),
                descriptors
                        .stream()
                        .filter(Objects::nonNull)
                        .flatMap(desc -> desc
                                .filter()
                                .getExtensions()
                                .stream())
                        .toArray(String[]::new)
        );

        filters.add(anyFilter);
        filters.addAll(descriptors
                .stream()
                .map(ArchiveDescriptor::filter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(filters);
        File selected = chooser.showOpenDialog(view.getStage());

        if (selected == null) {
            return false;
        }

        for (ArchiveDescriptor archive: descriptors) {
            if (!archive.support(selected)) {
                continue;
            }
            ArchiveView archiveView = view.getView(ArchiveView.class);
            try {
                Archive archiver = archive.open(archiveView,selected);
                if (archiver != null) {
                    archiveView.show();
                    openFileCounts ++;
                    return true;
                } else {
                    Alert alert = view.alert(
                            bundle.getString(ArchiveLangConstants.LangArchiveErrorTitle),
                            bundle.getString(ArchiveLangConstants.LangArchiveCannotOpenFile) + "：" + selected.getAbsolutePath(),
                            Alert.AlertType.ERROR
                    );
                    alert.showAndWait();
                    return false;
                }
            } catch (Exception e) {
                logger.error("failed to open a archive file.", e);
                Alert alert = view.alert(
                        bundle.getString(ArchiveLangConstants.LangArchiveErrorTitle),
                        bundle.getString(ArchiveLangConstants.LangArchiveCannotOpenFile) + "：" + selected.getAbsolutePath(),
                        Alert.AlertType.ERROR
                );
                alert.showAndWait();
                return false;
            }
        }
        return false;
    }

    public List<MenuItem> buildArchiveCreationMenus(AbstractView view) {
        List<MenuItem> archives = new ArrayList<>();
        for (ArchiveDescriptor descriptor : descriptors) {
            if (descriptor.readonly()) {
                continue;
            }
            MenuItem item = new MenuItem(descriptor.name());
            item.setOnAction(e -> {
                CompressView compressView = view.getView(CompressView.class);
                descriptor.createArchive(compressView);
            });

            archives.add(item);
        }
        return archives;
    }

    public void archiveClosed() {
        if (openFileCounts > 0) {
            openFileCounts --;
        }
        if (openFileCounts == 0) {
            startView.show();
        }
    }


    public void showAbout(AbstractView view) {
        ResourceBundle bundle = resources.getResourceBundle();
        Alert alert = view.alert(
                bundle.getString(ArchiveLangConstants.LangArchiveMessageTitle),
                bundle.getString(ArchiveLangConstants.LangAbout),
                Alert.AlertType.INFORMATION
        );
        alert.showAndWait();
    }

}
