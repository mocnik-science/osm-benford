package com.OSMBenford.Analyse;

import com.OSMBenford.Aspects.Aspect;
import com.OSMBenford.Aspects.AspectNumberOfVersions;
import com.OSMBenford.Aspects.AspectTimespanBetweenVersions;
import com.OSMBenford.Benford.Benford;
import com.OSMBenford.Helper.FileHandling;
import com.OSMBenford.Helper.Progress;
import org.OsmosisModified.PbfReaderModified;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AnalyseOsmosis {
    private static final int NODE = 0;
    private static final int WAY = 1;
    private static final int RELATION = 2;

    public static void analyseGlobal(File osmFile) throws IOException {
        AnalyseOsmosis.compute(null, osmFile, null);
    }

    public static void analyseCountries(File osmFile) throws IOException, ParseException {
        for (Map.Entry<String, Geometry> country : FileHandling.readWKTFiles().entrySet()) AnalyseOsmosis.compute(country.getKey(), osmFile, country.getValue());
    }

    public static String compute(String iso, File osmFile, Geometry geometry) throws IOException {
        PbfReaderModified reader = new PbfReaderModified(osmFile, 1);
        AspectsOsmosisWorker worker = new AspectsOsmosisWorker(iso, "nodes", "Nodes", AnalyseOsmosis.NODE, geometry);
        reader.setSink(worker);
        reader.run();
        if (worker.getException() != null) throw worker.getException();
        return worker.getResult();
    }

    public static class AspectsOsmosisWorker implements Sink {
        private String iso;
        private String labelData;
        private String suffix;
        private int entityClass;
        private Geometry geometry;

        private String python = "";
        private IOException exception = null;

        private GeometryFactory geometryFactory = new GeometryFactory();

        private Long lastId = -1L;
        private Integer lastVersion = null;
        private Long lastTimestamp = null;
        private Map<Integer, Long> dataNumberOfVersions = new HashMap<>();
        private Map<Integer, Long> dataTimespanBetweenVersions = new HashMap<>();

        public AspectsOsmosisWorker(String iso, String labelData, String suffix, int entityClass, Geometry geometry) {
            this.iso = iso;
            this.labelData = labelData;
            this.suffix = suffix;
            this.entityClass = entityClass;
            this.geometry = geometry;
            for (int i = 0; i < 10; i++) this.dataNumberOfVersions.put(i, 0L);
            for (int i = 0; i < 10; i++) this.dataTimespanBetweenVersions.put(i, 0L);
        }

        public String getResult() {
            return this.python;
        }

        public IOException getException() {
            return this.exception;
        }

        @Override
        public void initialize(Map<String, Object> map) {
            Progress.query(((iso == null) ? "global - " : iso + " - ") + labelData);
        }

        private void process(long id, int version, Date timestamp, Double latitude, Double longitude) {
            if (this.geometry != null && !this.geometryFactory.createPoint(new Coordinate(longitude, latitude)).within(this.geometry)) return;
            if (this.lastId != id && this.lastId != -1L) {
                int n = Benford.leadingDigit(this.lastVersion);
                this.dataNumberOfVersions.put(n, this.dataNumberOfVersions.get(n) + 1);
            }
            long t = timestamp.getTime();
            if (this.lastId == id && this.lastId != -1L) {
                int n = Benford.leadingDigit(t - this.lastTimestamp);
                this.dataTimespanBetweenVersions.put(n, this.dataTimespanBetweenVersions.get(n) + 1);
            }
            this.lastId = id;
            this.lastVersion = version;
            this.lastTimestamp = t;
        }

        @Override
        public void process(EntityContainer entityContainer) {
            if (this.entityClass == AnalyseOsmosis.NODE && entityContainer instanceof NodeContainer) {
                Node node = ((NodeContainer) entityContainer).getEntity();
                this.process(node.getId(), node.getVersion(), node.getTimestamp(), node.getLatitude(), node.getLongitude());
            } else if (this.entityClass == AnalyseOsmosis.WAY && entityContainer instanceof WayContainer) {
                Way way = ((WayContainer) entityContainer).getEntity();
                this.process(way.getId(), way.getVersion(), way.getTimestamp(), null, null);
            } else if (this.entityClass == AnalyseOsmosis.RELATION && entityContainer instanceof RelationContainer) {
                Relation relation = ((RelationContainer) entityContainer).getEntity();
                this.process(relation.getId(), relation.getVersion(), relation.getTimestamp(), null, null);
            }
        }

        @Override
        public void complete() {
            // complete last element
            int n = Benford.leadingDigit(this.lastVersion);
            this.dataNumberOfVersions.put(n, this.dataNumberOfVersions.get(n) + 1);
            // result
            try {
                Aspect aspect = new AspectNumberOfVersions();
                this.python += Benford.analyse(this.dataNumberOfVersions).python(aspect.getSuffix() + "_" + suffix) + "computeForDistribution(absolute" + aspect.getSuffix() + "_" + suffix + ", '" + labelData + "', '" + aspect.getLabel() + "'" + ((iso == null) ? "" : ", iso='" + iso + "'") + ")\n";
                aspect = new AspectTimespanBetweenVersions();
                this.python += Benford.analyse(this.dataTimespanBetweenVersions).python(aspect.getSuffix() + "_" + suffix) + "computeForDistribution(absolute" + aspect.getSuffix() + "_" + suffix + ", '" + labelData + "', '" + aspect.getLabel() + "'" + ((iso == null) ? "" : ", iso='" + iso + "'") + ")\n";
                FileHandling.appendPythonToFile(this.python, "iso-" + ((iso == null) ? "global" : iso));
            } catch (IOException e) {
                this.exception = e;
            }
        }

        @Override
        public void close() {}
    }
}
