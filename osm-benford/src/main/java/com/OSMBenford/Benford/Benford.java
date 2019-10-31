package com.OSMBenford.Benford;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Benford {
    public static BenfordResult analyse(List<Number> numbers) {
        long[] resultAbsolute = new long[10];
        for (Number n : numbers) resultAbsolute[Benford.leadingDigit(n)] += 1L;
        return new BenfordResult(resultAbsolute);
    }

    public static BenfordResult analyse(Map<Integer, Long> frequencies) {
        long[] resultAbsolute = new long[10];
        for (Integer n : frequencies.keySet()) resultAbsolute[n] = frequencies.get(n);
        return new BenfordResult(resultAbsolute);
    }

    public static int leadingDigit(Number n) {
        Double d = Math.abs(n.doubleValue());
        return (int) Math.floor(d / Math.pow(10, Math.floor(Math.log10(d))));
    }

    public static double[] benfordDistribution() {
        double[] distribution = new double[10];
        for (int i = 1; i < 10; i++) distribution[i] = Math.log10(((double) i + 1) / i);
        return distribution;
    }

    public static String pythonBenfordDistribution() {
        return "benford = " + Arrays.toString(Benford.benfordDistribution()) + "\n";
    }
}
