package com.example.myfirstapp;

import java.util.Arrays;

public class Butterworth {

    private final int order;
    private final double cut;
    private final boolean low;
    private final double[] b;
    private final double[] a;

    public Butterworth(int order, double cut, boolean low) {
        this.order = order;
        this.cut = cut;
        this.low = low;
        a = computeA();
        b = computeB();

        if (a[0] == 1.) {
            for (int i = 1; i < a.length; ++i) {
                a[i] /= a[0];
            }
            for (int i = 0; i < b.length; ++i) {
                b[i] /= a[0];
            }
        }
    }

    private double computeScale() {
        double omega = Math.PI * cut;
        double fomega = Math.sin(omega);
        double parg0 = Math.PI / (double) (2 * order);

        double sf = 1.0f;
        for (int k = 0; k < order / 2; ++k) {
            sf *= 1.0 + fomega * Math.sin((double) (2 * k + 1) * parg0);
        }

        fomega = Math.sin(omega / 2.0);

        if (order % 2 == 1) {
            sf *= fomega + (low ? Math.cos(omega / 2.0) : Math.sin(omega / 2.));
        }
        sf = (double) Math.pow(fomega, order) / sf;

        return sf;
    }

    private double[] computeB() {
        double[] ccof = new double[order + 1];

        ccof[0] = 1;
        ccof[1] = order;

        for (int i = 2; i < order / 2 + 1; ++i) {
            ccof[i] = (order - i + 1) * ccof[i - 1] / i;
            ccof[order - i] = ccof[i];
        }

        ccof[order - 1] = order;
        ccof[order] = 1;

        if (!low) {
            for (int i = 1; i < order + 1; i += 2) {
                ccof[i] = -ccof[i];
            }
        }

        double scale = computeScale();

        for (int i = 0; i < ccof.length; ++i) {
            ccof[i] *= scale;
        }

        return ccof;
    }

    public double[] filter(double[] signal) {
        double[] result = new double[signal.length];
        for (int i = 0; i < signal.length; ++i) {
            double tmp = 0.0;
            for (int j = 0; j < b.length; ++j) {
                if (i - j < 0) continue;
                tmp += b[j] * signal[i - j];
            }
            for (int j = 1; j < a.length; ++j) {
                if (i - j < 0) continue;
                tmp -= a[j] * result[i - j];
            }
            tmp /= a[0];
            result[i] = tmp;
        }
        return result;
    }

    private double[] computeA() {
        double parg; // pole angle
        double sparg; // sine of the pole angle
        double cparg; // cosine of the pole angle
        double a; // workspace variable
        double[] rcof = new double[2 * order]; // binomial coefficients

        double theta = Math.PI * cut;
        double st = Math.sin(theta);
        double ct = Math.cos(theta);

        for (int k = 0; k < order; ++k) {
            parg = Math.PI * (double) (2 * k + 1) / (double) (2 * order);
            sparg = Math.sin(parg);
            cparg = Math.cos(parg);
            a = 1. + st * sparg;
            rcof[2 * k] = -ct / a;
            rcof[2 * k + 1] = -st * cparg / a;
        }

        // compute the binomial
        double[] temp = binomialMult(rcof);

        // we only need the n+1 coefficients
        double[] dcof = new double[order + 1];
        dcof[0] = 1.0;
        dcof[1] = temp[0];
        dcof[2] = temp[2];
        for (int k = 3; k < order + 1; ++k) {
            dcof[k] = temp[2 * k - 2];
        }

        return dcof;
    }

    private static double[] binomialMult(double[] p) {
        int n = p.length / 2;
        double[] a = new double[2 * n];

        for (int i = 0; i < n; ++i) {
            for (int j = i; j > 0; --j) {
                a[2 * j] += p[2 * i] * a[2 * (j - 1)] - p[2 * i + 1]
                        * a[2 * (j - 1) + 1];
                a[2 * j + 1] += p[2 * i] * a[2 * (j - 1) + 1] + p[2 * i + 1]
                        * a[2 * (j - 1)];
            }

            a[0] += p[2 * i];
            a[1] += p[2 * i + 1];
        }

        return a;
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + Arrays.toString(a) + "\n" + Arrays.toString(b);
    }
}
