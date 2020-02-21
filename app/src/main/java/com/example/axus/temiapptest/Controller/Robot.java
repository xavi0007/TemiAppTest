package com.example.axus.temiapptest.Controller;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.axus.temiapptest.Model.MapPointHelper;
import com.example.axus.temiapptest.Model.RobotTask;
import com.example.axus.temiapptest.ViewModel.MainActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

// This class is for standard all robots to connect Adaptor and also set all the parameters needed for the robot.
// It should not be changed
public class Robot {

    public MqttHelper mqttHelper;
    private String TAG = "Robot";

    public RobotSkillSet robotSkillSet;
    public int currentLevel; // robot current level
    public MapPointHelper mapPointHelper; // robot map id
    public RobotTask robotTask; // robot current task
    public boolean taskFromRobot = false; // is the task from robot?
    public static int robotWorkStatus = 0; // 0 = idle, 1 = robot is working
    public boolean cancelTask = false;
    public int totalTaskNo =0; // total number of sub task in a robot task
    public int currentTaskNo=0; // current executing sub task in a robot task

    public Context context;




    public void initialiseRobot(RobotSkillSet robotSkillset)
    {
        startMqtt();
        this.robotSkillSet = robotSkillset;
        //this.context =  context;
        // initialise all the mappoint data
        mapPointHelper = new MapPointHelper();
        mapPointHelper.initialiseMapID();
    }

    // ------------------- MQTT -------------------------------------
    public void startMqtt() {
        mqttHelper = new MqttHelper(context);
        mqttHelper.setCallback(mqttCallbackListnener);
    }

    /**
     * MQTT Call back
     */
    private MqttCallbackExtended mqttCallbackListnener = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.w(TAG, "MQTT Connected to Host: " + serverURI);
        }

        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.w(TAG, message.toString());
            //mqttHelper.publishRobotStatus();
            if (topic.equals(mqttHelper.subscribeTaskTopic)) {
                getTask(message);
                System.out.println("MQTT in Robot.Java" + message);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    /**
     * Get Robot Task from MQTT
     * @param mqttMessage
     */
    public void getTask(@NotNull MqttMessage mqttMessage) throws JSONException {
        JSONObject reader = new JSONObject(mqttMessage.toString());
        RobotTask robotTask = mqttHelper.readTask(reader);
        Log.i("Skillset", "Received Robot Task: " + "modificationType:" +robotTask.getModificationType() + " taskType:"+ robotTask.getTaskType() + " mapVerID:" + robotTask.getMapVerId() + " positionName:" + robotTask.getPositionName() + " x:" + robotTask.getX() + " y:" + robotTask.getY() + " heading:" + robotTask.getHeading());
        robotTaskExecuteAlgo(robotTask);
    }

    /**
     * To execute the robot task
     * @param robotTask
     */
    public void robotTaskExecuteAlgo(final RobotTask robotTask){
        switch (robotTask.getTaskType()) {
            case "GOTO":
                if(robotTask.getModificationType().equals("CANCEL"))
                {
                    robotSkillSet.cancelNavigation();
                }
                else
                {
                    if(robotTask.isAbort())
                    {
                        // if abort is true means robot will immediately execute the task.
                        try {
                            robotSkillSet.gotoSkill(robotTask);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(totalTaskNo>1&&currentTaskNo!=1)
                    {
                        // if robot has a composite task and is currently not in its final task
                        try {
                            robotSkillSet.gotoSkill(robotTask);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else if (robotWorkStatus==1)
                    {
                        // if robot is busy
                        // need to start a thread to wait until robot status is 0 then only continue to do the job here
                        final Handler taskHandler = new Handler();
                        final Timer timer = new Timer();
                        final TimerTask statusCheck = new TimerTask() {
                            @Override
                            public void run() {
                                taskHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(robotWorkStatus==0)
                                        {
                                            cancel();
                                            timer.cancel();
                                            if(cancelTask!=true) {
                                                try {
                                                    robotSkillSet.gotoSkill(robotTask);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            else
                                            {
                                                cancelTask = false;
                                                mqttHelper.publishTaskStatus(robotTask.getTaskType(), "CANCELLED", "{\"reasonFail\":\"Action Cancelled\"}");
                                            }
                                        }
                                    }
                                });
                            }
                        };
                        timer.schedule(statusCheck,50,500);
                    }
                    else {
                        try {
                            robotSkillSet.gotoSkill(robotTask);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;

            default:
                break;
        }
    }
}
