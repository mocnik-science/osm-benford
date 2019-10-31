package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.geometry.Geo;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AspectLength extends Aspect {
    @Override
    public String getLabel() {
        return "length";
    }

    @Override
    public String getSuffix() {
        return "Length";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        return this.length(e.getGeometry());
    }

    private List<Number> length(Geometry geometry) {
        if (geometry instanceof GeometryCollection) {
            List<Number> results = new ArrayList<>();
            for (int i = 0; i < geometry.getNumGeometries(); i++) results.addAll(this.length(geometry.getGeometryN(i)));
            return results;
        } else return Arrays.asList((Number) Math.max(Geo.lengthOf(geometry.getBoundary()), Geo.lengthOf(geometry)));
    }
}
