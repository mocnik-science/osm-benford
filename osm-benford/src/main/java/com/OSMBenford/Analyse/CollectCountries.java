package com.OSMBenford.Analyse;

import com.OSMBenford.Helper.FileHandling;
import com.OSMBenford.Helper.Progress;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CollectCountries {
    public static void collect(MapReducer<OSMEntitySnapshot> oshdbView, TagTranslator tagTranslator) throws Exception {
        Progress.query("collect countries");
        String keyISO3166 = "ISO3166-1:alpha3";
        int intKeyISO3166 = tagTranslator.getOSHDBTagKeyOf(keyISO3166).toInt();
        String keyType = "type";
        int intKeyType = tagTranslator.getOSHDBTagKeyOf(keyType).toInt();
        List<Map.Entry<String, Map.Entry<String, Geometry>>> countriesRaw = oshdbView
                .osmTag("admin_level", "2")
                .osmTag(keyISO3166)
                .map(s -> {
                    try {
                        String iso = null;
                        for (OSHDBTag tag : s.getEntity().getTags()) if (tag.getKey() == intKeyISO3166) iso = tagTranslator.getOSMTagOf(tag).getValue();
                        Geometry g = s.getGeometryUnclipped();
                        if (g instanceof GeometryCollection) g = g.union();
                        if (!(g instanceof Polygon) && !(g instanceof MultiPolygon)) return null;
                        String type = "null";
                        for (OSHDBTag tag : s.getEntity().getTags()) if (tag.getKey() == intKeyType) type = tagTranslator.getOSMTagOf(tag).getValue();
                        if (iso == null || type == null) return null;
                        if (g == null) {
                            System.out.println("Geometry could not be constructed for " + iso);
                            return null;
                        }
                        for (OSHDBTag tag : s.getEntity().getTags()) if (tag.getKey() == intKeyISO3166) return Map.entry(iso, Map.entry(type, g));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect();
        Map<String, Geometry> countries = new HashMap<>();
        for (Map.Entry<String, Map.Entry<String, Geometry>> country : countriesRaw) if (country.getValue().getKey().equals("boundary") || !countries.containsKey(country.getKey())) countries.put(country.getKey(), country.getValue().getValue());
        for (Map.Entry<String, Geometry> country : countries.entrySet()) {
            if (country.getValue() == null) System.out.println("no geometry found for " + country.getKey());
            else FileHandling.writeWKTToFile(country.getValue(), country.getKey());
        }
    }
/*    public static void collect(MapReducer<OSMEntitySnapshot> oshdbView, TagTranslator tagTranslator) throws Exception {
        com.benfordOsm.Helper.Progress.query("collect countries");
        String keyISO3166 = "ISO3166-1:alpha3";
        int intKeyISO3166 = tagTranslator.getOSHDBTagKeyOf(keyISO3166).toInt();
        String keyType = "type";
        int intKeyType = tagTranslator.getOSHDBTagKeyOf(keyType).toInt();
        List<Map.Entry<String, Map.Entry<String, Geometry>>> countriesRaw = oshdbView
                .osmTag("admin_level", "2")
                .osmTag(keyISO3166)
                .map(s -> {
                    try {
                        System.out.println("1");
                        Geometry g = s.getGeometryUnclipped();
                        System.out.println(g.toString().substring(0, 70));
                        if (g instanceof GeometryCollection)
                            g = g.union();
                        System.out.println(g.toString().substring(0, 70));
                        if (!(g instanceof Polygon) && !(g instanceof MultiPolygon)) {
                            System.out.println("HURRAY, no geometry found");
                            return null;
                        }
                        System.out.println(g == null);
                        String type = "null";
                        for (OSHDBTag tag : s.getEntity().getTags())
                            if (tag.getKey() == intKeyType)
                                type = tagTranslator.getOSMTagOf(tag).getValue();
                        System.out.println("2");
                        for (OSHDBTag tag : s.getEntity().getTags())
                            if (tag.getKey() == intKeyISO3166)
                                System.out.println(tagTranslator.getOSMTagOf(tag).getValue() + " " + type);
                        System.out.println("3");
                        if (type == null) {
                            System.out.println("HURRAY, type is null: ");
                            return null;
                        }
                        if (g == null) {
                            System.out.println("HURRAY, g is null: ");
                            return null;
                        }
                        for (OSHDBTag tag : s.getEntity().getTags())
                            if (tag.getKey() == intKeyISO3166) {
                                System.out.println("4");
                                System.out.println(type);
                                System.out.println(g != null);
                                Map.Entry<String, Geometry> y = Map.entry(type, g);
                                System.out.println("4b");
                                String a = tagTranslator.getOSMTagOf(tag).getValue();
                                System.out.println(a);
                                System.out.println("4c");
                                Map.Entry<String, Map.Entry<String, Geometry>> x = Map.entry(a, y);
                                System.out.println("5");
                                System.out.println(x == null);
                                System.out.println("6");
                                return x;
                            }
                        System.out.println("7");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("NOPE");
                    return null;
//                    for (OSHDBTag tag : s.getEntity().getTags()) if (tag.getKey() == intKeyISO3166) return Map.entry(tagTranslator.getOSMTagOf(tag).getValue(), Map.entry(type, g));
//                    return (Map.Entry<String, Map.Entry<String, Geometry>>) null;
                })
                .filter(Objects::nonNull)
                .collect();
        System.out.println("HURRRRRAAAAYYYY");
        System.out.println(countriesRaw.size());
        Map<String, Geometry> countries = new HashMap<>();
        for (Map.Entry<String, Map.Entry<String, Geometry>> country : countriesRaw) System.out.println(country.getKey() + " " + country.getValue().getKey() + " " + (country.getValue().getValue() != null));
        for (Map.Entry<String, Map.Entry<String, Geometry>> country : countriesRaw) if (country.getValue().getKey().equals("boundary") || !countries.containsKey(country.getKey())) countries.put(country.getKey(), country.getValue().getValue());
        System.out.println(countries.size());
        for (Map.Entry<String, Geometry> country : countries.entrySet()) {
            System.out.println(country.getKey() + " " + (country.getKey() != null));
            if (country.getValue() == null) System.out.println("no geometry found for " + country.getKey());
            else com.benfordOsm.Helper.FileHandling.writeWKTToFile(country.getValue(), country.getKey());
        }
    }*/
}