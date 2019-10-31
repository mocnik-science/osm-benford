package com.OSMBenford.Benford;

import java.util.Arrays;

public class BenfordResult {
    private long[] absolute;
    private double[] relative = null;

    public BenfordResult(long[] absolute) {
        this.absolute = absolute;
    }

    public long[] getAbsolute() {
        return this.absolute;
    }

    public double[] getRelative() {
        if (this.relative == null) {
            Arrays.stream(this.absolute).sum();
            double sum = Arrays.stream(this.absolute).sum();
            this.relative = new double[10];
            for (int i = 0; i < 10; i++) this.relative[i] = this.absolute[i] / sum;
        }
        return this.relative;
    }

    public String python() {
        return this.python("");
    }

    public String python(String suffix) {
        return this.python(suffix, false);
    }

    public String python(String suffix, Boolean includeRelative) {
        String s = "absolute" + suffix + " = " + Arrays.toString(this.getAbsolute()) + "\n";
        if (includeRelative) s += "relative" + suffix + " = " + Arrays.toString(this.getRelative()) + "\n";
        return s;
    }
}
