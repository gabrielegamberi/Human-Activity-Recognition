package com.example.myfirstapp;
import android.content.ContextWrapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class Preprocessing
{

    private float [][] mean;
    private float [][] std;
    private float [][][] outputMatrix;
    private Measurement totalAccelData;
    private Measurement gyroData;
    private Measurement bodyAccelData;
    private Measurement gravityAccelData;
    private ContextWrapper context;

    Preprocessing(ContextWrapper contextWrapper){
        this.context = contextWrapper;
        fillStandardizationMatrixes();
        outputMatrix = new float[1][128][9];    //neural-network input format
    }

    //Retrieve Matrixes used for standardization
    private void fillStandardizationMatrixes(){
        mean = new float[128][9];
        std = new float[128][9];
        int i;
        try(InputStream meanInputStream = context.getAssets().open("meanMatrix");
            InputStream stdInputStream = context.getAssets().open("stdMatrix");
            BufferedReader meanBufferedReader = new BufferedReader(new InputStreamReader(meanInputStream));
            BufferedReader stdBufferedReader = new BufferedReader(new InputStreamReader(stdInputStream))
        ) {
            String line;
            String[] values;
            i = 0;
            while ((line = meanBufferedReader.readLine()) != null) {
                values = line.trim().split("\\s+");
                for(int j=0; j<values.length; j++)
                    mean[i][j] = Float.parseFloat(values[j]);
                i++;
            }
            i = 0;
            while ((line = stdBufferedReader.readLine()) != null) {
                values = line.trim().split("\\s+");
                for(int j=0; j<values.length; j++)
                    std[i][j] = Float.parseFloat(values[j]);
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Measurement getTotalAccelData() {
        return totalAccelData;
    }

    public Measurement getGyroData() {
        return gyroData;
    }

    public Measurement getBodyAccelData() {
        return bodyAccelData;
    }

    //Start pre-processing
    public void execute(Measurement rawAccelData, Measurement rawGyroData){
        //Clone Measurements
        try {
            this.totalAccelData = (Measurement)rawAccelData.clone();
            this.gyroData = (Measurement)rawGyroData.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        this.gravityAccelData = new Measurement(true);
        this.bodyAccelData = new Measurement(true);

        //Apply Filters
        medianFilter(this.totalAccelData);
        medianFilter(this.gyroData);
        butterworthFilter(this.totalAccelData, 20);
        butterworthFilter(this.gyroData, 20);

        //Separate body/gravity components (another filtering)
        separateBodyGravity();

        //Convert data to a valid format (matrix: 128x9)
        formatOutputMatrix();

        //Apply standardization
        standardize();
    }

    //Median filtering
    private void medianFilter(Measurement sensorData)
    {
        int window_size = 3;
        int pad_size = (int)Math.floor(window_size/2);

        Vector<Double> x = sensorData.getX();
        Vector<Double> y = sensorData.getY();
        Vector<Double> z = sensorData.getZ();

        Vector<Double> tempX = (Vector<Double>)x.clone();
        Vector<Double> tempY = (Vector<Double>)y.clone();
        Vector<Double> tempZ = (Vector<Double>)z.clone();

        List<Double> chunk;
        for(int i=pad_size; i<x.size()-pad_size; i++)
        {
            chunk = new Vector<>(x.subList(i-pad_size, i+pad_size));
            Collections.sort(chunk);
            tempX.set(i,chunk.get(pad_size));

            chunk = new Vector<>(y.subList(i-pad_size, i+pad_size));
            Collections.sort(chunk);
            tempY.set(i,chunk.get(pad_size));

            chunk = new Vector<>(z.subList(i-pad_size, i+pad_size));
            Collections.sort(chunk);
            tempZ.set(i,chunk.get(pad_size));
        }
        sensorData.setX((Vector<Double>) tempX.clone());
        sensorData.setY((Vector<Double>) tempY.clone());
        sensorData.setZ((Vector<Double>) tempZ.clone());

    }

    //ButterWorth filtering
    private void butterworthFilter(Measurement sensorData, double cut)
    {
        //Declare a low-pass butterworth filter, 3° order with a certain cut-off frequency and sampling frequency
        Butterworth butterworth = new Butterworth(3, toNormalizedFrequency(cut, 50), true);  //NORMALIZED CUT-OFF FREQUENCY (matlab notation): F_cut/(Fs/2)
        double[] tempX = convertToDouble(sensorData.getX());
        double[] tempY = convertToDouble(sensorData.getY());
        double[] tempZ = convertToDouble(sensorData.getZ());
        tempX = butterworth.filter(tempX);
        tempY = butterworth.filter(tempY);
        tempZ = butterworth.filter(tempZ);
        for(int i=0; i<totalAccelData.WINDOW_SIZE; i++)
        {
            sensorData.getX().set(i,new Double(tempX[i]));
            sensorData.getY().set(i,new Double(tempY[i]));
            sensorData.getZ().set(i,new Double(tempZ[i]));
        }
    }

    //Separate Body/Gravity components from total
    private void separateBodyGravity()
    {
        butterworthFilter(gravityAccelData,0.3);
        for(int i=0; i<totalAccelData.WINDOW_SIZE; i++)
        {
            bodyAccelData.getX().set(i,new Double(totalAccelData.getX().get(i) - gravityAccelData.getX().get(i)));
            bodyAccelData.getY().set(i,new Double(totalAccelData.getY().get(i) - gravityAccelData.getY().get(i)));
            bodyAccelData.getZ().set(i,new Double(totalAccelData.getZ().get(i) - gravityAccelData.getZ().get(i)));
        }
    }

    //Standardization method
    private void standardize(){
        for(int i=0; i<Measurement.WINDOW_SIZE; i++)
            for(int j=0; j<9; j++)
                outputMatrix[0][i][j] = (outputMatrix[0][i][j]-mean[i][j])/std[i][j];
    }

    public float[][][] getOutputMatrix(){
        return this.outputMatrix;
    }

    //Storing data locally
    private void writeToFile(String filename, Measurement data)
    {
        Writer writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(filename, context.MODE_PRIVATE)));
            for(int i=0; i<data.WINDOW_SIZE; i++)
            {
                String x = Double.toString( data.getX().get(i));
                String y = Double.toString( data.getY().get(i));
                String z = Double.toString( data.getZ().get(i));
                writer.write(x + " " + y + " " + z + "\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Measurement[] getResult()
    {
        Measurement[] data = {totalAccelData, bodyAccelData, gyroData};
        return data;
    }

    //Convert data to a valid format (matrix: 128x9)
    public void formatOutputMatrix(){
        int feature;

        for(feature=0; feature<3; feature++){

            for(int sample=0; sample<Measurement.WINDOW_SIZE; sample++){
                if(feature%3 == 0)
                    outputMatrix[0][sample][feature] = bodyAccelData.getX().get(sample).floatValue();
                else if(feature%3 == 1)
                    outputMatrix[0][sample][feature] = bodyAccelData.getY().get(sample).floatValue();
                else
                    outputMatrix[0][sample][feature] = bodyAccelData.getZ().get(sample).floatValue();
            }
        }

        for(;feature<6; feature++){
            for(int sample=0; sample<Measurement.WINDOW_SIZE; sample++){
                if(feature%3 == 0)
                    outputMatrix[0][sample][feature] = gyroData.getX().get(sample).floatValue();
                else if(feature%3 == 1)
                    outputMatrix[0][sample][feature] = gyroData.getY().get(sample).floatValue();
                else
                    outputMatrix[0][sample][feature] = gyroData.getZ().get(sample).floatValue();
            }
        }

        for(;feature<9; feature++){
            for(int sample=0; sample<Measurement.WINDOW_SIZE; sample++){
                if(feature%3 == 0)
                    outputMatrix[0][sample][feature] = totalAccelData.getX().get(sample).floatValue();
                else if(feature%3 == 1)
                    outputMatrix[0][sample][feature] = totalAccelData.getY().get(sample).floatValue();
                else
                    outputMatrix[0][sample][feature] = totalAccelData.getZ().get(sample).floatValue();
            }
        }
    }

    //Vector to Double array
    private double[] convertToDouble(Vector<Double> vector)
    {
        double[] temp = new double[128];
        for(int i=0;i<vector.size();i++)
        {
            temp[i] = vector.get(i);
        }
        return temp;
    }

    //Return normalized frequency (matlab notation)
    private static double toNormalizedFrequency(double fc, double fs)
    {
        return (fc/(fs/2));
    }
}