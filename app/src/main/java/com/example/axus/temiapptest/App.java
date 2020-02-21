package com.example.axus.temiapptest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.graphics.Bitmap;
import android.app.ActivityManager;
import android.os.Handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.axus.temiapptest.Camera.CameraActivity;
import com.example.axus.temiapptest.Controller.MqttHelper;
import com.example.axus.temiapptest.Controller.TemiRobot;

import detection.BitmapHandler;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.Point;

public class App extends Application {

    //Location
    public Point currentDetectPoint = null;
    public TemiRobot robot;
    private int[] displayPosition;

    Context context;
    public static App app;

    private static final String TAG = "AudioConfig";

    //Facial Recognition
    private boolean faceDetected;
    public static boolean exceedRange = false;
    private int CAMERADISTANCETODETECT = 107 * 107; //107 = 5 meters; ensure camera can only view 5 meters
    public boolean shouldWeRunDetection = false;
    public boolean detected = false;
    public String notificationDate;

    public MqttHelper mqtt;
    private Handler checkRTSPlooper;
    public static boolean logInState = false;

    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        app = this;
    }



    public void sendAlertAlgo() {
        CameraActivity activity = CameraActivity.getInstance();
        // Stop button
//        if(sosButton){
//            Log.i("OK12", "SOS button initiated");
//            app.currentDetectPoint = new Point(app.displayPosition[0], app.displayPosition[1], app.displayPosition[2]);
//            App.app.sendNotification(cropCopyBitmap);
//            sosButton = false;
//            if (logInState == false);
////                fileToVideo();
//        }
        if (logInState == true){
            detected = activity.runDetection();
            exceedRange = distance(app.currentDetectPoint, new Point(app.displayPosition[0], app.displayPosition[1], app.displayPosition[2]));
        }
        else if (app.shouldWeRunDetection == true){
//        boolean detected = false;
            if (exceedRange) {
                detected = activity.runDetection();
                if (detected) {
                    // store point
                    currentDetectPoint = new Point(app.displayPosition[0], app.displayPosition[1], app.displayPosition[2]);
                    sendNotification(activity.getCropCopyBitmap());
                    exceedRange = false;
//                    fileToVideo();
                }
            } else if (app.currentDetectPoint != null) // got point but havent exceed range
            {
                exceedRange = distance(app.currentDetectPoint, new Point(app.displayPosition[0], app.displayPosition[1]));
                if (exceedRange) {
                    detected = activity.runDetection();
                    if (detected) {
                        currentDetectPoint = new Point(app.displayPosition[0], app.displayPosition[1], app.displayPosition[2]);
                        sendNotification(activity.getCropCopyBitmap());
                        exceedRange = false;
//                        fileToVideo();
                    }
                }
            } else {
                detected = activity.runDetection();
                if (detected) {
                    // store point
                    currentDetectPoint = new Point(app.displayPosition[0], app.displayPosition[1], app.displayPosition[2]);
                    sendNotification(activity.getCropCopyBitmap());
                    exceedRange = false;
//                    fileToVideo();
                }

            }
        }

    }

    public void sendNotification(Bitmap notificationImage) {
        ((Activity) SessionBuilder.getInstance().getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /** UI things */
                //MainActivity.currentDisplayDetails.setText(app.currentDetectPoint.x + ", " +app.currentDetectPoint.y + ", " + app.currentDetectPoint.angle);
            }
        });
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        notificationDate = dateFormat.format(date);
        BitmapHandler bitmapHandler;
