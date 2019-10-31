package com.OSMBenford.Analyse;

import com.OSMBenford.Benford.Benford;
import com.OSMBenford.Helper.FileHandling;
import com.OSMBenford.Helper.Progress;

import java.io.IOException;

public class Analyse {
    public static void benford() throws IOException {
        Progress.query("com.benfordOsm.Benford.Benford");
        String python = "";
        python += Benford.pythonBenfordDistribution();
        python += "plotDistribution(benford, 'com.benfordOsm.Benford.Benford\\'s law', None, isBenford=True)\n";
        FileHandling.writePythonToFile(python, "benford");
    }
}
