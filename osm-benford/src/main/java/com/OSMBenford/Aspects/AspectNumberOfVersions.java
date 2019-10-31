package com.OSMBenford.Aspects;

import com.google.common.collect.Iterables;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.Arrays;
import java.util.List;

public class AspectNumberOfVersions extends Aspect {

    @Override
    public String getLabel() {
        return "number of versions";
    }

    @Override
    public String getSuffix() {
        return "NumberOfVersions";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        return Arrays.asList((Number) Iterables.size(e.getOSHEntity().getVersions()));
    }
}
