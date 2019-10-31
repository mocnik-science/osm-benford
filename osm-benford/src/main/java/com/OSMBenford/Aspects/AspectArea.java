package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.geometry.Geo;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.Arrays;
import java.util.List;

public class AspectArea extends Aspect {
    @Override
    public String getLabel() {
        return "area";
    }

    @Override
    public String getSuffix() {
        return "Area";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        return Arrays.asList((Number) Geo.areaOf(e.getGeometry()));
    }
}
