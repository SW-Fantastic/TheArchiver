package org.swdc.archive;

import javafx.application.Application;

import java.util.concurrent.ExecutionException;

public class Launcher {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ArchiverApplication application = new ArchiverApplication();
        application.applicationLaunch(args);
    }

}


