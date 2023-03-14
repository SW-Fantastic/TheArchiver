package org.swdc.archive.splash;

import org.swdc.fx.FXResources;
import org.swdc.fx.SwingSplashView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class SplashScene extends SwingSplashView {

    private JWindow splash;

    public SplashScene(FXResources resources) {
        super(resources);
    }

    @Override
    public JWindow getSplash() {
        if (splash != null) {
            return splash;
        }
        JWindow window = new JWindow();
        window.setBackground(new Color(0,0,0,0));
        ImageIcon image = new ImageIcon();

        try (InputStream in = new FileInputStream(resources.getAssetsFolder().getAbsolutePath() + File.separator +"splash.png")){
            image.setImage(ImageIO.read(in));
            JLabel imgLab = new JLabel(image);
            window.setContentPane(imgLab);
            window.setSize(image.getIconWidth(),image.getIconHeight());
            window.setLocationRelativeTo(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.splash = window;
        return window;
    }

}
