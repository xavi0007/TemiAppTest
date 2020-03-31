package com.example.axus.temiapptest.Controller;

import android.os.Handler;
import android.util.Log;

import com.example.axus.temiapptest.Interfaces.Skillset;
import com.example.axus.temiapptest.Interfaces.adaptorTopics;
import com.example.axus.temiapptest.Model.MapModel;
import com.example.axus.temiapptest.Model.RobotTask;
import com.robotemi.sdk.BatteryData;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;


// Please edit this class to fit the robot Skillset and SDK
public class RobotSkillSet extends Robot implements Skillset, adaptorTopics {

    private String TAG = "RobotSkillSet";
    private final String INSIDE_ELEVATOR_POS = "lift";
    public TemiRobot temiRobot;

    public RobotSkillSet()
    {

        CompositeSubscription subscriptions = new CompositeSubscription();
        // Need to delay a bit for other classes to initialise.
        subscriptions.add(Observable.timer(1000, TimeUnit.MILLISECONDS).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                initialiseRobot(temiRobot);
            }
        }));
    }
    @Override
    public void gotoBroadcastSkill() {
        // Modify this part to interface with RobotSDK

    }

    @Override
    public void gotoSkill(@NotNull RobotTask robotTask) throws JSONException {
        temiRobot.goToDestination(robotTask.getPositionName());
    }


    public void setTemiRobot(TemiRobot temiRobot) {
        this.temiRobot = temiRobot;
    }


    @Override
    public void liftOperationMapSwitch(int status, String message) {

    }

    @Override
    public void liftOperationLocalise(int status, String message) {
    }


    @Override
    public void cancelNavigation() {
        // Modify this part to interface with RobotSDK
        temiRobot.stopMovement();
        temiRobot.goToDestination("homebase");
    }


    @Override
    public void generateRobotStatusTopic() {
        // Modify this part to interface with RobotSDK
        MapModel mapModel = new MapModel();
        Double[] displayPosition = { (double)1,(double)1,(double)1};
        BatteryData batteryInfo = temiRobot.getBatteryData();
        String mapVersionId = "546c3055-6742-4ec3-8fb9-c2101fe50a7a";
//        Log.d(TAG, "MAIN Battery Info is " + batteryInfo.getBatteryPercentage());
//        Log.d(TAG,"MAIN MapVerID is " + mapVersionId);
//        Log.d(TAG, "MAIN Position X is " + displayPosition[0]);
//        Log.d(TAG, "MAIN Position Y is " + displayPosition[1]);
//        Log.d(TAG, "MAIN Position Heading is " + displayPosition[2]);
        mqttHelper.publishRobotStatus(batteryInfo.getBatteryPercentage(), mapVersionId, displayPosition[0], displayPosition[1], displayPosition[2]);
    }

    //Use this function to do task for robot through adaptor
    @Override
    public void generateRobotTaskGotoRequestTopic(int level, String destinationName) {
        // Modify this part to interface with RobotSDK

        RobotTask robotTask = new RobotTask();
        robotTask.setTaskType("GOTO");
        robotTask.setMapVerId(mapPointHelper.getMapVersionIdFromMapName(mapPointHelper.mapNameByLevelHashmap.get(level)));
        robotTask.setPositionName(destinationName);
        robotTask.setX(0);
        robotTask.setY(0);
        robotTask.setHeading(0);
        robotTask.setTts("");


        // if same level, we just send the request to adaptor, if it is different level, we need to send request to RFM
        if(level == currentLevel)
        {
            mqttHelper.publishRobotTaskRequest(true,false,"HIGH",robotTask); // Just navigate on same floor.
        }
        else
        {
            mqttHelper.publishRobotTaskRequest(true,true,"HIGH",robotTask); // Just navigate on same floor.
        }
    }

    // --------------- Handler to publish Robot Staus Topics --------------
    private static final int postRobotStatusinterval = 1000; // Wait 1 second
    Handler mHandlerPostRobotStatus = new Handler();
    // publish robot status
    //Async Task
    Runnable mHandlerTaskPostRobotStatus = new Runnable() {

        int i = 0;

        @Override
        public void run() {
            i++;
            generateRobotStatusTopic();
            mHandlerPostRobotStatus.postDelayed(mHandlerTaskPostRobotStatus, postRobotStatusinterval);
        }
    };


    void startPublishingRobotStatus() {
        mHandlerTaskPostRobotStatus.run();
    }

    void stopPublishingRobotStatus() {
        mHandlerPostRobotStatus.removeCallbacks(mHandlerTaskPostRobotStatus);
    }
}
