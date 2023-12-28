package org.swdc.archive.core;

import jakarta.inject.Inject;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.swdc.archive.service.CommonService;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.fx.FXResources;

import java.io.File;

@MultipleImplement(ArchiveDescriptor.class)
public class RarArchiverDescriptor implements ArchiveDescriptor {

    @Inject
    private Logger logger;

    @Inject
    private FXResources resources;

    @Inject
    private CommonService commonService;

    private FileChooser.ExtensionFilter filter = null;

    @Override
    public String name() {
        return resources.getResourceBundle()
                .getString(ArchiveLangConstants.LangRarArchiveDisplayName);
    }

    @Override
    public FileChooser.ExtensionFilter filter() {
        if (filter == null) {
            filter = new FileChooser.ExtensionFilter(
                    resources.getResourceBundle()
                            .getString(ArchiveLangConstants.LangRarArchiveDisplayName),
                    "*.rar"
            );
        }
        return filter;
    }

    @Override
    public boolean support(File file) {
        return file.getName().toLowerCase().endsWith("rar");
    }

    @Override
    public boolean creatable() {
        return false;
    }

    @Override
    public Archive open(ArchiveView view, File file) {
        RarArchiver archiver = new RarArchiver(resources,file,commonService,view);
        return archiver;
    }

    @Override
    public void createArchive(CompressView view) {

    }
}
