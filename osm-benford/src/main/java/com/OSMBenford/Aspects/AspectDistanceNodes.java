package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.geometry.Geo;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class AspectDistanceNodes extends Aspect {
    @Override
    public String getLabel() {
        return "distance between nodes";
    }

    @Override
    public String getSuffix() {
        return "DistanceNodes";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        Coordinate[] cs = e.getGeometry().getCoordinates();
        List<Number> rs = new ArrayList<>();
        for (int i = 1; i < cs.length; i++) rs.add(Geo.distanceBetweenCoordinates(cs[i - 1].getY(), cs[i - 1].getX(), cs[i].getY(), cs[i].getX()));
        return rs;
    }
}
