package org.swdc.archive.views.controller;

import java.util.List;

public class HelpItem {

    private String title;

    private String content;

    private List<HelpItem> children;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<HelpItem> getChildren() {
        return children;
    }

    public void setChildren(List<HelpItem> children) {
        this.children = children;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
