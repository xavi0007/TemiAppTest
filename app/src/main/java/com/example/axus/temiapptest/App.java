package com.example.axus.temiapptest;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.example.axus.temiapptest.Camera.CameraActivity;
import com.example.axus.temiapptest.Tasks.RobotTask;
import com.robotemi.sdk.BatteryData;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.Point;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import detection.BitmapHandler;
import detection.MediaFileHandler;
import detection.MqttHelper;

public class App extends Application {

    private static final String TAG = "AudioConfig";
    private static volatile App app;

    //Classes
    private MainActivity mActivity;

    private static String robotStateDetails;
    public static String stateDetails = "Initialization";

    private int[] displayPosition;


    // mediafilehandler for SFTP audio broadcast
    private MediaFileHandler mMediaFileHandler;
    private File playFile;

    //MQTT and RTSP
    private MqttHelper mqttHelper;
    private Handler checkRTSPlooper;
    public String notificationDate;
    public boolean receivedNotification = false;
    public String payload;
    public String dockName = "Initial Point"; // to become a variable when support multiple charging ports
    public boolean goTocharge = false;
    public boolean localizeSuccessfulFlag = false;
    public boolean waitForAcknowledgement = false;

    //Facial Recognition
    private boolean faceDetected;
    public static boolean exceedRange = false;
    private int CAMERADISTANCETODETECT = 107 * 107; //107 = 5 meters; ensure camera can only view 5 meters
    public boolean shouldWeRunDetection = false;
    public boolean detected = false;

    //Location
    public Point currentDetectPoint = null;
    public static boolean logInState = false;
    final public String HomeBase = "home base";

    //Task
    private int totalTaskNo =0;
    private int currentTaskNo=0;
    public boolean cancelTask = false;
    public boolean taskFromRobot = false;
    private RobotTask robotTask; //This is empty, going to be a problem

    //Handler
    Handler mHandler = new Handler();
    private static final int interval = 1000; // Every 1 second

    private Context context;

    //Constructor for the class
    public App() {
        app = this;
    }

    public void onCreate(){
        super.onCreate();
        MqttHelper mqttHelper = new MqttHelper(getApplicationContext());
        Handler checkRTSPlooper = new Handler();
        startMqtt();
        if(mqttHelper.isMqttConnected()){
            startPublishing();
        }else{
            Log.d("OnCreate", "MQTT not connected");
        }
    }

    void startPublishing() {
        mHandlerTask.run();
    }
    Runnable mHandlerTask = new Runnable() {

        int i = 0;

        @Override
        public void run() {
            i++;
            publishToTopic();
            mHandler.postDelayed(mHandlerTask, interval);
        }
    };


    //TEMI no need to localize
//    public void goToLocalize(){
//        List<MapPointModel> mapPointModels = NavigationApi.get().queryAllMapPointByMapName(App.INITIAL_MAP);
//        for (MapPointModel point : mapPointModels) {
//            if (point.getPointName().equals("Initial Point")) {
//                // Divide 100: Conversion from meters to centimeters
//                mSetLocalizeId = RosRobotApi.get().syncToRos(Float.parseFloat(point.getMapX()),Float.parseFloat(point.getMapY()),Float.parseFloat(point.getTheta()));
//                //App.app.remoteCommonListenerSectionId = RosRobotApi.get().navigateRelocationStartByPos(Float.parseFloat(point.getMapX()),Float.parseFloat(point.getMapY()),Float.parseFloat(point.getTheta()));
//                stateDetails = "Idle"; // added in response to RFM requirement
//                App.app.remoteCommonListenerMessage = "Localise";
//                return;
//            }
//        }
//        App.app.TtsPlay("No initial point found on map");
//
//    }



