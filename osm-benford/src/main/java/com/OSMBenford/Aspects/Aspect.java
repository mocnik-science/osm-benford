package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract public class Aspect {
    private Integer index = null;
    public void setIndex(Integer index) {
        this.index = index;
    }
    public Integer getIndex() {
        return this.index;
    }
    abstract public String getLabel();
    abstract public String getSuffix();
    public List<Map.Entry<Integer, Number>> compute(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        List<Number> cs = this.computeInternal(e, tagTranslator);
        List<Map.Entry<Integer, Number>> rs = new ArrayList();
        if (cs == null) return rs;
        for (Number c : cs) rs.add(Map.entry(this.getIndex(), c));
        return rs;
    }
    abstract protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator);
}