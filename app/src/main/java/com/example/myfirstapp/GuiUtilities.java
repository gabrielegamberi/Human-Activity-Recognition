package com.example.myfirstapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class GuiUtilities {
    // Setting the UI to be "immersive"
    public static void hideSystemUI(Activity activity) {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        //| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    }

    public static void chartInit(Context context, LineChart chart, float delta)
    {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(true);
        chart.setMaxHighlightDistance(300);
        chart.setGridBackgroundColor(Color.WHITE);
        chart.setPinchZoom(false);
        chart.setBackgroundColor(Color.rgb(43,48,54));
        chart.getDescription().setEnabled(false);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        chart.setData(data);

        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setFormSize(25);
        l.setTextColor(Color.WHITE);

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(ContextCompat.getColor(context,R.color.myBlack));
        xAxis.setEnabled(true);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawGridLines(true);
        yAxis.setTextColor(Color.WHITE);
        yAxis.setAxisMaximum(delta);
        yAxis.setAxisMinimum(-delta);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getAxisLeft().setDrawGridLines(true);
        chart.getXAxis().setDrawGridLines(true);
        chart.setDrawBorders(true);
    }

    public static LineDataSet createSet(int color, String label)
    {
        LineDataSet set = new LineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        set.setLineWidth(2);
        return set;
    }

    //Adding new entries to chart (static window)
    public static void addEntry(Context context, Measurement measurements, LineChart chart)
    {
        LineData data = chart.getData();
        if (data != null) {
            LineDataSet setX =  (LineDataSet)data.getDataSetByIndex(0);
            LineDataSet setY =  (LineDataSet)data.getDataSetByIndex(1);
            LineDataSet setZ =  (LineDataSet)data.getDataSetByIndex(2);
            if (setX == null) {
                setX = GuiUtilities.createSet(ContextCompat.getColor(context,R.color.myViolet), "X");
                setY = GuiUtilities.createSet(ContextCompat.getColor(context,R.color.myBlue), "Y");
                setZ = GuiUtilities.createSet(ContextCompat.getColor(context,R.color.myCyan), "Z");

                data.addDataSet(setX);
                data.addDataSet(setY);
                data.addDataSet(setZ);
            }
            for (int i = 0; i < Measurement.WINDOW_SIZE ; i++)
            {
                data.addEntry(new Entry(setX.getEntryCount(), measurements.getX().get(i).floatValue()), 0);
                data.addEntry(new Entry(setY.getEntryCount(), measurements.getY().get(i).floatValue()), 1);
                data.addEntry(new Entry(setZ.getEntryCount(), measurements.getZ().get(i).floatValue()), 2);

            }
            data.notifyDataChanged();
            chart.notifyDataSetChanged();

            chart.setVisibleXRangeMaximum(Measurement.WINDOW_SIZE);
            chart.moveViewToX(data.getEntryCount());
        }
    }

    //Adding new entries to chart (live)
    public static void addEntry(Context context, SensorEvent sensorEvent, LineChart chart) {
        LineData data = chart.getData();
        if (data != null) {
            ILineDataSet setX = data.getDataSetByIndex(0);
            ILineDataSet setY = data.getDataSetByIndex(1);
            ILineDataSet setZ = data.getDataSetByIndex(2);
            if (setX == null) {
                setX = GuiUtilities.createSet(ContextCompat.getColor(context,R.color.myViolet), "X");
                setY = GuiUtilities.createSet(ContextCompat.getColor(context,R.color.myBlue), "Y");
                setZ = GuiUtilities.createSet(ContextCompat.getColor(context,R.color.myCyan), "Z");

                data.addDataSet(setX);
                data.addDataSet(setY);
                data.addDataSet(setZ);
            }
            data.addEntry(new Entry(setX.getEntryCount(), sensorEvent.values[0]), 0);
            data.addEntry(new Entry(setY.getEntryCount(), sensorEvent.values[1]), 1);
            data.addEntry(new Entry(setZ.getEntryCount(), sensorEvent.values[2]), 2);

            data.notifyDataChanged();
            chart.notifyDataSetChanged();

            chart.setVisibleXRangeMaximum(50);
            chart.moveViewToX(data.getEntryCount());
        }
    }
}
