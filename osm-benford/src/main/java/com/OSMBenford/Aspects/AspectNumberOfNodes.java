package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.Arrays;
import java.util.List;

public class AspectNumberOfNodes extends Aspect {
    @Override
    public String getLabel() {
        return "number of nodes";
    }

    @Override
    public String getSuffix() {
        return "NumberOfNodes";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        return Arrays.asList((Number) e.getGeometry().getNumPoints());
    }
}
