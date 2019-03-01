package org.opentripplanner.graph_builder.annotation;

import java.util.HashMap;
import java.util.Map;

public class TurnRestrictionBad extends GraphBuilderOSMAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Bad turn restriction at relation %s. Reason: %s";
    public static final String HTMLFMT = "Bad turn restriction at relation <a href='http://www.openstreetmap.org/relation/%s'>%s</a>. Reason: %s";
    public static final String DESCRIPTION = "Bad turn restriction at relation as required from/via/to element is missing.";
    public static final String WHY_PROBLEM = "";
    public static final String HOW_TO_FIX = "";

    private final String reason;

    public TurnRestrictionBad(long relationOSMID, String reason) {
        super(GraphBuilderOSMAnnotation.OsmType.RELATION, relationOSMID);
        this.reason = reason;
    }
    
    public TurnRestrictionBad(long relationOSMID, String reason, double lon, double lat) {
        super(GraphBuilderOSMAnnotation.OsmType.RELATION, relationOSMID, lon, lat);
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, osmId, reason);
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, osmId, osmId, reason);
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
        map.put("Reason", reason);
        return map;
    }
}
