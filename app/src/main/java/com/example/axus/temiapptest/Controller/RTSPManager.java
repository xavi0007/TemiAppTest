package com.example.axus.temiapptest.Controller;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;

import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class RTSPManager {

    private Context context;
    private MqttHelper mqttHelper;
    private Handler checkRTSPlooper = new Handler();


    public RTSPManager(Context context, MqttHelper mqttHelper){
        this.context = context;
        this.mqttHelper = mqttHelper;
    }



    Runnable waitRtspRun= new Runnable() { // this delay is to ensure RTSP is started before RFM is informed (no feedback?)
        @Override
        public void run() {
            // send robotmanager completed
            if (isMyServiceRunning(RtspServer.class)){
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

    //For RTSP
    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public Handler getCheckRTSPlooper() {
        return checkRTSPlooper;
    }
}