    public void startMqtt() {
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w(TAG, serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                //Log.w(TAG, message.toString());
                //-----------------Robot Acknowledge-----------------------------------
                if (topic.equals(mqttHelper.subscribeRobotNotificationTopic)) {
                    getNotificationTask(message);
                } else if (topic.equals(mqttHelper.subscribeTaskTopic)) {
                    System.out.println("MQTT " + message);
                    getTask(message);
//                    MainActivity.notificationDetails.setText(topic);
//                    MainActivity.NotificationRespPayload.setText(message.toString());
                    /*
                    TODO use back this method to update FAQ list
                     */
                    //updateFaqlist(mqttMessage);
                } else if (topic.equalsIgnoreCase(mqttHelper.publishRobotNotificationTopic)){
//                    MainActivity.robotTaskDetails.setText(topic);
//                    MainActivity.RbNotificationPayload.setText(message.toString());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void publishToTopic() {
//        mapModel = queryMapModelByMapName(RosRobotApi.get().getCurrentMap());
        BatteryData batteryInfo = MainActivity.getInstance().getBatteryData();
        Double batteryLevel = Double.parseDouble(String.valueOf(batteryInfo.getBatteryPercentage()));
//        Position currentPosition = RosRobotApi.get().getPosition(true);
//        if (null == mapModel || null == currentPosition) {
//            return;
//        }
        String mapVersionId = "MapV1";
        displayPosition = convertMapPositionToDisplayPosition();
        Log.d(TAG, "MAIN Battery Info is " + batteryInfo.toString());
        Log.d(TAG,"MAIN MapVerID is " + mapVersionId);
        Log.d(TAG, "MAIN Position X is " + displayPosition[0]);
        Log.d(TAG, "MAIN Position Y is " + displayPosition[1]);
        Log.d(TAG, "MAIN Position Heading is " + displayPosition[2]);
        Log.i(TAG, "stateDetail is " + stateDetails);
//        mqttHelper.publishRobotStatus(batteryInfo.battery, robotTask.getMapVerId(), displayPosition[0], displayPosition[1], displayPosition[2]);
        mqttHelper.publishRobotStatus(batteryLevel, mapVersionId, displayPosition[0], displayPosition[1], displayPosition[2], stateDetails);

        //adding distance checking here as a POC for speed of algo comparison
//        VideoStream.exceedRange = distance(currentDetectPoint, new Point(displayPosition[0],displayPosition[1]));
    }

    public int[] convertMapPositionToDisplayPosition(){
        return new int[]{1,1,0};

    }
    // For Ubtech
//    public int[] convertMapPositionToDisplayPosition(MapModel mapModel, Position position) {
//        if (null == mapModel || null == position) {
//            MyLogger.mLog().e("convertMapPositionToDisplayPosition mapModel or position is null! " +
//                    "Return null!");
//
//            return null;
//        }
//
//        System.out.println("Current Pos Coordinate x: " + position.x + " y: " + position.y);
//
//        float resolution = Float.valueOf(mapModel.getResolution());
//        float mapLeftTopX = Float.valueOf(mapModel.getBmp_x());
//        float mapLeftTopY = Float.valueOf(mapModel.getBmp_y());
//
//        int x = (int) (-mapLeftTopX + position.x / resolution);
//        int y = (int) (mapLeftTopY - position.y / resolution);
//        int theta = (int) (180 * position.theta / Math.PI);
//
//        return new int[]{x, y, theta};
//    }

    private void getNotificationTask(MqttMessage mqttMessage) throws JSONException {
        JSONObject reader = new JSONObject(mqttMessage.toString());
        String ID = reader.getString("ID");
        boolean sentToRfm = reader.getBoolean("sentToRfm");
        String status = reader.getString("status");
        String details = reader.getString("details");
        if (status.equals("Acknowledged")) {
            app.waitForAcknowledgement = false;
            Log.i(TAG, "Acknowledged point");
            //gonna hve a null point error here
            if (robotTask.getTaskType().equals("GOTO_CHARGING")){
                mActivity.speak("I'm going back to charge");
                goTocharge = true;
                MainActivity.getInstance().goToDestination(HomeBase);
            }
//            else if (robotTask.getPositionName()!=null || !robotTask.getPositionName().equals("Dockseven")) {
//                NavigationApi.get().startNavigationService(robotTask.getPositionName());
//                mActivity.speak("An admin has acknowledged.");
//            }
        }else if (status!= null){
            Log.i(TAG, "\"status\" field is:" + status);
        }
    }

    private void getTask(MqttMessage mqttMessage) throws JSONException {
        JSONObject reader = new JSONObject(mqttMessage.toString());
        RobotTask robotTask = new RobotTask();
        robotTask.setModificationType(reader.getString("modificationType"));
        robotTask.setTaskType(reader.getString("taskType"));
        robotTask.setAbort(reader.getBoolean("abort"));
        totalTaskNo = (reader.getInt("totalTaskNo"));
        currentTaskNo = (reader.getInt("currTaskNo"));
        JSONObject point = reader.getJSONObject("point");
        robotTask.setMapVerId(point.getString("mapVerID"));
        robotTask.setPositionName(point.getString("positionName"));
        robotTask.setX(point.getDouble("x"));
        robotTask.setY(point.getDouble("y"));
        robotTask.setHeading(point.getInt("heading"));
        robotTask.setParameter(reader.getJSONObject("parameters"));
        String temp = "false"; // default value
        if(robotTask.getTaskType().equals("GOTO")) {
            try {
                temp = reader.getJSONObject("parameters").getString("detection").equals("true") ? "true":"false";// assume robotmanager sends as string
            }catch (Exception e){
                e.printStackTrace();
                temp = "false";
            }
//            Toast.makeText(getApplicationContext(), "detection? " + temp, Toast.LENGTH_SHORT).show();
            shouldWeRunDetection = Boolean.valueOf(temp); //| MainActivity.logInState;
        }
//        else{
//            shouldWeRunDeteection = MainActivity.logInState;
//        }
        System.out.println("Received Robot Task: " + "modificationType:" +robotTask.getModificationType()
                + " taskType:"+ robotTask.getTaskType() + " mapVerID:" + robotTask.getMapVerId()
                + " positionName:" + robotTask.getPositionName() + " x:" + robotTask.getX() + " y:" + robotTask.getY()
                + " heading:" + robotTask.getHeading());

        robotTaskExecuteAlgo(robotTask,reader);
//
    }

    /* Algorithms */
    public void robotTaskExecuteAlgo(final RobotTask robotTask, final JSONObject reader) throws JSONException {

        switch (robotTask.getTaskType()) {
            case "GOTO":
                if (robotTask.getModificationType().equals("CANCEL")) {
                    cancelTask = true;
                    MainActivity.getInstance().stopMovement();
                    taskFromRobot = false;
//                    robotWorkStatus = 0;
                }else{
                    MainActivity.getInstance().goToDestination(robotTask.getPositionName());
                }

                break;
            case "LOCALIZE":
                Log.d("RobotTaskExecuteAlgo", "No localize for this robot");
                break;
            case "GOTO_BROADCAST":
                // play audio and get sftp client here (audio handler)
                // also copy GOTO


                //-------------------------------play audio configs---------------------------------
                JSONObject parameters = robotTask.getParameters();

//                String audioFilePath = new JSONArray(parameters.getString("medias")).getJSONObject(0).getString("path");
//                String audioFilePath = parameters.getJSONArray("medias").getJSONObject(0).getString("path");
                String data = parameters.getString("medias");
                JSONArray medias = new JSONArray(data);
                String audioFilePath = medias.getJSONObject(0).getString("path");
                Log.i("New protocol", audioFilePath);

                playFile = new File(audioFilePath);

                Log.i(TAG, "File to be played is " + playFile.getName() + ", Full Path is: " + playFile.getAbsolutePath());

                Log.i(TAG, parameters.getString("bcastBeforeGOTO") + " , " + parameters.getString("bcastInBtw") + " , "  + parameters.getString("loopInBtw") + " , " + parameters.getString("bcastEndGOTO"));

                if (parameters.getString("bcastBeforeGOTO").equals("false") && parameters.getString("bcastInBtw").equals("false") && parameters.getString("loopInBtw").equals("false")){
                    //endGOTO = false;
                    mMediaFileHandler.stop();
                }
                // ------------------------------end audio configs----------------------------------
                //Then carry on moving
                if (robotTask.getModificationType().equals("CANCEL")) {
                    cancelTask = true;
                    MainActivity.getInstance().stopMovement();
                    taskFromRobot = false;
//                    robotWorkStatus = 0;
                }else{
                    MainActivity.getInstance().goToDestination(robotTask.getPositionName());
                }
                break;
            case "GOTO_CHARGING":
                /*go back home base*/
                MainActivity.getInstance().goToDestination(HomeBase);
                break;
            case "SURVEILLANCE_MODE":
                // need to set robot working status to not busy!
                if (robotTask.getModificationType().equals("CREATE"))
                {
                    mqttHelper.publishTaskStatus("SURVEILLANCE_MODE", "EXECUTING", "");
                    if(robotTask.getParameters().getString("rtsp").equals("true")){ // completed is sent within the waitRtspRun/stop
                        if (isMyServiceRunning(RtspServer.class)){
                            System.out.println("RTSP server already running!");
                            mqttHelper.publishTaskStatus("SURVEILLANCE_MODE", "COMPLETED", "RTSP server already running!");
                            break;
                        }
                        startService(new Intent(SessionBuilder.getInstance().getContext(), RtspServer.class));
                        checkRTSPlooper.postDelayed(waitRtspRun, 500);

                    }else if (robotTask.getParameters().getString("rtsp").equals("false")){
                        if (!isMyServiceRunning(RtspServer.class)){ // if already not runnning then fail the task
                            System.out.println("RTSP server already stopped!");
                            mqttHelper.publishTaskStatus("SURVEILLANCE_MODE", "COMPLETED", "RTSP server already stopped!");
                            break;
                        }
                        stopService(new Intent(SessionBuilder.getInstance().getContext(), RtspServer.class));
                        checkRTSPlooper.postDelayed(waitRtspStop, 500);
                    }
                }
            default:
                break;
        }
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
        ((Activity)SessionBuilder.getInstance().getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (app.displayPosition!= null){
                    /* UI things */
//                    MainActivity.displayPositionDetails.setText(displayPosition[0] + ", " + displayPosition[1] + ", " + displayPosition[2]);
//                    if (app.currentDetectPoint != null)
//                        MainActivity.currentDisplayDetails.setText(currentDetectPoint.x + ", " + currentDetectPoint.y + ", " + currentDetectPoint.angle);
//                    if (detected && MainActivity.distanceBtwPtsDetails.getText().equals("false (default)")) {
////                        notificationDate = new SimpleDateFormat("yyyymmdd_hhmmss").format(new Date());
//                        MainActivity.distanceBtwPtsDetails.setText(String.valueOf(App.app.notificationDate));
//                    }
//                    MainActivity.detectionYesNo.setText(String.valueOf(detected));
////                Log.i(TAG, "Updating UI thread");
//                    MainActivity.imageUploadFromTensorflow.setImageBitmap(cropCopyBitmap);
////                    MainActivity.imageUploadFromTensorflow.clearColorFilter();
                }
            }
        });
    }


