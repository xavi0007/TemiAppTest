package com.example.axus.temiapptest.Controller;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.example.axus.temiapptest.R;
import com.example.axus.temiapptest.ViewModel.MainActivity;
import com.robotemi.sdk.BatteryData;
import com.robotemi.sdk.MediaObject;
import com.robotemi.sdk.NlpResult;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.activitystream.ActivityStreamObject;
import com.robotemi.sdk.activitystream.ActivityStreamPublishMessage;
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener;
import com.robotemi.sdk.listeners.OnConstraintBeWithStatusChangedListener;
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static com.example.axus.temiapptest.ViewModel.MainActivity.hideKeyboard;

//All temi initialization here

public class TemiRobot extends RobotSkillSet implements Robot.NlpListener, OnRobotReadyListener, Robot.ConversationViewAttachesListener, Robot.WakeupWordListener, Robot.ActivityStreamPublishListener, Robot.TtsListener, OnBeWithMeStatusChangedListener, OnGoToLocationStatusChangedListener, OnLocationsUpdatedListener, OnDetectionStateChangedListener, Robot.AsrListener, OnConstraintBeWithStatusChangedListener, AdapterView.OnItemSelectedListener{

    //variable declaration
    private Robot robot;
    List<String> locations;
    RobotSkillSet robotSkillSet;
    public Context context;

    // ------------- Robot SDK --------------------------------------
    public static int mapModel = 1; // for other function to use
    public static int mSetCurMapSectionId = 0;
    public static int mSetLocalisedId = 0;

    public TemiRobot(Context context){
        robotSkillSet = this;
        robotSkillSet.setTemiRobot(this);
        this.robot = Robot.getInstance();
        initRobot();
        super.context = context;
    }

    public void initRobot(){
        robot.addOnRobotReadyListener(this);
        robot.addNlpListener(this);
        robot.addOnBeWithMeStatusChangedListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addConversationViewAttachesListenerListener(this);
        robot.addWakeupWordListener(this);
        robot.addTtsListener(this);
        robot.addOnLocationsUpdatedListener(this);
        robot.addOnConstraintBeWithStatusChangedListener(this);
        robot.addOnDetectionStateChangedListener(this);
        robot.addAsrListener(this);
        robot.hideTopBar();
    }

    public void onDestroy(){
        robot.removeOnRobotReadyListener(this);
        robot.removeNlpListener(this);
        robot.removeOnBeWithMeStatusChangedListener(this);
        robot.removeOnGoToLocationStatusChangedListener(this);
        robot.removeConversationViewAttachesListenerListener(this);
        robot.removeWakeupWordListener(this);
        robot.removeTtsListener(this);
        robot.removeOnLocationsUpdateListener(this);
        robot.removeDetectionStateChangedListener(this);
        robot.removeAsrListener(this);
        robot.stopMovement();
    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onPublish(@NotNull ActivityStreamPublishMessage activityStreamPublishMessage) {

    }

    @Override
    public void onAsrResult(@NotNull String s) {

    }

    @Override
    public void onConversationAttaches(boolean b) {

    }

    @Override
    public void onNlpCompleted(@NotNull NlpResult nlpResult) {
        switch (nlpResult.action) {
            case "home.welcome":
                robot.tiltAngle(23, 5.3F);
                break;

            case "home.dance":
                long t = System.currentTimeMillis();
                long end = t + 5000;
                while (System.currentTimeMillis() < end) {
                    robot.skidJoy(0F, 1F);
                }
                break;

            case "home.sleep":
                robot.goTo("home base");
                break;
        }
    }

    @Override
    public void onTtsStatusChanged(@NotNull TtsRequest ttsRequest) {

    }

    @Override
    public void onWakeupWord(@NotNull String s, int i) {

    }

    @Override
    public void onConstraintBeWithStatusChanged(boolean b) {

    }

    @Override
    public void onDetectionStateChanged(int i) {

    }

    @Override
    public void onLocationsUpdated(@NotNull List<String> list) {

    }

    @Override
    public void onBeWithMeStatusChanged(@NotNull String s) {
        //  When status changes to "lock" the robot recognizes the user and begin to follow.
        switch (s) {
            case "abort":
                // do something i.e. speak
                robot.speak(TtsRequest.create("Abort", false));
                break;

            case "calculating":
                robot.speak(TtsRequest.create("Calculating", false));
                break;

            case "lock":
                robot.speak(TtsRequest.create("Lock", false));
                break;

            case "search":
                robot.speak(TtsRequest.create("search", false));
                break;

            case "start":
                robot.speak(TtsRequest.create("Start", false));
                break;

            case "track":
                robot.speak(TtsRequest.create("Track", false));
                break;
        }
    }

    //there should be a listener
    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, @NotNull String status, int descriptionId, @NotNull String description) {
        Log.d("GoToStatusChanged", "descriptionId=" + descriptionId + ", description=" + description + ", status=" + status + ", location" + location);
        switch (status) {
            case "start":
                robot.speak(TtsRequest.create("Starting", false));
                break;

            case "calculating":
                robot.speak(TtsRequest.create("Calculating", false));
                break;

            case "going":
                robot.speak(TtsRequest.create("Going", false));
                break;

            case "complete":
                if(location.equals("Cruzr")){
                    welcomeSpeech();
                }
                else{
                    robot.speak(TtsRequest.create("Completed", false));
                    robot.goTo("home base");
                    robot.speak(TtsRequest.create("Going back to home base", false));
                }
                break;

            case "abort":
                robot.speak(TtsRequest.create("Cancelled", false));
                robot.goTo("home base");
                robot.speak(TtsRequest.create("Going back to home base", false));
                break;
        }
    }

