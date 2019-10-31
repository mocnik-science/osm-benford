package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTag;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AspectTagValue extends Aspect {
    private Pattern matcherFirstNumber = Pattern.compile("[^0-9]*([0-9]+).*");
    private String label;
    private String suffix;
    private String keyToReturn;
    private Integer intKeyToReturn = null;

    public AspectTagValue(String label, String suffix, String keyToReturn) {
        this.label = label;
        this.suffix = suffix;
        this.keyToReturn = keyToReturn;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getSuffix() {
        return this.suffix;
    }

    protected Number mapFunction(String s) {
        return this.firstNumber(s);
    }

    protected  Number firstNumber(String s) {
        Matcher m = this.matcherFirstNumber.matcher(s);
        if (m.matches()) return Double.parseDouble(m.group(1));
        return null;
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        if (this.intKeyToReturn == null) this.intKeyToReturn = tagTranslator.getOSHDBTagKeyOf(this.keyToReturn).toInt();
        return StreamSupport
                .stream(e.getEntity().getTags().spliterator(), true)
                .filter(t -> t.getKey() == intKeyToReturn)
                .map(tagTranslator::getOSMTagOf)
                .map(OSMTag::getValue)
                .map(this::mapFunction)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
