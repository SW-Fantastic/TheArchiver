package org.swdc.archive.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 提供ArchiveView访问压缩文件条目的
 * 接口。
 *
 * 此对象需要提供压缩条目对应的Name，ModifiedDate，Size等数据
 * 以在TableView中展示。
 *
 * 另外本类包装了原本的压缩条目，基于本类可以进行各类压缩文件操作，
 * 如添加文件，删除文件等，这些功能由对应的Archiver提供。
 *
 * 把本类的对象提供给创建它的Archiver，即可完成压缩文件的各类操作。
 *
 * @param <T>
 */
public class ArchiveEntry<T> {

    private ArchiveEntry<T> parent;
    private T entry;
    private Function<T,String> nameGetter;
    private Function<T,Long> sizeGetter = T -> 0L;

    /**
     * 存放额外数据的字段，想放什么都可以。
     */
    private Object userData;

    private Map<String,ArchiveEntry<T>> folders = new HashMap<>();
    private Map<String,ArchiveEntry<T>> files = new HashMap<>();

    public ArchiveEntry(T entry) {
        this.entry = entry;
    }

    public ArchiveEntry<T> getParent() {
        return parent;
    }

    public void setParent(ArchiveEntry<T> parent) {
        this.parent = parent;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }


    public <E> E getUserData() {
        return (E)userData;
    }

    public void addFolder(String name, ArchiveEntry<T> folderEntry) {
        if (name == null || name.isEmpty()) {
            return;
        }
        if (folders.containsKey(name)) {
            return;
        }
        folders.put(name,folderEntry);
    }

    public List<ArchiveEntry<T>> getChildrenFolder() {
        return new ArrayList(folders.values());
    }

    public List<ArchiveEntry<T>> getFiles(){
        return new ArrayList<>(files.values());
    }

    public void addFile(String fileName, ArchiveEntry<T> entry) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        if (files.containsKey(fileName)) {
            return;
        }
        this.files.put(fileName,entry);
    }

    public ArchiveEntry<T> getFolder(String folderName) {
        return folders.get(folderName);
    }

    public ArchiveEntry<T> getFile(String fileName) {
        return files.get(fileName);
    }

    public String name() {
        return nameGetter.apply(entry);
    }

    public long size() {
        return sizeGetter.apply(entry);
    }

    public String readableSize() {
        long bytes = size();

        if (bytes < 1024) {
            return bytes + " Bytes";
        } else if (bytes < Math.pow(1024,2)) {
            return Math.ceil(bytes / 1024.0) + " KB";
        } else if (bytes < Math.pow(1024,3)) {
            return Math.ceil(bytes / Math.pow(1024,2)) + " MB";
        } else {
            return Math.ceil(bytes / Math.pow(1024,3)) + " GB";
        }
    }

    public void name(Function<T,String> getter) {
        this.nameGetter = getter;
    }

    public void size(Function<T,Long> getter) {
        this.sizeGetter = getter;
    }

    public T getEntry() {
        return entry;
    }

    public void setEntry(T entry) {
        this.entry = entry;
    }

    @Override
    public String toString() {
        return nameGetter.apply(entry);
    }
}
