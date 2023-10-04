module archiver {

    requires swdc.application.fx;
    requires swdc.application.dependency;
    requires swdc.application.configs;

    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires java.desktop;
    requires jakarta.annotation;
    requires jakarta.inject;
    requires org.apache.commons.compress;

    requires cpdetector;
    requires juniversalchardet;
    requires jchardet;

    requires sevenzipjbinding;
    requires org.slf4j;
    requires swdc.commons;
    requires zip4j;

    opens org.swdc.archive to
            swdc.application.dependency,
            swdc.application.fx,
            swdc.application.configs,
            javafx.graphics,
            javafx.controls;

    opens org.swdc.archive.splash to
            swdc.application.dependency,
            swdc.application.fx,
            swdc.application.configs,
            javafx.graphics,
            javafx.controls;

    opens org.swdc.archive.views.viewer to
            swdc.application.dependency,
            swdc.application.fx,
            javafx.graphics,
            javafx.controls;

    opens org.swdc.archive.views.viewer.views to
            swdc.application.dependency,
            swdc.application.fx,
            javafx.graphics,
            javafx.controls;

    opens org.swdc.archive.views to
            swdc.application.dependency,
            swdc.application.fx,
            javafx.graphics,
            javafx.controls;

    opens org.swdc.archive.core to
            swdc.application.dependency,
            swdc.application.configs,
            swdc.application.fx,
            javafx.fxml,
            javafx.controls,
            org.controlsfx.controls
            ;

    opens org.swdc.archive.service to
            swdc.application.dependency,
            swdc.application.fx;

    opens lang;
    opens views.main;
    opens views.viewers;
    opens icons;

    opens org.swdc.archive.views.controller to
            javafx.controls,
            javafx.fxml,
            org.controlsfx.controls,
            swdc.application.configs,
            swdc.application.dependency,
            com.fasterxml.jackson.databind,
            swdc.application.fx;

}