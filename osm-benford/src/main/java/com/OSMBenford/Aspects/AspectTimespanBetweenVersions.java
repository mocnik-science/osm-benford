package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AspectTimespanBetweenVersions extends Aspect {
    @Override
    public String getLabel() {
        return "timespan between versions";
    }

    @Override
    public String getSuffix() {
        return "TimespanBetweenVersions";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        List<Long> ts = StreamSupport.stream(e.getOSHEntity().getVersions().spliterator(), true).map(x -> x.getTimestamp().getRawUnixTimestamp()).collect(Collectors.toList());
        List<Number> rs = new ArrayList<>();
        for (int i = 1; i < ts.size() - 1; i++) rs.add(ts.get(i) - ts.get(i + 1));
        return rs;
    }
}