    public void sendNotification(Bitmap notificationImage) {
        ((Activity)SessionBuilder.getInstance().getContext()).runOnUiThread(new Runnable() {
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
        Log.i("FileFormat", dateFormat.format(date) + ".jpg");


        try {
            bitmapHandler.save();
//			bitmapHandler.uploadFile("192.168.21.236", "robotmanager", 9300, "robotmanager", "sdcard/" + bitmapHandler.getFilename(), "/home/godzilla/mount/web/html/sftp/NCS");
//            bitmapHandler.uploadFile("192.168.21.194","sftpuser", 22,"q1w2e3r4","sdcard/" + bitmapHandler.getFilename(),"/data/sftpuser/upload");
//            bitmapHandler.uploadFile("172.18.4.35", "robotmanager", 9300, "robotmanager", "sdcard/" + bitmapHandler.getFilename(), "/home/godzilla/mount/web/html/sftp/NCS/");
            bitmapHandler.uploadFile("172.27.67.53", "robotmanager", 9300, "robotmanager", "sdcard/" + bitmapHandler.getFilename(), "/home/godzilla/mount/web/html/sftp/NCS/");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (mqttHelper.isMqttConnected()) {
                Log.i(TAG, "detected something");
//				mqttHelper.publishRbNotification("Human Detected", bitmapHandler.getFilename(), "5c899c07-7b0a-4f1c-810e-f4bb419e1547");
                CameraActivity cActivity =  CameraActivity.getInstance();
                mqttHelper.publishRbNotification("Human Detected", bitmapHandler.getFilename(), "0e1055fb-c89c-4086-b92f-abfcd20233c5", cActivity.getNhuman(), cActivity.getConfidence());
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
                mqttHelper.publishTaskStatus("SURVEILLANCE_MODE", "COMPLETED", "");
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
                mqttHelper.publishTaskStatus("SURVEILLANCE_MODE", "COMPLETED", "");
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

    public static String getRobotStateDetails() {
        return robotStateDetails;
    }

    public static void setRobotStateDetails(String robotStateDetails) {
        App.robotStateDetails = robotStateDetails;
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
