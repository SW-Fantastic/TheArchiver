package org.swdc.archive.core;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * 本类用于协助Archive的路径处理。
 * 主要是获取相对路径，方便各个压缩文件创建功能,
 * SevenZipJBinding使用的是本地模块，需要依赖Callback传输数据，
 * 所以不能像Tar和Gz那样方便的进行文件的处理。
 */
public class ArchiveSource {

    private File parent;

    private List<File> files;

    public ArchiveSource(File file) {
        if (file.isDirectory()) {
            files = UIUtils.indexFolders(file);
            parent = file;
        } else {
            parent = file.getParentFile();
            files = Collections.singletonList(file);
        }
    }

    public File getParent() {
        return parent;
    }

    public List<File> getFiles() {
        return files;
    }

}
