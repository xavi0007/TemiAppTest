package com.example.axus.temiapptest.RobotInit;

import android.app.Application;

public class App extends Application {
    private static volatile App app;

    public static String robotStateDetails;


    //Constructor for the class
    private App() {}


    /* Get Set functions*/



    //Get app object
    public static App getInstance() {
        if (app == null) {
            synchronized (App.class) {
                if (app == null) {
                    app = new App();
                }
            }
        }
        return app;
    }




}
