package com.OSMBenford.Helper;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.io.*;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class FileHandling {
    public static void writePythonToFile(String python, String filename) throws IOException {
        FileHandling.writePythonToFile(python, filename, false);
    }

    public static void appendPythonToFile(String python, String filename) throws IOException {
        FileHandling.writePythonToFile(python, filename, true);
    }

    public static void writePythonToFile(String python, String filename, Boolean append) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("../output-python/" + filename + ".py", append));
        writer.write(python);
        writer.close();
    }

    public static void writeWKTToFile(Geometry geometry, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("../output-wkt/" + filename + ".wkt", false));
        WKTWriter w = new WKTWriter();
        w.writeFormatted(geometry, writer);
        writer.close();
    }
    public static SortedMap<String, Geometry> readWKTFiles() throws IOException, ParseException {
        SortedMap<String, Geometry> countries = new TreeMap<>();
        WKTReader r = new WKTReader();
        for (File file : Objects.requireNonNull(new File("../output-wkt/").listFiles())) if (!file.getName().endsWith(".DS_Store")) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            countries.put(file.getName().replace("iso-", "").replace(".wkt", ""), r.read(reader));
            reader.close();
        }
        return countries;
    }
}
