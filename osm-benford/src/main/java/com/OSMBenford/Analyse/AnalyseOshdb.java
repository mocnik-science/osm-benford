package com.OSMBenford.Analyse;

import com.OSMBenford.Aspects.*;
import com.OSMBenford.Benford.Benford;
import com.OSMBenford.Helper.FileHandling;
import com.OSMBenford.Helper.Progress;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializablePredicate;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTag;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTagKey;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.util.*;

public class AnalyseOshdb {
    private static final String ISO_SCALE = "SCALE_";

    public static void analyseGlobal(MapReducer<OSMEntitySnapshot> oshdbView, TagTranslator tagTranslator) throws Exception {
        AnalyseOshdb.analyse(null, oshdbView, tagTranslator, false);
    }

    public static void analyseCountries(MapReducer<OSMEntitySnapshot> oshdbView, TagTranslator tagTranslator) throws Exception {
        for (Map.Entry<String, Geometry> country : FileHandling.readWKTFiles().entrySet()) {
            MapReducer<OSMEntitySnapshot> oshdbViewCountry = null;
            if (country.getValue() instanceof Polygon) oshdbViewCountry = oshdbView.areaOfInterest((Polygon) country.getValue());
            else if (country.getValue() instanceof MultiPolygon) oshdbViewCountry = oshdbView.areaOfInterest((MultiPolygon) country.getValue());
            if (oshdbViewCountry != null) AnalyseOshdb.analyse(country.getKey(), oshdbViewCountry, tagTranslator, true);
            else System.out.println("The query could not be executed for country " + country.getKey());
        }
/*        String keyISO3166 = "ISO3166-1:alpha3";
        int intKeyISO3166 = tagTranslator.getOSHDBTagKeyOf(keyISO3166).toInt();
        String keyType = "type";
        int intKeyType = tagTranslator.getOSHDBTagKeyOf(keyType).toInt();
        List<Map.Entry<String, Map.Entry<String, Geometry>>> countriesRaw = oshdbView
                .osmTag("admin_level", "2")
                .osmTag(keyISO3166)
                .map(s -> {
                    Geometry g = s.getGeometry();
                    if (!(g instanceof Polygon) && !(g instanceof MultiPolygon)) g = null;
                    String type = null;
                    for (OSHDBTag tag : s.getEntity().getTags()) if (tag.getKey() == intKeyType) type = tagTranslator.getOSMTagOf(tag).getValue();
                    for (OSHDBTag tag : s.getEntity().getTags()) if (tag.getKey() == intKeyISO3166) return Map.entry(tagTranslator.getOSMTagOf(tag).getValue(), Map.entry(type, g));
                    return null;
                })
                .collect();
        Map<String, Geometry> countries = new HashMap<>();
        for (Map.Entry<String, Map.Entry<String, Geometry>> country : countriesRaw) if (country.getValue().getKey() == "boundary" || !countries.containsKey(country.getKey())) countries.put(country.getKey(), country.getValue().getValue());
        for (Map.Entry<String, Geometry> country : countries.entrySet()) {
            MapReducer<OSMEntitySnapshot> oshdbViewCountry = null;
            if (country.getValue() == null) System.out.println("no geometry found for " + country.getKey());
            else if (country.getValue() instanceof Polygon) oshdbViewCountry = OSMEntitySnapshotView.on(oshdb).areaOfInterest((Polygon) country.getValue());
            else if (country.getValue() instanceof MultiPolygon) oshdbViewCountry = OSMEntitySnapshotView.on(oshdb).areaOfInterest((MultiPolygon) country.getValue());
            if(oshdbViewCountry != null) com.benfordOsm.Analyse.AnalyseOshdb.analyse(country.getKey(), oshdbViewCountry, tagTranslator, true);
        }*/
    }

    public static void analyseScaleDependency(MapReducer<OSMEntitySnapshot> oshdbView, TagTranslator tagTranslator, double lon, double lat) throws Exception {
        for (double i = 0; i <= 5; i += .25) {
            double d = Math.pow(10, -i);
            double dCos = d / Math.cos(Math.toRadians(lat));
            AnalyseOshdb.analyse(ISO_SCALE + i + "_" + lon + "_" + lat, oshdbView.areaOfInterest(new OSHDBBoundingBox(lon - dCos, lat - d, lon + dCos, lat + d)), tagTranslator, false);
        }
    }