    @Override
    public void onRobotReady(boolean isReady) {
//        if (isReady) {
//            final ActivityInfo activityInfo = MainActivity.getInstance().getActivityInfos();
//            robot.onStart(activityInfo);
//        }
    }


    public void tiltAngle(View view) {
        robot.tiltAngle(23, 5.3F);
    }

    public void skidJoy(View view) {
        long t = System.currentTimeMillis();
        long end = t + 1000;
        while (System.currentTimeMillis() < end) {
            robot.skidJoy(1F, 0F);
        }
    }

    //isShowOnConversationLayer
    public void speak(String text, boolean show) {
        TtsRequest ttsRequest = TtsRequest.create(text.trim(), show);
        robot.speak(ttsRequest);
    }

    public void welcomeSpeech(){
        String str = "Hi, I am you guide";
        TtsRequest ttsRequest = TtsRequest.create(str.trim(), true);
        robot.speak(ttsRequest);
    }

    public BatteryData getBatteryData() {
        BatteryData batteryData = robot.getBatteryData();
        return batteryData;
    }
    //Movement methods

    public void stopMovement(){
        robot.stopMovement();
    }

    public void goToDestination(String destination){
        if(robot == null){
            robot =  Robot.getInstance();
        }
        for(String location : robot.getLocations()){
            if(location.equals(destination.toLowerCase().trim())){
                robot.goTo(destination.toLowerCase().trim());
                speak(("I am going to" + destination.toLowerCase().trim() + "now"),true);
                if(!destination.toLowerCase().trim().equals("homebase")){
                    speak(("Please follow me"),true);
                }
            }
            else{
                Log.d("goToDestionation","fail to find location");
            }
        }
    }


    //Location methods

    public boolean deleteLocation(String location){
        if(robot.deleteLocation(location))
        return true;
        else return false;
    }

    public boolean saveLocation(String location){
        if(robot.saveLocation(location)){
            return true;
        }else return false;
    }

    public List<String> getLocations(){
        return robot.getLocations();
    }


    //MISC
    /**
     * publishToActivityStream takes an image stored in the resources folder
     * and uploads it to the mobile application under the Activities tab.
     */
    public void publishToActivityStream(View view){
        ActivityStreamObject activityStreamObject;
        if (robot != null) {
            final String fileName = "puppy.png";
            Bitmap bm = BitmapFactory.decodeResource(MainActivity.getInstance().getResources(), R.drawable.puppy);
            File puppiesFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), fileName);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(puppiesFile);
                bm.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            activityStreamObject = ActivityStreamObject.builder()
                    .activityType(ActivityStreamObject.ActivityType.PHOTO)
                    .title("Puppy")
                    .media(MediaObject.create(MediaObject.MimeType.IMAGE, puppiesFile))
                    .build();

            robot.shareActivityObject(activityStreamObject);
            robot.speak(TtsRequest.create("Uploading Image", false));
        }
    }

    public void hideTopBar(View view) {
        robot.hideTopBar();
    }

    public void showTopBar(View view) {
        robot.showTopBar();
    }

    public static int getMapModel() {
        return mapModel;
    }

    public static void setMapModel(int mapModel) {
        TemiRobot.mapModel = mapModel;
    }

}
