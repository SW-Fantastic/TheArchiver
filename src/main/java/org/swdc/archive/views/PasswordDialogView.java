package org.swdc.archive.views;

import jakarta.annotation.PostConstruct;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

@View(
        viewLocation = "views/main/PasswordDialog.fxml",
        dialog = true,
        resizeable = false,
        title = "%stage.password.title",
        multiple = true
)
public class PasswordDialogView extends AbstractView {

    private String text;

    @PostConstruct
    public void init() {
        Button btnOk = findById("ok");
        Button btnCancel = findById("cancel");
        PasswordField pwd = findById("password");

        btnOk.setOnAction(e -> {
            this.text = pwd.getText();
            hide();
        });

        btnCancel.setOnAction(e -> {
            this.text = "";
            hide();
        });

    }


    public String getText() {
        return text;
    }
}
