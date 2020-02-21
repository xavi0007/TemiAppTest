package com.example.axus.temiapptest.ViewModel;

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
import android.os.Bundle;
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

import com.example.axus.temiapptest.Camera.CameraActivity;
import com.example.axus.temiapptest.Controller.RobotSkillSet;
import com.example.axus.temiapptest.Controller.TemiRobot;
import com.example.axus.temiapptest.R;
import com.example.axus.temiapptest.UIAdapter.CustomAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

import java.util.List;

public class MainActivity extends AppCompatActivity  {


    //variable declaration
    List<String> locations;
    private static volatile MainActivity mainActivity;

    //TemiRobot class
    TemiRobot tm;
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
        setContentView(R.layout.activity_main);
        tm = new TemiRobot(this.getApplicationContext());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getSupportActionBar().hide(); // hide the title bar
        mainActivity = this;
        initUI();
    }

    public Context getContext() {
        return context;
    }

    public void initUI() {
        verifyStoragePermissions(this);
        checkCameraHardware(this);
        //initUI all UI items
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

    }

    protected void onStop() {
        super.onStop();
        tm.onDestroy();
    }

    //populate the spinner
    public void setLocationSpinner(){
        locations = tm.getLocations();
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
        boolean result = tm.saveLocation(location);
        if (result) {
            tm.speak("I've successfully saved the " + location + " location.", true);
        } else {
            tm.speak("Saved the " + location + " location failed.", true);
        }
        hideKeyboard(MainActivity.this);
        setLocationSpinner();
    }

    /**
     * goTo checks that the location sent is saved then goes to that location.
     */
    public void goTo(View view) {
        for (String location : tm.getLocations()) {
            if(locationSpinner.getSelectedItem().toString().isEmpty()){
                setLocationSpinner();
            }
            else if (location.equals(locationSpinner.getSelectedItem().toString().toLowerCase().trim())) {
                tm.goToDestination(locationSpinner.getSelectedItem().toString().toLowerCase().trim());
                hideKeyboard(MainActivity.this);
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
        tm.showTopBar(this.getCurrentFocus());
    }

    public void startSurveillance(View view){
        if(checkCameraHardware(this)){
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }


    public void savedLocationsDialog(View view) {
        hideKeyboard(MainActivity.this);
        locations = tm.getLocations();
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
                        boolean result = tm.deleteLocation(location);
                        if (result) {
                            locations.remove(position);
                            tm.speak((location + "delete successfully!"),true);
                            customAdapter.notifyDataSetChanged();
                        } else {
                            tm.speak((location + "delete failed!"),true);
                        }
                    }
                });
                Dialog deleteDialog = builder.create();
                deleteDialog.show();
            }
        });
        dialog.show();
    }



    public ActivityInfo getActivityInfos() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            return ai;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public TemiRobot getTemi(){
        return this.tm;
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
