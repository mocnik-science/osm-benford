package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTag;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AspectTagValueLength extends Aspect {
    @Override
    public String getLabel() {
        return "tag value length";
    }

    @Override
    public String getSuffix() {
        return "TagValueLength";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        return StreamSupport
                .stream(e.getEntity().getTags().spliterator(), true)
                .map(tagTranslator::getOSMTagOf)
                .map(OSMTag::getValue)
                .map(String::length)
                .collect(Collectors.toList());
    }
}
