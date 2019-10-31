package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.Arrays;
import java.util.List;

public class AspectBearingNormalized extends Aspect {
    @Override
    public String getLabel() {
        return "bearing normalized";
    }

    @Override
    public String getSuffix() {
        return "BearingNormalized";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        List<Number> b = (new AspectBearing()).computeInternal(e, tagTranslator);
        if (b == null) return null;
        else return Arrays.asList(b.get(0).doubleValue() / 360);
    }
}
