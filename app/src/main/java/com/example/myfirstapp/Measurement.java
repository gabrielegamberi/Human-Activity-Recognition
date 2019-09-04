package com.example.myfirstapp;
import androidx.annotation.NonNull;
import java.util.Vector;

class Measurement implements Cloneable
{
    private Vector<Double> x;
    private Vector<Double> y;
    private Vector<Double> z;
    public static final int WINDOW_SIZE = 128;

    Measurement(boolean setAllZeros)
    {
        x = new Vector<>();
        y = new Vector<>();
        z = new Vector<>();
        if(setAllZeros)
        {
            for (int i = 0; i < WINDOW_SIZE ; i++)
            {
                x.add(0.0);
                y.add(0.0);
                z.add(0.0);
            }
        }
    }

    public Vector<Double> getX() {
        return x;
    }

    public Vector<Double> getY() {
        return y;
    }

    public Vector<Double> getZ() {
        return z;
    }

    public void setX(Vector<Double> x) {
        this.x = x;
    }

    public void setY(Vector<Double> y) {
        this.y = y;
    }

    public void setZ(Vector<Double> z) {
        this.z = z;
    }

    public void rearrangeHalfBack()
    {
        x = new Vector<>(x.subList(64,WINDOW_SIZE));
        y = new Vector<>(y.subList(64,WINDOW_SIZE));
        z = new Vector<>(z.subList(64,WINDOW_SIZE));
    }

    public void insert(double x, double y, double z)
    {
        if(this.x.size()<WINDOW_SIZE)
        {
            this.x.addElement(x);
            this.y.addElement(y);
            this.z.addElement(z);

        }
    }

    public void clear()
    {
        x = new Vector<>();
        y = new Vector<>();
        z = new Vector<>();
    }

    public boolean isFull()
    {
        if(x.size()==WINDOW_SIZE)
            return true;
        else
            return false;
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}



