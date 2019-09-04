package com.example.myfirstapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.varunest.sparkbutton.SparkButton;
import com.varunest.sparkbutton.SparkEventListener;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    //PREDICTION TEXT & ICON
    public TextView predictionText;
    private ImageView predictionIcon;
    //ZOOM IN ANIMATION
    private Animation animZoomIn;
    //CENTRAL SPARK BUTTON
    private SparkButton sparkButton;
    //STOP BUTTON
    private Button stopButton;
    //MEDIA PLAYERS
    private MediaPlayer player[] = new MediaPlayer[8];
    //CLASSIFIER
    private Classifier classifier;
    //CLASSIFIER FLAG
    private boolean readyToAcquireData;
    //MEASUREMENTS
    private Measurement accelMeasurements;
    private Measurement gyroMeasurements;
    //ANDROID
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private SensorManager sensorManager;
    //PREVIOUS PREDICTIONS
    private ImageView[] previousPredictions;
    private int recognizedActivities;
    //NAVIGATION TOGGLE
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    //SOUND SWITCH
    private boolean isSoundEnabled;
    //PHONE RELATED STUFF
    private MenuItem contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Default init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Force Activity to be "Portrait"
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Hide System UI
        GuiUtilities.hideSystemUI(this);

        //Init Predicion Label
        predictionText = this.findViewById(R.id.predictionText);

        //Init zoomIn animation
        animZoomIn = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.zoom_in);

        //Init Stop Button
        stopButton = this.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //Restore start button's features
                sparkButton.startAnimation(animZoomIn);
                sparkButton.setVisibility(View.VISIBLE);
                sparkButton.setEnabled(true);
                sparkButton.startAnimation(animZoomIn);

                //Reinitialize recognition graphical fields
                predictionIcon.setVisibility(View.INVISIBLE);
                predictionText.setText("BEGIN\nRECOGNITION");

                //Stop data acquisition
                readyToAcquireData = false;

                //Hide stop button
                stopButton.setVisibility(View.INVISIBLE);

                //Set previous activities to invisible
                previousPredictions[previousPredictions.length-1].clearAnimation();
                for(int i=0; i<previousPredictions.length; i++){
                    previousPredictions[i].setVisibility(View.INVISIBLE);
                }

                //Reset recognized activities
                recognizedActivities = 0;

                //Clear the measurements
                accelMeasurements.clear();
                gyroMeasurements.clear();
            }
        });

        //Init Spark Button
        sparkButton = this.findViewById(R.id.sparkButton);
        sparkButton.startAnimation(animZoomIn);


        //sparkButton.startAnimation(aniFade);
        sparkButton.setEventListener(new SparkEventListener(){
            @Override
            public void onEvent(ImageView button, boolean buttonState) {
                if (buttonState) {
                } else {
                }
            }

            @Override
            public void onEventAnimationEnd(ImageView button, boolean buttonState) {
            }

            @Override
            public void onEventAnimationStart(ImageView button, boolean buttonState) {
                //Make button unclickable
                sparkButton.setEnabled(false);
                //Start data acquisition
                readyToAcquireData = true;
                //Start the loading animation
                sparkButton.clearAnimation();
                RotateAnimation rotate = new RotateAnimation(0,1800,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setDuration(6000);
                rotate.setRepeatCount(Animation.INFINITE);
                sparkButton.setAnimation(rotate);
            }
        });

        //Disable data acquisition till the start button is clicked
        readyToAcquireData = false;

        //Init Prediction Icon
        predictionIcon = (ImageView)findViewById(R.id.predictionIcon);

        //Init media players
        for(int i=0; i<player.length; i++)
        {
            player[i] = new MediaPlayer();
            //Load activity sounds
            AssetFileDescriptor sound = null;
            try {
                sound = getAssets().openFd((i+1) + ".mp3");
                player[i].setDataSource(sound.getFileDescriptor(),sound.getStartOffset(),sound.getLength());;
                player[i].prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //Init & Launch Classifier
        classifier = new Classifier(new UIHandler());
        classifier.init(this);
        classifier.start();

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

        //Init previous prediction
        previousPredictions = new ImageView[3];
        for(int i=0; i<previousPredictions.length; i++){
            previousPredictions[i] = this.findViewById(R.id.prevAct1 +i);
        }
        recognizedActivities = 0;

        //Get the drawer and enable it
        drawerLayout = findViewById(R.id.drawer);
        toggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Highlight the menu item associated to the current view
        NavigationView navigationView = this.findViewById(R.id.navigation_view);
        navigationView.setCheckedItem(R.id.har_recognition);

        //Enable Sound
        isSoundEnabled = true;

        //Load Phone Number
        PhoneUtilities.loadPhoneNo(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        if(resultCode==RESULT_OK)
        {
            switch (requestCode) {
                case PhoneUtilities.RESULT_PICK_CONTACT:
                    contactPicked(data);
                    break;
            }
        }
        else
        {
            Toast.makeText (this, "Failed To pick contact", Toast.LENGTH_SHORT).show ();
        }
    }


    private void contactPicked(Intent data) {
        Cursor cursor;
        try {
            String phoneNo;
            Uri uri = data.getData ();
            cursor = getContentResolver ().query (uri, null, null,null,null);
            cursor.moveToFirst ();
            int phoneIndex = cursor.getColumnIndex (ContactsContract.CommonDataKinds.Phone.NUMBER);

            phoneNo = cursor.getString (phoneIndex);

            PhoneUtilities.savePhoneNo(this, phoneNo);
            PhoneUtilities.loadPhoneNo(this);

        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    //Enable/Disable Sound
    public void toggleSound(View view) {
        CompoundButton switchButton = (CompoundButton)view;
        isSoundEnabled = switchButton.isChecked();
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
        if(readyToAcquireData){
            if (sensorEvent.sensor == gyroSensor) {
                gyroMeasurements.insert(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            } else if (sensorEvent.sensor == accelSensor) {
                accelMeasurements.insert(sensorEvent.values[0] / 9.80665, sensorEvent.values[1] / 9.80665, sensorEvent.values[2] / 9.80665); //We want to save the data in g units
            }

            if (accelMeasurements.isFull() && gyroMeasurements.isFull()) {
                performClassification();
                accelMeasurements.rearrangeHalfBack();
                gyroMeasurements.rearrangeHalfBack();
            }
        }
    }

    //Classification & Showing its label
    public void performClassification()
    {
        Message msg = Message.obtain();
        try {
            Measurement[] packedData = {(Measurement)accelMeasurements.clone(),(Measurement)gyroMeasurements.clone()};
            msg.obj = packedData;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        classifier.getHandler().sendMessage(msg);   //Send the measurements to the Classifier (as a message)
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        sensorManager.unregisterListener(MainActivity.this);
        classifier.getHandler().removeCallbacksAndMessages(null);
        classifier.quitSafely();
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

    //Enable a flag to start the recognition process
    public void beginRecognition(View view) {
        readyToAcquireData = true;
    }

    //Switch Activity
    public void changeActivity(MenuItem item) {
        if(item.getItemId() == R.id.har_acquisition){
            Intent intent = new Intent(this, DataAcquisitionActivity.class);
            startActivity(intent);
        }
    }

    //Select Contact
    public void selectContact(MenuItem item) {
        Intent in = new Intent (Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult (in, PhoneUtilities.RESULT_PICK_CONTACT);
    }


    //Handler that handles the messages coming from the Classifier
    private class UIHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle bundle = msg.getData();

            //Acquire data labelled as "refresh" (classification result)
            if(bundle.containsKey("refresh")){
                String[] currentPrediction = bundle.getString("refresh").split("_");
                String lastPrediction = predictionText.getText().toString();

                //If consecutive predictions are equal, don't do anything
                if(!lastPrediction.equals(currentPrediction[0]))
                {
                    predictionText.setText(currentPrediction[0]);
                    try
                    {
                        if(recognizedActivities>0) {
                            int maximumVisibleActivities = 4;
                            //Set Previous Predictions Increasingly Visible
                            int predictionSlot = previousPredictions.length - recognizedActivities;
                            if (recognizedActivities != maximumVisibleActivities) {
                                previousPredictions[predictionSlot].setVisibility(View.VISIBLE);
                                recognizedActivities++;
                            }
                            int lastSlot = previousPredictions.length - 1;
                            if (recognizedActivities > 2){
                                for (int i = maximumVisibleActivities - recognizedActivities; i < lastSlot; i++) {
                                    previousPredictions[i].setImageDrawable(previousPredictions[i + 1].getDrawable());
                                }
                            }

                            Drawable activityToSave = predictionIcon.getDrawable();
                            previousPredictions[lastSlot].setVisibility(View.VISIBLE);
                            previousPredictions[lastSlot].setImageDrawable(activityToSave);
                            previousPredictions[lastSlot].startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.translation_y));
                        }

                        //Load current predicted icon
                        InputStream ims = getAssets().open(currentPrediction[0] + ".png");
                        Drawable d = Drawable.createFromStream(ims, null);
                        predictionIcon.setImageDrawable(d);
                        ims.close();

                        if(isSoundEnabled){
                            //Get activity sound and play it
                            int selectedSound = Integer.parseInt(currentPrediction[1]);
                            player[selectedSound].start();
                        }

                    }
                    catch(IOException ex) {return;}
                }
                sparkButton.clearAnimation();
                sparkButton.setVisibility(View.INVISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                predictionIcon.setVisibility(View.VISIBLE);

                //If you have recognized the first activity, then set "recognizedActivities" to enable the bottom labels
                if(recognizedActivities == 0)
                    recognizedActivities = 1;
            }
        }
    }

}
