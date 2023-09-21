package org.swdc.archive.core;


import javafx.scene.control.TreeItem;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UIUtils {


    public static <T> void createTree(TreeItem<ArchiveEntry<T>> parent, ArchiveEntry<T> value) {

        List<ArchiveEntry<T>> folders = value.getChildrenFolder();

        for (ArchiveEntry<T> folder: folders) {
            TreeItem<ArchiveEntry<T>> item = new TreeItem<>();
            item.setValue(folder);
            parent.getChildren().add(item);
            if (folder.getChildrenFolder().size() > 0) {
                createTree(item,folder);
            }
        }

    }

    private static <T> List<ArchiveEntry<T>> doExpandFolders(ArchiveEntry<T> parent) {
        List<ArchiveEntry<T>> files = new ArrayList<>();
        if (parent.getFiles() != null) {
            files.addAll(
                    parent.getFiles()
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            );
        }
        if (parent.getChildrenFolder() != null) {
            for (ArchiveEntry<T> dict : parent.getChildrenFolder()) {
                files.addAll(doExpandFolders(dict));
            }
            files.addAll(parent.getChildrenFolder());
        }
        return files;
    }

    public static <T> List<ArchiveEntry<T>> expandAllFolders(ArchiveEntry<T> folderEntry) {
        return doExpandFolders(folderEntry);
    }

    public static List<File> indexFolders(File file) {
        if (!file.isDirectory()) {
            return Arrays.asList(file);
        } else {
            File [] files = file.listFiles();
            if (files == null) {
                return Arrays.asList(file);
            } else {
                List<File> indexed = new ArrayList<>();
                indexed.add(file);
                for (File f : files) {
                    indexed.add(f);
                    if (f.isDirectory()) {
                        List<File> subFiles = indexFolders(f);
                        indexed.addAll(subFiles);
                    }
                }
                return indexed;
            }
        }
    }

    public static String generateEntryInArchivePath(ArchiveEntry entry, String extension) {
        StringBuilder inArchivePath = new StringBuilder();

        if (entry != null) {
            ArchiveEntry<ZipArchiveEntry> parentEntry = entry;
            while (parentEntry != null && !parentEntry.name().toLowerCase().endsWith(extension.toLowerCase())) {
                if (inArchivePath.length() > 0) {
                    inArchivePath.insert(0, File.separator);
                }
                inArchivePath.insert(0,parentEntry.name());
                parentEntry = parentEntry.getParent();
            }
        }
        return inArchivePath.toString();
    }

    public static String generateFileInArchivePath(ArchiveEntry entry, String extension, File willAdded) {
        StringBuilder inArchivePath = new StringBuilder();

        if (entry == null) {
            inArchivePath.append(willAdded.getName());
        } else {
            ArchiveEntry<ZipArchiveEntry> parentEntry = entry;
            while (parentEntry != null && !parentEntry.name().toLowerCase().endsWith(extension.toLowerCase())) {
                if (inArchivePath.length() > 0) {
                    inArchivePath.insert(0, "/");
                }
                inArchivePath.insert(0,parentEntry.name());
                parentEntry = parentEntry.getParent();
            }
            inArchivePath.append(inArchivePath.length() > 0 ? "/": "").append(willAdded.getName());
        }
        return inArchivePath.toString();
    }

}
