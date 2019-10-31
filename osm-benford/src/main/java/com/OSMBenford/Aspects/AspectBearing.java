package com.OSMBenford.Aspects;

import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

import java.util.Arrays;
import java.util.List;

public class AspectBearing extends Aspect {
    @Override
    public String getLabel() {
        return "bearing";
    }

    @Override
    public String getSuffix() {
        return "Bearing";
    }

    @Override
    protected List<Number> computeInternal(OSMEntitySnapshot e, TagTranslator tagTranslator) {
        // https://www.movable-type.co.uk/scripts/latlong.html
        double lat1 = e.getGeometry().getCoordinates()[0].getY();
        double lon1 = e.getGeometry().getCoordinates()[0].getX();
        double lat2 = e.getGeometry().getCoordinates()[e.getGeometry().getCoordinates().length - 1].getY();
        double lon2 = e.getGeometry().getCoordinates()[e.getGeometry().getCoordinates().length - 1].getX();
        if (lat1 == lat2 && lon1 == lon2) return null;
        double dLon = lon2 - lon1;
        double b = Math.toDegrees(Math.atan2(Math.sin(dLon) * Math.cos(lat2), Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)));
        return Arrays.asList((b + 360) % 360);
    }
}
