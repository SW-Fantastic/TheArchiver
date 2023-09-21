package org.swdc.archive.core;

import javafx.scene.control.TreeItem;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Archive<R,T extends ArchiveEntry<R>> {


    /**
     * 当前操作的压缩文件
     * @return
     */
    File getArchiveFile();

    /**
     * 构建文件树（Tree）。
     * @return
     */
    void getDictionaryTree(Consumer<TreeItem<T>> item);

    /**
     * 解压指定的内容。
     * @param extract
     * @param target
     */
    void extract(List<T> extract, File target, BiConsumer<String, Double> progressCallback);

    /**
     * 添加新的压缩文件条目
     * @param targetFolderEntry
     * @param item
     */
    void addEntry(T targetFolderEntry, File item);

    /**
     * 删除指定的压缩文件条目
     * @param entries
     */
    void removeEntry(List<T> entries);

    /**
     * 压缩文件是否支持修改，
     * 能否添加新的文件或者删除内容。
     * @return
     */
    boolean editable();

    boolean exist();

    void saveAs(File file);

    void saveFile();

    /**
     * 关闭本压缩文件，在这之后不能
     * 进行任何压缩文件操作。
     */
    void close();

    default List<ArchiveEntry<R>> getFiles(ArchiveEntry<R> entry) {
        if (entry == null) {
            return Collections.emptyList();
        }
        return entry.getFiles();
    }

    default boolean isMessyCode(String strName) {
        try {
            Pattern p = Pattern.compile("\\s*|\t*|\r*|\n*");
            Matcher m = p.matcher(strName);
            String after = m.replaceAll("");
            String temp = after.replaceAll("\\p{P}", "");
            char[] ch = temp.trim().toCharArray();

            int length = (ch != null) ? ch.length : 0;
            for (int i = 0; i < length; i++) {
                char c = ch[i];
                if (!Character.isLetterOrDigit(c)) {
                    String str = "" + ch[i];
                    if (!str.matches("[\u4e00-\u9fa5]+")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 用于通过ArchiveEntry获取输出的目标文件。
     * @param ent 被解压的ArchiveEntry
     * @param target 目标文件夹
     * @return 解压后文件应当输出的位置
     */
    default File getExtractTargetFile(ArchiveEntry<R> ent, File target) {
        ArchiveEntry<R> parent = ent.getParent();
        File targetFile = null;
        if (parent == null) {
            return new File(target.getAbsolutePath() + File.separator + ent.name());
        } else {
            ArchiveEntry<R> curr = parent;
            StringBuilder sb = new StringBuilder();
            while (curr != null && curr.getParent() != null) {
                sb.insert(0,curr.name()).insert(0, File.separator);
                curr = curr.getParent();
            }
            targetFile = new File(target.getAbsolutePath() + sb);
            if (!targetFile.exists()) {
                targetFile.mkdirs();
            }
            return new File(targetFile.getAbsolutePath() + File.separator + ent.name());
        }
    }

}
