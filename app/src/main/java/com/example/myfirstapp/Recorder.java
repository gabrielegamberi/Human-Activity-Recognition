package com.example.myfirstapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import com.github.mikephil.charting.charts.LineChart;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Vector;

public class Recorder extends HandlerThread
{
    private static int NUM_OF_EXPERIMENTS = 0;
    private Preprocessing preprocessing;
    private ContextWrapper context;
    private Handler handler;
    private LineChart staticAccelChart;
    private LineChart staticGyroChart;

    Recorder()
    {
        super("RecorderHandler");
    }


    public void init(ContextWrapper context)
    {
        this.context = context;
        View rootView = ((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);
        staticAccelChart = rootView.findViewById(R.id.staticAccelChart);
        staticGyroChart = rootView.findViewById(R.id.staticGyroChart);
        GuiUtilities.chartInit(context, staticAccelChart, 2f);
        GuiUtilities.chartInit(context, staticGyroChart, 2f);
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

                GuiUtilities.addEntry(context, preprocessing.getResult()[0], staticAccelChart);      //visualizzo la componente di accel (0) | gyro = 1
                GuiUtilities.addEntry(context, preprocessing.getResult()[2], staticGyroChart);      //visualizzo la componente di accel (0) | gyro = 1
            }
        };
    }


    public Handler getHandler() {
        return handler;
    }


    public int getNumberOfExperiments(){
        NUM_OF_EXPERIMENTS = 0;
       try(BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput("y_train.txt")))) {
           while (reader.readLine() != null) NUM_OF_EXPERIMENTS++;
        }catch (FileNotFoundException e){
            return 0;
        }catch (IOException e){
           e.printStackTrace();
       }
       return NUM_OF_EXPERIMENTS;
    }

    public void writeTrainingFiles(int associatedActivity)
    {
        String fileName;
        char [] axis = {'x','y','z'};
        Measurement dataToWrite = null;
        for(int i=0; i<9; i++){
            if(i<3) {
                fileName = "body_acc";
                dataToWrite = preprocessing.getBodyAccelData();
            }
            else if(i<6) {
                fileName = "body_gyro";
                dataToWrite = preprocessing.getGyroData();
            }
            else {
                fileName = "total_acc";
                dataToWrite = preprocessing.getTotalAccelData();

            }
            Vector<Double> axisToWrite;
            int actualAxis = i%3;

            if(actualAxis== 0)
                axisToWrite = dataToWrite.getX();
            else if(actualAxis== 1)
                axisToWrite = dataToWrite.getY();
            else
                axisToWrite = dataToWrite.getZ();

            try(Writer writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(
                    fileName+"_"+axis[actualAxis]+"_train.txt", context.MODE_PRIVATE | context.MODE_APPEND)))
            ){
                String samples = new String("");
                for(int j=0; j<dataToWrite.WINDOW_SIZE; j++)
                    samples = samples.concat(axisToWrite.get(j)+" ");
                writer.append(samples.trim()+"\n");
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        try(Writer writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput("y_train.txt", context.MODE_PRIVATE | context.MODE_APPEND)));
        ){
            writer.append(associatedActivity+"\n");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