    private static List<Aspect> getAspectsAny(Boolean countryOnly) {
        List<Aspect> aspectsAny = new ArrayList<>();
        aspectsAny.add(new AspectNumberOfVersions());
        aspectsAny.add(new AspectTimespanBetweenVersions());
        aspectsAny.add(new AspectTagValueLength());
        return aspectsAny;
    }
    private static List<Aspect> getAspectsOpenOrClosed(Boolean countryOnly) {
        List<Aspect> aspectsOpenOrClosed = new ArrayList<>(AnalyseOshdb.getAspectsAny(countryOnly));
        aspectsOpenOrClosed.add(new AspectNumberOfNodes());
        aspectsOpenOrClosed.add(new AspectDistanceNodes());
        return aspectsOpenOrClosed;
    }
    private static List<Aspect> getAspectsOpen(Boolean countryOnly) {
        List<Aspect> aspectsOpen = new ArrayList<>(AnalyseOshdb.getAspectsOpenOrClosed(countryOnly));
        aspectsOpen.add(new AspectLength());
        if (!countryOnly) {
            aspectsOpen.add(new AspectBearing());
            aspectsOpen.add(new AspectBearingNormalized());
        }
        return aspectsOpen;
    }
    private static List<Aspect> getAspectsClosed(Boolean countryOnly) {
        List<Aspect> aspectsClosed = new ArrayList<>(AnalyseOshdb.getAspectsOpenOrClosed(countryOnly));
        aspectsClosed.add(new AspectArea());
        return aspectsClosed;
    }

    private static void analyse(String iso, MapReducer<OSMEntitySnapshot> oshdbView, TagTranslator tagTranslator, Boolean countryOnly) throws Exception {
        FileHandling.writePythonToFile("", AnalyseOshdb.stringForIsoExtended(iso));
        AnalyseOshdb.compute(iso, "nodes with tags", "NodesWithTags", oshdbView, tagTranslator, OSMType.NODE, null, null, filterHasTag, AnalyseOshdb.getAspectsAny(countryOnly));
        AnalyseOshdb.compute(iso, "ways", "Ways", oshdbView, tagTranslator, OSMType.WAY, null, null, AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "buildings", "Building", oshdbView, tagTranslator, OSMType.WAY, "building", null, AnalyseOshdb.getAspectsClosed(countryOnly));
        AnalyseOshdb.compute(iso, "residential areas", "Residential", oshdbView, tagTranslator, OSMType.WAY, "landuse", "residential", AnalyseOshdb.getAspectsClosed(countryOnly));
        AnalyseOshdb.compute(iso, "retail areas", "Retail", oshdbView, tagTranslator, OSMType.WAY, "landuse", "retail", AnalyseOshdb.getAspectsClosed(countryOnly));
        AnalyseOshdb.compute(iso, "roads", "Highway", oshdbView, tagTranslator, OSMType.WAY, "highway", null, AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "primary roads", "HighwayPrimary", oshdbView, tagTranslator, OSMType.WAY, "highway", "primary", AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "secondary roads", "HighwaySecondary", oshdbView, tagTranslator, OSMType.WAY, "highway", "secondary", AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "tertiary roads", "HighwayTertiary", oshdbView, tagTranslator, OSMType.WAY, "highway", "tertiary", AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "residential roads", "HighwayResidential", oshdbView, tagTranslator, OSMType.WAY, "highway", "residential", AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "tracks", "HighwayTrack", oshdbView, tagTranslator, OSMType.WAY, "highway", "track", AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "tram tracks", "Tram", oshdbView, tagTranslator, OSMType.WAY, "railway", "tram", AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "waterways", "Waterway", oshdbView, tagTranslator, OSMType.WAY, "waterway", null, AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "rivers", "River", oshdbView, tagTranslator, OSMType.WAY, "waterway", "river", AnalyseOshdb.getAspectsOpen(countryOnly));
        AnalyseOshdb.compute(iso, "water bodies", "NaturalWater", oshdbView, tagTranslator, OSMType.WAY, "natural", "water", AnalyseOshdb.getAspectsClosed(countryOnly));
        AnalyseOshdb.compute(iso, "all buildings", "AllBuilding", oshdbView, tagTranslator, (OSMType) null, "building", null, Arrays.asList(new AspectTagValue("street numbers", "StreetNumber", "addr:housenumber"), new AspectTagValue("levels", "Levels", "building:levels")));
        AnalyseOshdb.compute(iso, "towns", "Town", oshdbView, tagTranslator, (OSMType) null, "place", "town", Arrays.asList(new AspectTagValue("population", "Population", "population")));
        AnalyseOshdb.compute(iso, "peaks", "Peak", oshdbView, tagTranslator, (OSMType) null, "natural", "peak", Arrays.asList(new AspectTagValue("elevation", "Elevation", "ele")));
    }

    private static SerializablePredicate<OSMEntitySnapshot> filterHasTag = e -> e.getEntity().getRawTags().length > 0;