//        if (cropCopyBitmap != null) {
//            bitmapHandler = new BitmapHandler(cropCopyBitmap, notificationDate + ".jpg", 1L);
//        }else{
//            bitmapHandler = new BitmapHandler(image, notificationDate + ".jpg", 1L);
//        }
        if (notificationImage != null){
            bitmapHandler = new BitmapHandler(notificationImage, notificationDate + ".jpg", 1L);
        } else{
            Log.i("Null Bitmap!", "Failed to save file due to empty bitmap.");
            return;
        }
        Log.d("FileFormat", dateFormat.format(date) + ".jpg");


        try {
            bitmapHandler.save();
//			bitmapHandler.uploadFile("192.168.21.236", "robotmanager", 9300, "robotmanager", "sdcard/" + bitmapHandler.getFilename(), "/home/godzilla/mount/web/html/sftp/NCS");
//            bitmapHandler.uploadFile("192.168.21.194","sftpuser", 22,"q1w2e3r4","sdcard/" + bitmapHandler.getFilename(),"/data/sftpuser/upload");
//            bitmapHandler.uploadFile("172.18.4.35", "robotmanager", 9300, "robotmanager", "sdcard/" + bitmapHandler.getFilename(), "/home/godzilla/mount/web/html/sftp/NCS/");
            bitmapHandler.uploadFile("172.27.67.53", "robotmanager", 9300, "robotmanager", "sdcard/" + bitmapHandler.getFilename(), "/home/godzilla/mount/web/html/sftp/NCS/");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (mqtt.isMqttConnected()) {
                Log.i(TAG, "detected something");
//				mqttHelper.publishRbNotification("Human Detected", bitmapHandler.getFilename(), "5c899c07-7b0a-4f1c-810e-f4bb419e1547");
                CameraActivity cActivity =  CameraActivity.getInstance();
                mqtt.publishRbNotification("Human Detected", bitmapHandler.getFilename(), "0e1055fb-c89c-4086-b92f-abfcd20233c5", cActivity.getNhuman(), cActivity.getConfidence());
//                if (app.goTocharge == false) {
//                    app.stopMovement();
//                    app.waitForAcknowledgement = true;
//                    app.goTocharge = false;
//                }
            } else {
                Log.w(TAG, "Error, mqtt not connected!");

            }
        }
    }

    private boolean distance(Point current, Point checkPoint){
//			if(current.x==checkPoint.x&&current.y==checkPoint.y)return false;
        int x = current.x - checkPoint.x; int y = current.y - checkPoint.y;
        return (x*x+y*y) > CAMERADISTANCETODETECT;

    }
    // Check if a service is running
    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

/* RTSP */

    Runnable waitRtspRun= new Runnable() { // this delay is to ensure RTSP is started before RFM is informed (no feedback?)
        @Override
        public void run() {
            // send robotmanager completed
            if (isMyServiceRunning(RtspServer.class)){
//                TtsPlay("Surveillance mode activate");
                mqtt.publishTaskStatus("SURVEILLANCE_MODE", "COMPLETED", "");
                try{
                    checkRTSPlooper.removeCallbacks(waitRtspRun);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                checkRTSPlooper.postDelayed(waitRtspRun, 500);
            }
        }
    };

    Runnable waitRtspStop = new Runnable() { // this delay is to ensure RTSP is started before RFM is informed (no feedback?)
        @Override
        public void run() {
            // send robotmanager completed
            if (!isMyServiceRunning(RtspServer.class)){
//                TtsPlay("Surveillance mode in stand-by");
                mqtt.publishTaskStatus("SURVEILLANCE_MODE", "COMPLETED", "");
                try{
                    checkRTSPlooper.removeCallbacks(waitRtspStop);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                checkRTSPlooper.postDelayed(waitRtspStop, 500);
            }
        }
    };





    /* Get Set functions*/

    public boolean isFaceDetected() {
        return faceDetected;
    }

    public void setFaceDetected(boolean faceDetected) {
        this.faceDetected = faceDetected;
    }

    public static String getTAG() {
        return TAG;
    }


    public int getDisplayPosition(int i) {
        return displayPosition[i];
    }

    public void setDisplayPosition(int[] displayPosition) {
        this.displayPosition = displayPosition;
    }

    //Get app object as singleton, and thread safe
    public static App getInstance() {
        if (app == null) {
            synchronized (App.class) {
                if (app == null) {
                    app = new App();
                    app.onCreate();
                }
            }
        }
        return app;
    }




}
