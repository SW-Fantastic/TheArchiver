package org.swdc.archive.core;

import org.swdc.config.AbstractConfig;
import org.swdc.config.annotations.Property;
import org.swdc.fx.config.PropEditor;
import org.swdc.fx.config.editors.NumberEditor;
import org.swdc.fx.config.editors.SelectionEditor;

import java.util.ResourceBundle;


public class ZipCompressConf extends AbstractConfig  {

    @Property("compress-method")
    @PropEditor(editor = SelectionEditor.class,
            name = "%archive.zip.compress-method.name",
            description = "%archive.zip.compress-method.desc",
            resource = "DEFLATE => DEFLATE,STORE => STORE")
    private String compressMethod = "DEFLATE";

    @Property("compress-level")
    @PropEditor(editor = SelectionEditor.class,
            name = "%archive.zip.compress-level.name",
            description = "%archive.zip.compress-level.desc",
            resource = "%archive.zip.compress-level-no-archive => NO_COMPRESSION," +
                    "%archive.zip.compress-level-fastest => FASTEST," +
                    "%archive.zip.compress-level-more-faster => FASTER," +
                    "%archive.zip.compress-level-faster => FAST," +
                    "%archive.zip.compress-level-fast => MEDIUM_FAST," +
                    "%archive.zip.compress-levels-default => NORMAL," +
                    "%archive.zip.compress-level-high => HIGHER," +
                    "%archive.zip.compress-level-higher => MAXIMUM," +
                    "%archive.zip.compress-level-highest => ULTRA")
    private String level = "NORMAL";

    public ZipCompressConf(ResourceBundle resourceBundle) {
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCompressMethod() {
        return compressMethod;
    }

    public void setCompressMethod(String compressMethod) {
        this.compressMethod = compressMethod;
    }

}
