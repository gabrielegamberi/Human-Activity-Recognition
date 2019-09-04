package com.example.myfirstapp;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Message;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;

public class DataAcquisitionActivity extends AppCompatActivity implements SensorEventListener
{
    //MEDIA PLAYER
    private MediaPlayer player = new MediaPlayer();
    //RECORDER
    private Recorder recorder;
    //MEASUREMENTS
    private Measurement accelMeasurements;
    private Measurement gyroMeasurements;
    //ANDROID
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private SensorManager sensorManager;
    //CHART
    private LineChart dynamicAccelChart;
    private LineChart dynamicGyroChart;
    //BUTTON
    private ImageView recordButton;
    private Button saveButton;
    private Button discardButton;
    //TEXT VIEW
    private TextView countdownText;
    //IMAGE VIEW
    private LinearLayout[] activities;
    //DIALOG
    private Dialog activityDialog;
    //SELECTED ACTIVITY
    private Integer selectedActivity;
    //RECORD FLAG
    private boolean readyToRecord = false;
    //NAVIGATION TOGGLE
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private boolean isSoundEnabled;
    //PHONE RELATED STUFF
    private static final int RESULT_PICK_CONTACT =1;
    private MenuItem contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Default init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_recognition);
        GuiUtilities.hideSystemUI(this);

        //Force Activity to be "Portrait"
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Recorder
        recorder = new Recorder();
        recorder.init(this);
        recorder.start();

        //Creating Sensor Manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Retrieving Accelerometer and Gyroscope
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //Register Sensors' listeners
        sensorManager.registerListener(this, accelSensor, 20000);
        sensorManager.registerListener(this, gyroSensor, 20000);

        //Initializing Measurements (all set to 0)
        accelMeasurements = new Measurement(false);
        gyroMeasurements = new Measurement(false);

        //Initializing Dynamic/Live Charts
        dynamicAccelChart = (LineChart) findViewById(R.id.dynamicAccelChart);
        dynamicGyroChart = (LineChart) findViewById(R.id.dynamicGyroChart);
        GuiUtilities.chartInit(this, dynamicAccelChart, 15f);
        GuiUtilities.chartInit(this, dynamicGyroChart, 15f);

        //Initializing Buttons
        recordButton = findViewById(R.id.recordButton);
        saveButton = findViewById(R.id.saveButton);
        discardButton = findViewById(R.id.discardButton);

        //Initializing Dialog
        activityDialog = new Dialog(this);

        //Initializing ImageViews (8 activities array)
        activities = new LinearLayout[8];

        //Initializing Countdown Text
        countdownText = findViewById(R.id.countdownText);

        //Initializing Countdown Sound
        AssetFileDescriptor beep = null;
        try {
            beep = getAssets().openFd("beep.mp3");
            player.setDataSource(beep.getFileDescriptor(),beep.getStartOffset(),beep.getLength());;
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Setting Listener to Record Button
        recordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                recordButton.setVisibility(View.INVISIBLE);
                saveButton.setVisibility(View.INVISIBLE);
                discardButton.setVisibility(View.INVISIBLE);
                countdownText.setText(" ");
                countdownText.setVisibility(View.VISIBLE);
                new CountDownTimer(5000, 1000) {
                    int counter = 5;
                    public void onTick(long millisUntilFinished) {
                        countdownText.setText(Integer.toString(counter));
                        if(counter--<=3 && isSoundEnabled)
                            player.start();
                    }

                    public void onFinish() {
                        countdownText.setText("recording..");
                        if(isSoundEnabled)
                            player.start();
                        readyToRecord = true;
                    }
                }.start();
            }
        });
        //Get the drawer and enable it
        drawerLayout = findViewById(R.id.drawer);
        toggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Highlight the menu item associated to the current view
        NavigationView navigationView = this.findViewById(R.id.navigation_view);
        navigationView.setCheckedItem(R.id.har_acquisition);

        //Enable Sound
        isSoundEnabled = true;

        //Load Phone Number
        PhoneUtilities.loadPhoneNo(this);
    }


    //Select Contact
    public void selectContact(MenuItem item) {
        Intent in = new Intent (Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult (in, RESULT_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        if(resultCode==RESULT_OK)
        {
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    contactPicked(data);
                    break;
            }
        }
        else
        {
            Toast.makeText (this, "Failed To pick contact", Toast.LENGTH_SHORT).show ();
        }
    }

    //Method called when a contact has been picked
    private void contactPicked(Intent data) {
        Cursor cursor;
        try {
            String phoneNo;
            Uri uri = data.getData ();
            cursor = getContentResolver ().query (uri, null, null,null,null);
            cursor.moveToFirst();
            int phoneIndex = cursor.getColumnIndex (ContactsContract.CommonDataKinds.Phone.NUMBER);

            phoneNo = cursor.getString (phoneIndex);
            PhoneUtilities.savePhoneNo(this, phoneNo);
            PhoneUtilities.loadPhoneNo(this);

        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    public void changeActivity(MenuItem item) {
        if(item.getItemId() == R.id.har_recognition){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    //Activity Selector: it lights up the activity you've selected and darkening the others
    public void activitySelector(View view){
        selectedActivity = Integer.parseInt(view.getTag().toString());
        for(int id=0; id<activities.length; id++){
            if(id+1 != selectedActivity)
                activities[id].setAlpha(0.3f);
            else
                activities[id].setAlpha(1f);
        }
    }

    //Show a Toast which informs you that the current recorded activity has been discarded
    public void discardActivity(View view){
        String message = new String("Activity discarded!");
        Toast.makeText(DataAcquisitionActivity.this, message,
                Toast.LENGTH_LONG).show();
        restoreButtons();
    }

    //Set the buttons' visibility to the initial configuration
    public void restoreButtons()
    {
        saveButton.setVisibility(View.INVISIBLE);
        discardButton.setVisibility(View.INVISIBLE);
        recordButton.setVisibility(View.VISIBLE);
        LineChart lineChart = findViewById(R.id.staticAccelChart);
        lineChart.clearValues();
        lineChart = findViewById(R.id.staticGyroChart);
        lineChart.clearValues();
    }

    //Method that opens a dialog to let the user select an activity to save
    public void openDialog(View view)
    {
        selectedActivity = new Integer(0);

        activityDialog.setContentView(R.layout.custompopup);

        activities[0] = activityDialog.findViewById(R.id.imageResting);
        activities[1] = activityDialog.findViewById(R.id.imageStanding);
        activities[2] = activityDialog.findViewById(R.id.imageWalking);
        activities[3] = activityDialog.findViewById(R.id.imageFalling);
        activities[4] = activityDialog.findViewById(R.id.imageWalkingUp);
        activities[5] = activityDialog.findViewById(R.id.imageWalkingDown);
        activities[6] = activityDialog.findViewById(R.id.imageStandingUp);
        activities[7] = activityDialog.findViewById(R.id.imageSitting);

        Button confirmButton = activityDialog.findViewById(R.id.confirmButton);
        Button cancelButton = activityDialog.findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityDialog.dismiss();
                GuiUtilities.hideSystemUI(DataAcquisitionActivity.this);
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //If you're saving an activity (whose range is [1,6])
                if(selectedActivity !=0)
                {
                    //Write the recorded data to files
                    recorder.writeTrainingFiles(selectedActivity);

                    //Remove the dialog and show your DataAcquisitionActivity properly
                    activityDialog.dismiss();
                    GuiUtilities.hideSystemUI(DataAcquisitionActivity.this);
                    restoreButtons();

                    //Inform the user that the new activity has been saved
                    String message = new String("Activity #"+recorder.getNumberOfExperiments()+" saved!");
                    Toast.makeText(DataAcquisitionActivity.this, message,
                            Toast.LENGTH_LONG).show();
                }
                //If you forgot to select an activity ( = 0)
                else
                {
                    //Inform the user that has to select an activity before saving it!
                    String message = new String("Select an activity!");
                    Toast.makeText(DataAcquisitionActivity.this, message,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        //If you click outside the Dialog, nothing happens
        activityDialog.setCanceledOnTouchOutside(false);

        //Show the Dialog
        activityDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if (sensorEvent.sensor == gyroSensor) {
            GuiUtilities.addEntry(this, sensorEvent, dynamicGyroChart);
            if(readyToRecord)
                gyroMeasurements.insert(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        } else if (sensorEvent.sensor == accelSensor) {
            GuiUtilities.addEntry(this, sensorEvent, dynamicAccelChart);
            if(readyToRecord)
                accelMeasurements.insert(sensorEvent.values[0] / 9.80665, sensorEvent.values[1] / 9.80665, sensorEvent.values[2] / 9.80665); //We want to save the data in g units
        }

        if (accelMeasurements.isFull() && gyroMeasurements.isFull()) {
            readyToRecord = false;
            performRecording();

            //Clearing data
            accelMeasurements.clear();
            gyroMeasurements.clear();

            //Showing Save & Discard buttons
            saveButton.setVisibility(View.VISIBLE);
            discardButton.setVisibility(View.VISIBLE);
            countdownText.setVisibility(View.INVISIBLE);

            //When recording is over, warn me with a sound
            if(isSoundEnabled)
                player.start();
        }

    }

    //Start recording process
    public void performRecording()
    {
        Message msg = Message.obtain();
        try {
            Measurement[] packedData = {(Measurement)accelMeasurements.clone(),(Measurement)gyroMeasurements.clone()};
            msg.obj = packedData;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        recorder.getHandler().sendMessage(msg);   //Send a message (it contains the measurements)
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        sensorManager.unregisterListener(DataAcquisitionActivity.this);
        recorder.getHandler().removeCallbacksAndMessages(null);
        recorder.quitSafely();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GuiUtilities.hideSystemUI(this);
        sensorManager.registerListener(this, accelSensor, 20000);
        sensorManager.registerListener(this, gyroSensor, 20000);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    //Enable/Disable Sound
    public void toggleSound(View view) {
        CompoundButton switchButton = (CompoundButton)view;
        isSoundEnabled = switchButton.isChecked();
    }
}
