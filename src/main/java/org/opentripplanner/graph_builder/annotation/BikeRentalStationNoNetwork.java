package org.opentripplanner.graph_builder.annotation;

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.util.I18NString;

public class BikeRentalStationNoNetwork extends GraphBuilderOSMAnnotation {
    
    private static final long serialVersionUID = 5107291909122060270L;

    public static final String FMT = "Bike rental station at osm node %s (%s) has neither network nor operator.";
    public static final String HTMLFMT = "Bike rental station at osm node <a href=\"http://www.openstreetmap.org/node/%s\">\"%s\"</a> (%s) has neither network nor operator.";
    public static final String DESCRIPTION = "Bike rental station has neither network nor operator.";
    public static final String WHY_PROBLEM = "OTP will treat a bike rental station without network/operator information as compatible-with-all, which may result in wrong route recommendations.";
    public static final String HOW_TO_FIX = "The open street map data should be extended with network/operator information.";

    private I18NString name;

    public BikeRentalStationNoNetwork(long osmId, I18NString name, double lon, double lat) {
        super(GraphBuilderOSMAnnotation.OsmType.NODE, osmId, lon, lat);
        this.name = name;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, osmId, osmId, name);
    }

    @Override
    public String getMessage() {
        return String.format(FMT, osmId, name);
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getWhyProblem() {
        return WHY_PROBLEM;
    }

    @Override
    public String getHowToFix() {
        return HOW_TO_FIX;
    }

    public Map<String, Object> getAdditionalInfo() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("Name", name != null ? name.toString() : "null");
        return map;
    }
}
