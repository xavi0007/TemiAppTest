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
        int[] displayPosition = {0,0,0};
        BatteryData batteryInfo = temiRobot.getBatteryData();
        String mapVersionId = "1";
        Log.d(TAG, "MAIN Battery Info is " + batteryInfo.getBatteryPercentage());
        Log.d(TAG,"MAIN MapVerID is " + mapVersionId);
        Log.d(TAG, "MAIN Position X is " + displayPosition[0]);
        Log.d(TAG, "MAIN Position Y is " + displayPosition[1]);
        Log.d(TAG, "MAIN Position Heading is " + displayPosition[2]);
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
    private static final int postRobotStatusinterval = 1000; // Every 1 second
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

    // ------------------------------------------------------------------------


    // ------------- Code below are for UBTECH Cruzr Function -----------------
//    public MapModel queryMapModelByMapName(String mapName) {
//        MyLogger.mLog().d("queryMapModelByMapName mapName:" + mapName);
//        if (TextUtils.isEmpty(mapName)) {
//            MyLogger.mLog().e("queryMapModelByMapName error: mapName is null.");
//            return null;
//        } else {
//            ContentResolver contentResolver = context.getContentResolver();
//            Uri uri = Uri.parse("content://com.ubtechinc.cruzr.map.mapProvider/map");
//            String[] projection = null;
//            String selection = "map_name = ?";
//            String[] selectionArgs = new String[]{mapName};
//            Cursor cursor = contentResolver.query(uri, (String[]) projection, selection, selectionArgs, (String) null);
//            MapModel mapModel = null;
//            if (cursor != null && cursor.moveToFirst()) {
//                mapModel = new MapModel();
//                mapModel.setId(cursor.getInt(cursor.getColumnIndex("id")));
//                mapModel.setMap_name(cursor.getString(cursor.getColumnIndex("map_name")));
//                mapModel.setMap_original(cursor.getString(cursor.getColumnIndex("map_original")));
//                mapModel.setMap_modify(cursor.getString(cursor.getColumnIndex("map_modify")));
//                mapModel.setMap_data_url(cursor.getString(cursor.getColumnIndex("map_data_url")));
//                mapModel.setResolution(cursor.getString(cursor.getColumnIndex("resolution")));
//                mapModel.setBmp_x(cursor.getString(cursor.getColumnIndex("bmp_x")));
//                mapModel.setBmp_y(cursor.getString(cursor.getColumnIndex("bmp_y")));
//                mapModel.setBmp_w(cursor.getString(cursor.getColumnIndex("bmp_w")));
//                mapModel.setBmp_h(cursor.getString(cursor.getColumnIndex("bmp_h")));
//                mapModel.setDisplay_w(cursor.getString(cursor.getColumnIndex("display_w")));
//                mapModel.setDisplay_h(cursor.getString(cursor.getColumnIndex("display_h")));
//                mapModel.setRail_mode(cursor.getString(cursor.getColumnIndex("rail_mode")));
//                mapModel.setRail_mode_in_use(cursor.getString(cursor.getColumnIndex("rail_mode_in_use")));
//                boolean isCruiserRandom = cursor.getInt(cursor.getColumnIndex("cruiser_random")) == 1;
//                mapModel.setCruiser_random(isCruiserRandom);
//                cursor.close();
//            }
//
//            MyLogger.mLog().d("mapModel:" + (mapModel == null ? "is null!" : Convert.toJson(mapModel)));
//            return mapModel;
//        }
//    }
//
//    // For Ubtech
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
//
//    public float[] getMapPointPos(String rfmPosName, String curMapName) {
//        List<MapPointModel> mapPointModels = NavigationApi.get().queryAllMapPointByMapName(curMapName);
//        float pos[] = {0,0,0};
//        for (MapPointModel point : mapPointModels) {
//            if (point.getPointName().equals(rfmPosName)) {
//                // TODO: May need to divide 100: Conversion from meters to centimeters
//                // NCS NO NEED TO DIVIDE 100; SIT SIDE REQUIRES CONFIRMATION (NOT CHECKED)
//                pos[0] = Float.parseFloat(point.getMapX());
//                pos[1] = Float.parseFloat(point.getMapY());
//                pos[2] = Float.parseFloat(point.getTheta());
//                return pos;
//            }
//        }
//        return pos;
//    }
}
