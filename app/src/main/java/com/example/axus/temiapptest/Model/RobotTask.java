package com.example.axus.temiapptest.Model;

import org.json.JSONObject;
/**
 * This is the object to store robottask. Only 1 executing robottask at any time.
 */
public class RobotTask {
    private String modificationType;
    private String taskType;
    private String mapVerId;
    private String positionName;
    private boolean abort;
    private boolean requestRfm;
    private String priority;
    private double x;
    private double y;
    private int heading;
    private String tts;
    private int totalTaskNo;
    private int currentTaskNo;
    private JSONObject parameters;

    public RobotTask() {

    }

    public RobotTask(String modificationType, String taskType, String mapVerId, String positionName,
                     double x, double y, int heading, String tts) {
        this.modificationType = modificationType;
        this.taskType = taskType;
        this.mapVerId = mapVerId;
        this.positionName = positionName;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.tts = tts;
    }

    public String getModificationType() {
        return modificationType;
    }

    public void setModificationType(String modificationType) {
        this.modificationType = modificationType;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getMapVerId() {
        return mapVerId;
    }

    public void setMapVerId(String mapVerId) {
        this.mapVerId = mapVerId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getHeading() {
        return heading;
    }

    public void setHeading(int heading) {
        this.heading = heading;
    }

    public String getTts() {
        return tts;
    }

    public void setTts(String tts) {
        this.tts = tts;
    }

    public boolean isAbort() {
        return abort;
    }

    public void setAbort(boolean abort) {
        this.abort = abort;
    }

    public boolean isRequestRfm() {
        return requestRfm;
    }

    public void setRequestRfm(boolean requestRfm) {
        this.requestRfm = requestRfm;
    }

    public String getPriority() {
        return priority;
    }

    public int getTotalTaskNo() {
        return totalTaskNo;
    }

    public void setTotalTaskNo(int totalTaskNo) {
        this.totalTaskNo = totalTaskNo;
    }

    public int getCurrentTaskNo() {
        return currentTaskNo;
    }

    public void setCurrentTaskNo(int currentTaskNo) {
        this.currentTaskNo = currentTaskNo;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public JSONObject getParameters() {
        return parameters;
    }

    public void setParameters(JSONObject parameters) {
        this.parameters = parameters;
    }
}