    private static String stringForIso(String iso) {
        if (iso == null) return "global";
        else if (iso.startsWith(ISO_SCALE)) return "scale-" + iso.replace(ISO_SCALE, "");
        else return iso;
    }
    private static String stringForIsoExtended(String iso) {
        if (iso == null) return "global";
        else if (iso.startsWith(ISO_SCALE)) return "scale-" + iso.replace(ISO_SCALE, "");
        else return "iso-" + iso;
    }
    private static String stringForIsoPython(String iso) {
        if (iso == null) return "";
        else if (iso.startsWith(ISO_SCALE)) return ", scale='" + iso.replace(ISO_SCALE, "") + "'";
        else return ", iso='" + iso + "'";
    }

    private static String compute(String iso, String labelData, String suffix, MapReducer<OSMEntitySnapshot> mapReducer, TagTranslator tagTranslator, OSMType osmType, String key, String value, List<Aspect> aspects) throws Exception {
        return AnalyseOshdb.compute(iso, labelData, suffix, mapReducer, tagTranslator, (osmType != null) ? EnumSet.of(osmType) : null, key, value, aspects);
    }
    private static String compute(String iso, String labelData, String suffix, MapReducer<OSMEntitySnapshot> mapReducer, TagTranslator tagTranslator, EnumSet<OSMType> osmTypes, String key, String value, List<Aspect> aspects) throws Exception {
        return AnalyseOshdb.compute(iso, labelData, suffix, mapReducer, tagTranslator, osmTypes, key, value, null, aspects);
    }
    private static String compute(String iso, String labelData, String suffix, MapReducer<OSMEntitySnapshot> mapReducer, TagTranslator tagTranslator, OSMType osmType, String key, String value, SerializablePredicate<OSMEntitySnapshot> filter, List<Aspect> aspects) throws Exception {
        return AnalyseOshdb.compute(iso, labelData, suffix, mapReducer, tagTranslator, (osmType != null) ? EnumSet.of(osmType) : null, key, value, filter, aspects);
    }
    private static String compute(String iso, String labelData, String suffix, MapReducer<OSMEntitySnapshot> mapReducer, TagTranslator tagTranslator, EnumSet<OSMType> osmTypes, String key, String value, SerializablePredicate<OSMEntitySnapshot> filter, List<Aspect> aspects) throws Exception {
        Progress.query(AnalyseOshdb.stringForIso(iso) + " - " + labelData);
        if (osmTypes != null) mapReducer = mapReducer.osmType(osmTypes);
        if (filter != null) mapReducer = mapReducer.filter(filter);
        if (key != null) mapReducer = mapReducer.osmTag((value != null) ? new OSMTag(key, value) : new OSMTagKey(key));
        for (int i = 0; i < aspects.size(); i++) aspects.get(i).setIndex(i);
/*        SortedMap<Integer, Long> data = new TreeMap<>();
        for (int i = 0; i < 10 * aspects.size() + 10; i++) data.put(i, 0L);
        data = mapReducer.map(e -> {
            SortedMap<Integer, Long> results = new TreeMap<>();
            for (com.benfordOsm.Aspects.Aspect aspect : aspects) for (Map.Entry<Integer, Number> result : aspect.compute(e, tagTranslator)) {
                Integer n = 10 * result.getKey() + com.benfordOsm.Benford.Benford.leadingDigit(result.getValue());
                results.put(n, results.getOrDefault(n, 0L) + 1L);
            }
            return results;
        }).filter(Objects::nonNull).stream().reduce(data, (result, treeMap) -> {
            for (Map.Entry<Integer, Long> t : treeMap.entrySet()) result.put(t.getKey(), result.get(t.getKey()) + t.getValue());
            return result;
        });*/
        SortedMap<Integer, Long> data = mapReducer.flatMap(e -> {
            List<Map.Entry<Integer, Number>> results = new ArrayList<>();
            for (Aspect aspect : aspects) results.addAll(aspect.compute(e, tagTranslator));
            return results;
        }).filter(Objects::nonNull).aggregateBy(result -> 10 * result.getKey() + Benford.leadingDigit(result.getValue())).map(r -> 1L).reduce(() -> 0L, Long::sum);
        String python = "";
        for (Aspect aspect : aspects) {
            Map<Integer, Long> dataForAspect = new HashMap<>();
            for (Map.Entry<Integer, Long> d : ((Map<Integer, Long>) data.subMap(10 * aspect.getIndex(), 10 * (aspect.getIndex() + 1))).entrySet()) dataForAspect.put(d.getKey() % 10, d.getValue());
            python += Benford.analyse(dataForAspect).python(aspect.getSuffix() + "_" + suffix) + "computeForDistribution(absolute" + aspect.getSuffix() + "_" + suffix + ", '" + labelData + "', '" + aspect.getLabel() + "'" + AnalyseOshdb.stringForIsoPython(iso) + ")\n";
        }
        FileHandling.appendPythonToFile(python, AnalyseOshdb.stringForIsoExtended(iso));
        return python;
    }
}
