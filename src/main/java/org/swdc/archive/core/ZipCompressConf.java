package org.swdc.archive.core;

import org.swdc.config.AbstractConfig;
import org.swdc.config.annotations.Property;
import org.swdc.fx.config.PropEditor;
import org.swdc.fx.config.editors.NumberEditor;
import org.swdc.fx.config.editors.SelectionEditor;


public class ZipCompressConf extends AbstractConfig  {

    @Property("compress-method")
    @PropEditor(editor = SelectionEditor.class,
            name = "%archive.zip.compress-method.name",
            description = "%archive.zip.compress-method.desc",
            resource = "DEFLATED,STORED")
    private String compressMethod = "DEFLATED";

    @Property("compress-level")
    @PropEditor(editor = NumberEditor.class,
            name = "%archive.zip.compress-level.name",
            description = "%archive.zip.compress-level.desc",
            resource = "1,5")
    private Integer level = 1;

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getCompressMethod() {
        return compressMethod;
    }

    public void setCompressMethod(String compressMethod) {
        this.compressMethod = compressMethod;
    }

}
