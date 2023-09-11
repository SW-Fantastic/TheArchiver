package org.swdc.archive;

import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;
import org.swdc.archive.splash.SplashScene;
import org.swdc.archive.views.StartView;
import org.swdc.dependency.DependencyContext;
import org.swdc.fx.FXApplication;
import org.swdc.fx.FXResources;
import org.swdc.fx.SWFXApplication;
import org.swdc.ours.common.PackageResources;
import org.swdc.ours.common.PlatformLoader;

import java.io.File;

/**
 * 应用启动和依赖控制的类
 * @author SW-Fantastic
 */
@SWFXApplication(assetsFolder = "./assets",
        splash = SplashScene.class,
        configs = { ArchiverConfig.class },
        icons = { "package16.png","package24.png","package32.png","package64.png","package128.png","package256.png","package512.png" })
public class ArchiverApplication extends FXApplication {


    @Override
    public void onStarted(DependencyContext dependencyContext) {

        FXResources resources = dependencyContext.getByClass(FXResources.class);
        File assetFolder = resources.getAssetsFolder();
        File platformSevenZip = new File(assetFolder.getAbsolutePath() + File.separator + "platform" + File.separator + "sevenzip");
        if (!platformSevenZip.exists()) {
            if(platformSevenZip.mkdirs()) {
                File libFile = new File(assetFolder.getAbsolutePath() + File.separator + "sevenzip.zip");
                if (!libFile.exists()) {
                    logger.error("no seven zip native library archive, can not start application.");
                    System.exit(0);
                    return;
                }
                PackageResources.extractZipFromFile(
                        libFile,platformSevenZip
                );
            }
        }

        PlatformLoader loader = new PlatformLoader();
        loader.load(new File(platformSevenZip.getAbsolutePath() + File.separator + "native-deps.xml"));

        try {
            SevenZip.initLoadedLibraries();
        } catch (SevenZipNativeInitializationException e) {
            logger.error("failed to init seven-zip, exiting...", e);
            System.exit(0);
        }

        StartView startView = dependencyContext.getByClass(StartView.class);
        startView.show();
    }

}
