package org.example.util;

import org.example.Things.Location;

import java.util.List;
import java.util.Map;

public final class GeoUtil {

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double lon1Rad = Math.toRadians(lon1);
        double lon2Rad = Math.toRadians(lon2);

        double x = (lon2Rad - lon1Rad) * Math.cos((lat1Rad + lat2Rad) / 2);
        double y = (lat2Rad - lat1Rad);

        double distance = Math.sqrt(x * x + y * y) * Config.EARTH_RADIUS;

        return Math.round(distance * 100.0) / 100.0;
    }

    public static double calculateDistance(Location location1, Location location2){
        return calculateDistance(location1.getLat(),
                location1.getLon(),
                location2.getLat(),
                location2.getLon());
    }

    private GeoUtil(){

    }


}
