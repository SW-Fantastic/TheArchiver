package org.swdc.archive.views.cells;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import org.swdc.archive.core.ArchiveEntry;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;

import java.util.HashMap;
import java.util.Map;

public class IconArchiveCell extends TableCell<ArchiveEntry,ArchiveEntry> {

    private Fontawsome5Service fontawsomeService;

    private static Map<String,String> iconExtensionRegularMap = new HashMap<>();
    private static Map<String,String> iconExtensionBrandsMap = new HashMap<>();
    private static Map<String,String> iconExtensionSolidMap = new HashMap<>();


    static {
        iconExtensionRegularMap.put("txt","file-alt");

        iconExtensionRegularMap.put("png","image");
        iconExtensionRegularMap.put("jpg","image");
        iconExtensionRegularMap.put("jpeg","image");
        iconExtensionRegularMap.put("bmp","image");
        iconExtensionRegularMap.put("webp","image");
        iconExtensionRegularMap.put("gif","image");
        iconExtensionRegularMap.put("tiff","image");
        iconExtensionRegularMap.put("ico","image");

        iconExtensionRegularMap.put("rb","gem");

        iconExtensionSolidMap.put("svg","object-group");
        iconExtensionSolidMap.put("eps","object-group");

        iconExtensionRegularMap.put("zip","file-archive");
        iconExtensionRegularMap.put("gz","file-archive");
        iconExtensionRegularMap.put("rar","file-archive");
        iconExtensionRegularMap.put("7z","file-archive");
        iconExtensionRegularMap.put("xz","file-archive");
        iconExtensionRegularMap.put("tar","file-archive");

        iconExtensionRegularMap.put("wps","file-word");
        iconExtensionRegularMap.put("doc","file-word");
        iconExtensionRegularMap.put("docx","file-word");
        iconExtensionRegularMap.put("rtf","file-word");

        iconExtensionRegularMap.put("ppt","file-powerpoint");
        iconExtensionRegularMap.put("pptx","file-powerpoint");
        iconExtensionRegularMap.put("pptm","file-powerpoint");
        iconExtensionRegularMap.put("pps","file-powerpoint");

        iconExtensionRegularMap.put("xls","file-excel");
        iconExtensionRegularMap.put("xlsx","file-excel");
        iconExtensionRegularMap.put("xlsb","file-excel");
        iconExtensionRegularMap.put("csv","file-excel");

        iconExtensionSolidMap.put("mp4","film");
        iconExtensionSolidMap.put("wmv","film");
        iconExtensionSolidMap.put("mov","film");
        iconExtensionSolidMap.put("mkv","film");
        iconExtensionSolidMap.put("avi","firm");
        iconExtensionSolidMap.put("flv","firm");
        iconExtensionSolidMap.put("3gp","firm");

        iconExtensionSolidMap.put("ttf","font");
        iconExtensionSolidMap.put("otf","font");
        iconExtensionSolidMap.put("woff","font");
        iconExtensionSolidMap.put("woff2","font");

        iconExtensionBrandsMap.put("swf","facebook");

        iconExtensionBrandsMap.put("py","python");
        iconExtensionBrandsMap.put("pyc","python");

        iconExtensionSolidMap.put("dart","dice-d20");
        iconExtensionSolidMap.put("url","link");

        iconExtensionSolidMap.put("xml","cog");
        iconExtensionSolidMap.put("yml","cog");
        iconExtensionSolidMap.put("yaml","cog");
        iconExtensionSolidMap.put("properties","cog");
        iconExtensionSolidMap.put("conf","cog");
        iconExtensionSolidMap.put("ini","cog");
        iconExtensionSolidMap.put("inf","cog");
        iconExtensionSolidMap.put("cfg","cog");
        iconExtensionSolidMap.put("cnf","cog");

        iconExtensionSolidMap.put("lock","lock");
        iconExtensionSolidMap.put("pem", "shield-alt");
        iconExtensionSolidMap.put("cer", "shield-alt");
        iconExtensionSolidMap.put("srl", "shield-alt");

        iconExtensionSolidMap.put("dll","cube");
        iconExtensionSolidMap.put("dylib","cube");
        iconExtensionSolidMap.put("jnilib","cube");
        iconExtensionSolidMap.put("so","cube");

        iconExtensionSolidMap.put("sh","terminal");
        iconExtensionSolidMap.put("bat","terminal");
        iconExtensionSolidMap.put("cmd","terminal");
        iconExtensionSolidMap.put("exe","terminal");

        iconExtensionSolidMap.put("mp3","headphones");
        iconExtensionSolidMap.put("m4a","headphones");
        iconExtensionSolidMap.put("wav","headphones");
        iconExtensionSolidMap.put("wma","headphones");
        iconExtensionSolidMap.put("ogg","headphones");
        iconExtensionSolidMap.put("mid","headphones");
        iconExtensionSolidMap.put("midi","headphones");
        iconExtensionSolidMap.put("aac","headphones");
        iconExtensionSolidMap.put("amr","headphones");

        iconExtensionSolidMap.put("a","th-large");
        iconExtensionSolidMap.put("lib","th-large");

        iconExtensionBrandsMap.put("c","cuttlefish");
        iconExtensionBrandsMap.put("h","cuttlefish");
        iconExtensionBrandsMap.put("cpp","cuttlefish");
        iconExtensionBrandsMap.put("hpp","cuttlefish");
        iconExtensionBrandsMap.put("java","java");
        iconExtensionBrandsMap.put("jar","java");
        iconExtensionBrandsMap.put("class","java");

        iconExtensionBrandsMap.put("css","css3-alt");
        iconExtensionBrandsMap.put("less","less");

        iconExtensionBrandsMap.put("scss","sass");
        iconExtensionBrandsMap.put("sass","sass");

        iconExtensionBrandsMap.put("md","markdown");

        iconExtensionBrandsMap.put("js","js");
        iconExtensionBrandsMap.put("mjs","js");
        iconExtensionBrandsMap.put("ts","node-js");
        iconExtensionBrandsMap.put("json","jira");
        iconExtensionBrandsMap.put("html","html5");
        iconExtensionBrandsMap.put("htm","html5");
        iconExtensionBrandsMap.put("jsp","html5");

        iconExtensionBrandsMap.put("gitignore","git-alt");
        iconExtensionBrandsMap.put("gitmodules","git-alt");
        iconExtensionBrandsMap.put("gitattributes","git-alt");

    }

