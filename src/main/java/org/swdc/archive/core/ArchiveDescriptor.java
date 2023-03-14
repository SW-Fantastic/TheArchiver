package org.swdc.archive.core;

import javafx.stage.FileChooser;
import org.swdc.archive.views.ArchiveView;
import org.swdc.archive.views.CompressView;
import org.swdc.dependency.annotations.ImplementBy;

import java.io.File;

/**
 * 压缩文件描述符
 *
 * 用于创建压缩文件或者创建用于操作压缩文件的Archiver。
 * 所有支持的压缩文件描述符都应该在本接口的@ImplementBy
 * 注解中被注册。
 *
 */
@ImplementBy({
        ZipArchiverDescriptor.class,
        SevenZipArchiverDescriptor.class,
        GZipArchiverDescriptor.class,
        TarArchiverDescriptor.class
})
public interface ArchiveDescriptor {



    /**
     * 名称
     * @return
     */
    String name();

    /**
     * 返回此类型的压缩包对应的extensionFilter
     * @return
     */
    FileChooser.ExtensionFilter filter();

    /**
     * 是否支持此文件
     * @param file
     * @return
     */
    boolean support(File file);

    /**
     * 是否能够创建此类型压缩包
     * @return
     */
    boolean readonly();

    /**
     * 打开一个压缩文件
     * @param view
     * @param file
     * @return
     */
    Archive open(ArchiveView view, File file);

    /**
     * 创建一个压缩文件
     * @param view
     */
    void createArchive(CompressView view);


}
