package org.swdc.archive.core;

import org.swdc.config.AbstractConfig;
import org.swdc.config.annotations.Property;
import org.swdc.fx.config.PropEditor;
import org.swdc.fx.config.editors.CheckEditor;
import org.swdc.fx.config.editors.NumberEditor;
import org.swdc.fx.config.editors.PasswordEditor;

public class SevenZipCompressConf extends AbstractConfig {

    @Property("compress-password")
    @PropEditor(editor = PasswordEditor.class,
            name = "%archive.seven-zip.compress-password.name",
            description = "%archive.seven-zip.compress-password.desc"
    )
    private String password = "";

    @Property("compress-level")
    @PropEditor(editor = NumberEditor.class,
            name = "%archive.seven-zip.compress-level.name",
            description = "%archive.seven-zip.compress-method.desc",
            resource = "1,6")
    private Integer level = 3;

    @Property("solid-compress")
    @PropEditor(editor = CheckEditor.class,
            name = "%archive.seven-zip.solid-compress.name",
            description = "%archive.seven-zip.solid-compress.desc",
            resource = "false")
    private Boolean solid = false;

    public void setSolid(Boolean solid) {
        this.solid = solid;
    }

    public Boolean getSolid() {
        return solid;
    }

    public Integer getLevel() {
        return level;
    }

    /**
     * level是ui的level，本方法将它转换为7z的内部值。
     * 0 - 复制模式（无压缩）
     * 1 - 最快
     * 3 - 快速
     * 5 - 正常
     * 7 - 最大
     * 9 - 极限
     * @return
     */
    public Integer getCompressLevel(){
        switch (level) {
            case 0:
            case 1:
                return level;
            case 2:
                return 3;
            case 3:
                return 5;
            case 4:
                return 7;
            case 5:
                return 9;
        }
        throw new RuntimeException("level incorrect.");
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
