package com.example.axus.temiapptest.RobotInit;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.example.axus.temiapptest.Tasks.RobotTask;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import detection.MediaFileHandler;

public class App extends Application {
    private static final String TAG = "AudioConfig";
    private static volatile App app;

    private static String robotStateDetails;
    private int[] displayPosition;
    public File playFile;

    // mediafilehandler for SFTP audio broadcast
    public MediaFileHandler mMediaFileHandler;

    //Constructor for the class
    private App() {}


    public void robotTaskExecuteAlgo(final RobotTask robotTask, final JSONObject reader) throws JSONException {

        switch (robotTask.getTaskType()) {
            case "GOTO":
                break;
            case "LOCALIZE":
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

                break;
            case "GOTO_CHARGING":
                if (robotTask.getModificationType().equals("CANCEL")) {
                    cancelTask = true;
                    NavigationApi.get().stopNavigationService();
//                    robotWorkStatus = 0;
                    taskFromRobot = false;

                } else {
                    goTocharge = true;
                    goToDockStation();
                    mqttHelper.publishTaskStatus("GOTO_CHARGING", "EXECUTING", "");
                }
            case "SURVEILLANCE_MODE":
                // need to set robot working status to not busy!
                if (robotTask.getModificationType().equals("CREATE"))
                {
                    mqttHelper.publishTaskStatus("SURVEILLANCE_MODE", "EXECUTING", "");
                    cancelTask = false;
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



    /* Get Set functions*/


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
