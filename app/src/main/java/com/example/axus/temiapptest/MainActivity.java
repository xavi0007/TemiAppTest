package com.example.axus.temiapptest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.axus.temiapptest.Camera.CameraActivity;
import com.example.axus.temiapptest.UIAdapter.CustomAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Robot.NlpListener, OnRobotReadyListener, Robot.ConversationViewAttachesListener, Robot.WakeupWordListener, Robot.ActivityStreamPublishListener, Robot.TtsListener, OnBeWithMeStatusChangedListener, OnGoToLocationStatusChangedListener, OnLocationsUpdatedListener, OnDetectionStateChangedListener, Robot.AsrListener, OnConstraintBeWithStatusChangedListener,AdapterView.OnItemSelectedListener {


    //variable declaration
    private Robot robot;
    List<String> locations;
    private static volatile MainActivity mainActivity;

    //LinearLayout rLL = (LinearLayout) findViewById(R.id.main_layout);
    //UI
    public EditText saveLocationInput;
    public Button btnSaveLocation;
    public Button btnSavedLocations;
    public Button ncsBtn;
    public Button goTo;
    public FloatingActionButton cameraView;
    public Spinner locationSpinner;
    public ImageView eyes;
    //permision variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CAMERA = 100;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        robot = Robot.getInstance(); // get an instance of the robot in order to begin using its features.
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getSupportActionBar().hide(); // hide the title bar
        init();
        mainActivity = this;
    }

    public Context getContext() {
        return context;
    }

    public void init() {
        verifyStoragePermissions(this);
        checkCameraHardware(this);
        //init all UI items
        saveLocationInput = findViewById(R.id.saveLocationInput);
        locationSpinner = findViewById(R.id.locationSpinner);
        btnSaveLocation = (Button) findViewById(R.id.btnSaveLocation);
        btnSavedLocations = (Button) findViewById(R.id.btnSavedLocations);
        ncsBtn = (Button) findViewById(R.id.ncsbtn);
        goTo = (Button) findViewById(R.id.btnGoTo);
        cameraView = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        eyes = (pl.droidsonroids.gif.GifImageView) findViewById(R.id.eyes);

        //set visibility
        eyes.setVisibility(View.VISIBLE);
        saveLocationInput.setVisibility(View.GONE);
        btnSaveLocation.setVisibility(View.GONE);
        btnSavedLocations.setVisibility(View.GONE);
        locationSpinner.setVisibility(View.GONE);
        goTo.setVisibility(View.GONE);
        cameraView.setVisibility(View.GONE);
        setLocationSpinner();
    }

    //hardware permissions
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camera", "Asking for camera permission");
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            Log.d("Camera", "Camera exist");
            return true;
        } else {
            Log.d("Camera", "No camera");
            return false;
        }

    }

    protected void onStart() {
        super.onStart();
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

    protected void onStop() {
        super.onStop();
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

    //populate the spinner
    public void setLocationSpinner(){
        locations = robot.getLocations();
        if(!locations.isEmpty()){
            final ArrayAdapter spinnerAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,locations);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //Setting the ArrayAdapter data on the Spinner
            locationSpinner.setAdapter(spinnerAdapter);
        }
        else{
            Log.i("Saved_Location", "Empty");
        }
    }

    public void saveLocation(View view) {
        String location = saveLocationInput.getText().toString().toLowerCase().trim();
        boolean result = robot.saveLocation(location);
        if (result) {
            robot.speak(TtsRequest.create("I've successfully saved the " + location + " location.", true));
        } else {
            robot.speak(TtsRequest.create("Saved the " + location + " location failed.", true));
        }
        hideKeyboard(MainActivity.this);
        setLocationSpinner();
    }

    /**
     * goTo checks that the location sent is saved then goes to that location.
     */
    public void goTo(View view) {
        for (String location : robot.getLocations()) {
            if(locationSpinner.getSelectedItem().toString().isEmpty()){
                setLocationSpinner();
            }
            else if (location.equals(locationSpinner.getSelectedItem().toString().toLowerCase().trim())) {
                robot.goTo(locationSpinner.getSelectedItem().toString().toLowerCase().trim());
                hideKeyboard(MainActivity.this);
            }
        }
    }




    public void goToDestination(String destination){
        if(robot == null){
            robot =  Robot.getInstance();
        }
        for(String location : robot.getLocations()){
            if(location.equals(destination.toLowerCase().trim())){
                robot.goTo(destination.toLowerCase().trim());
                speak("I am going to" + destination.toLowerCase().trim() + "now");
                if(!destination.toLowerCase().trim().equals("homebase")){
                    speak("Please follow me");
                }
            }
            else{
                Log.d("goToDestionation","fail to find location");
            }
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void adminMode(View view){
        saveLocationInput.setVisibility(View.VISIBLE);
        btnSaveLocation.setVisibility(View.VISIBLE);
        btnSavedLocations.setVisibility(View.VISIBLE);
        locationSpinner.setVisibility(View.VISIBLE);
        goTo.setVisibility(View.VISIBLE);
        cameraView.setVisibility(View.VISIBLE);
        eyes.setVisibility(View.GONE);
        robot.showTopBar();
    }

    public void startSurveillance(View view){
        if(checkCameraHardware(this)){
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }

//    /**
//     * Have the robot speak while displaying what is being said.
//     */
//    public void speak(View view) {
//        TtsRequest ttsRequest = TtsRequest.create(etSpeak.getText().toString().trim(), true);
//        robot.speak(ttsRequest);
//        hideKeyboard(MainActivity.this);
//    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        goTo(view);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onPublish(@NotNull ActivityStreamPublishMessage activityStreamPublishMessage) {

    }

    @Override
    public void onConversationAttaches(boolean b) {

    }

    @Override
    public void onNlpCompleted(@NotNull NlpResult nlpResult) {
        //do something with nlp result. Base the action specified in the AndroidManifest.xml
        Toast.makeText(MainActivity.this, nlpResult.action, Toast.LENGTH_SHORT).show();

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

    public void speak(String text) {
        TtsRequest ttsRequest = TtsRequest.create(text.trim(), true);
        robot.speak(ttsRequest);
    }

    public BatteryData getBatteryData() {
        BatteryData batteryData = robot.getBatteryData();
        return batteryData;
    }


    @Override
    public void onTtsStatusChanged(@NotNull TtsRequest ttsRequest) {

    }

    @Override
    public void onWakeupWord(@NotNull String s, int i) {

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
        Log.d("GoToStatusChanged", "descriptionId=" + descriptionId + ", description=" + description);
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
                robot.speak(TtsRequest.create("Completed", false));
                robot.goTo("home base");
                robot.speak(TtsRequest.create("Going back to home base", false));
                break;

            case "abort":
                robot.speak(TtsRequest.create("Cancelled", false));
                robot.goTo("home base");
                robot.speak(TtsRequest.create("Going back to home base", false));
                break;
        }
    }

    @Override
    public void onLocationsUpdated(@NotNull List<String> list) {

    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
                setLocationSpinner();
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onAsrResult(@NotNull String s) {

    }

    @Override
    public void onDetectionStateChanged(int i) {

    }

    @Override
    public void onConstraintBeWithStatusChanged(boolean b) {

    }

    public void savedLocationsDialog(View view) {
        hideKeyboard(MainActivity.this);
        locations = robot.getLocations();
        final CustomAdapter customAdapter = new CustomAdapter(MainActivity.this, android.R.layout.simple_selectable_list_item, locations);
        AlertDialog.Builder versionsDialog = new AlertDialog.Builder(MainActivity.this);
        versionsDialog.setTitle("Saved Locations: (Click to delete the location)");
        versionsDialog.setPositiveButton("OK", null);
        versionsDialog.setAdapter(customAdapter, null);
        AlertDialog dialog = versionsDialog.create();
        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Delete location \"" + customAdapter.getItem(position) + "\" ?");
                builder.setPositiveButton("No thanks", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String location = customAdapter.getItem(position);
                        if (location == null) {
                            return;
                        }
                        boolean result = robot.deleteLocation(location);
                        if (result) {
                            locations.remove(position);
                            robot.speak(TtsRequest.create(location + "delete successfully!", false));
                            customAdapter.notifyDataSetChanged();
                        } else {
                            robot.speak(TtsRequest.create(location + "delete failed!", false));
                        }
                    }
                });
                Dialog deleteDialog = builder.create();
                deleteDialog.show();
            }
        });
        dialog.show();
    }



    /**
     * publishToActivityStream takes an image stored in the resources folder
     * and uploads it to the mobile application under the Activities tab.
     */
    public void publishToActivityStream(View view){
        ActivityStreamObject activityStreamObject;
        if (robot != null) {
            final String fileName = "puppy.png";
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.puppy);
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

    public void stopMovement(){
        robot.stopMovement();
    }

    public Robot getRobot() {
        return robot;
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }

    public static MainActivity getInstance() {
        if (mainActivity == null) {
            synchronized (MainActivity.class) {
                if (mainActivity == null) {
                    mainActivity = new MainActivity();
                }
            }
        }
        return mainActivity;
    }

}
