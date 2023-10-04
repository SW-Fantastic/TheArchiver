package org.swdc.archive.views.controller;

import java.util.List;
import java.util.Map;

public class HelpToc {

    private String title;

    private Map<String, List<HelpItem>> langs;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, List<HelpItem>> getLangs() {
        return langs;
    }

    public void setLangs(Map<String, List<HelpItem>> langs) {
        this.langs = langs;
    }
}