    public IconArchiveCell(Fontawsome5Service fontawsomeService) {
        this.fontawsomeService = fontawsomeService;
    }


    @Override
    protected void updateItem(ArchiveEntry item, boolean empty) {
        super.updateItem(item, empty);
        if(empty) {
            setGraphic(null);
        } else {
            HBox box = new HBox();
            box.setAlignment(Pos.CENTER);

            Label label = new Label();

            String name = item.name();

            if (name.contains(".")) {
                String extension = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
                if (iconExtensionRegularMap.containsKey(extension)) {
                    label.setFont(fontawsomeService.getRegularFont(FontSize.MIDDLE_SMALL));
                    label.setText(fontawsomeService.getFontIcon(iconExtensionRegularMap.get(extension)));
                } else if (iconExtensionBrandsMap.containsKey(extension)) {
                    label.setFont(fontawsomeService.getBrandFont(FontSize.MIDDLE_SMALL));
                    label.setText(fontawsomeService.getFontIcon(iconExtensionBrandsMap.get(extension)));
                } else if(iconExtensionSolidMap.containsKey(extension)) {
                    label.setFont(fontawsomeService.getSolidFont(FontSize.MIDDLE_SMALL));
                    label.setText(fontawsomeService.getFontIcon(iconExtensionSolidMap.get(extension)));
                } else {
                    label.setFont(fontawsomeService.getRegularFont(FontSize.MIDDLE_SMALL));
                    label.setText(fontawsomeService.getFontIcon("file"));
                }
            } else {
                label.setFont(fontawsomeService.getRegularFont(FontSize.MIDDLE_SMALL));
                label.setText(fontawsomeService.getFontIcon("file"));
            }

            box.getChildren().add(label);
            setGraphic(box);
        }
    }

}
