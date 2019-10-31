package com.OSMBenford;

import com.OSMBenford.Analyse.Analyse;
import com.OSMBenford.Analyse.AnalyseOshdb;
import com.OSMBenford.Analyse.CollectCountries;
import com.OSMBenford.Helper.Progress;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMEntitySnapshotView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

/*

mvn install
export MAVEN_OTPS=-Xmx56g
mvn exec:java -Dexec.mainClass="com.BenfordOsm.Main" > ../analysis.log

*/

public class Main {
    private static final boolean step1 = false; // benford
    private static final boolean step2 = false; // collect countries
    private static final boolean step3 = false; // oshdb - countries
    private static final boolean step4 = false; // oshdb - global
    private static final boolean step5 = false; // oshdb - scale - ITC, University of Twente, Enschede, the Netherlands
    private static final boolean step6 = false; // oshdb - scale - Eiffel Tower, Paris, France
    private static final boolean step7 = false; // oshdb - scale - The Museum of Modern Art, New York City, NY, USA

    public static void main(String args[]) throws Exception {
        Progress.starting();

        /* WITHOUT DATABASE */

        if (step1) Analyse.benford();

        /* OSHDB*/

        if (step2 || step3 || step4 || step5 || step6 || step7) {
            // database
            OSHDBH2 oshdbH2 = new OSHDBH2(".../data/planet.oshdb");
            OSHDBDatabase oshdb = oshdbH2.inMemory(false).multithreading(true);
            TagTranslator tagTranslator = new TagTranslator(oshdbH2.getConnection());
            MapReducer<OSMEntitySnapshot> oshdbView = OSMEntitySnapshotView.on(oshdb)
                    .timestamps("2019-06-10");

            // content
            if (step2) CollectCountries.collect(oshdbView, tagTranslator);
            if (step3) AnalyseOshdb.analyseCountries(oshdbView, tagTranslator);
            if (step4) AnalyseOshdb.analyseGlobal(oshdbView, tagTranslator);
            if (step5) AnalyseOshdb.analyseScaleDependency(oshdbView, tagTranslator, 6.885556, 52.223611);
            if (step6) AnalyseOshdb.analyseScaleDependency(oshdbView, tagTranslator, 2.294500, 48.858222);
            if (step7) AnalyseOshdb.analyseScaleDependency(oshdbView, tagTranslator, -73.977222, 40.761111);
        }

        /* FINISHED */

        Progress.finished();
    }
}
