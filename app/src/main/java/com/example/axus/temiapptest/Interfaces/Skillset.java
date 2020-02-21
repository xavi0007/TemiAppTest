package com.example.axus.temiapptest.Interfaces;
import com.example.axus.temiapptest.Model.RobotTask;

import org.json.JSONException;

public interface Skillset {

    public void gotoBroadcastSkill();

    void gotoSkill(RobotTask robotTask) throws JSONException;

    public void liftOperationMapSwitch(int status, String message);
    public void liftOperationLocalise(int status, String message);
    public void cancelNavigation();

}
