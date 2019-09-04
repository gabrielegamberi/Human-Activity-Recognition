package com.example.myfirstapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContextWrapper;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

public class Classifier extends HandlerThread
{
    private String []classes = {"RESTING", "STANDING", "WALKING", "FALLING", "WALKING UPSTAIRS", "WALKING DOWNSTAIRS", "STANDING UP", "SITTING"};
    private Interpreter tflite;
    private Preprocessing preprocessing;
    private ContextWrapper context;
    private static final String MODEL_PATH = "model.tflite";
    private Handler handler;
    private Handler mainHandler;

    Classifier(Handler mainHandler)
    {
        super("ClassifierHandler");
        this.mainHandler = mainHandler;
    }


    public void init(ContextWrapper context)
    {
        this.context = context;

        //Carico il modello
        try {
            this.tflite = new Interpreter(loadModelFile((Activity)context));
        } catch (IOException e) {
            e.printStackTrace();
        }

        preprocessing = new Preprocessing(context);
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Measurement[] dataPacket = (Measurement[])msg.obj;
                preprocessing.execute(dataPacket[0], dataPacket[1]);

                String prediction = getPrediction();
                /*
                System.out.println("\t----> PREDICTED: "+prediction+" ----");
                 */
                //Reply back to the UI thread
                Bundle bundle = new Bundle();
                bundle.putString("refresh",prediction);
                Message message = mainHandler.obtainMessage();
                message.setData(bundle);
                mainHandler.sendMessage(message);
            }
        };
    }

    private String getPrediction(){
        float[][] classValues = new float[1][classes.length];
        tflite.run(preprocessing.getOutputMatrix(), classValues);

        int iMax = 0;
        float maxVal = Float.MIN_VALUE;
        for(int i=0; i<classValues[0].length; i++){
            System.out.print("\t"+classValues[0][i]);
            if(classValues[0][i] > maxVal){
                maxVal = classValues[0][i];
                iMax = i;
            }
        }
        return classes[iMax]+"_"+iMax;
    }


    public Handler getHandler() {
        return handler;
    }

    // Mappatura del file in memoria
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException
    {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


}
