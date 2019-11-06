package org.opentripplanner.graph_builder.annotation;

import java.util.HashMap;
import java.util.Map;

public class LevelAmbiguous extends GraphBuilderOSMAnnotation {

    private static final long serialVersionUID = -6319806552827170495L;

    public static final String FMT = "Could not infer floor number for layer called '%s' at %s. "
            + "Vertical movement will still be possible, but elevator cost might be incorrect. "
            + "Consider an OSM level map.";
    public static final String HTMLFMT = "Could not infer floor number for layer called <a href='http://www.openstreetmap.org/way/%d'>'%s' (%d)</a>"
            + "Vertical movement will still be possible, but elevator cost might be incorrect. "
            + "Consider an OSM level map.";
    private static final String DESCRIPTION = "OpenTripPlanner can't parse the level for this way.";
    private static final String WHY_PROBLEM = "OpenTripPlanner does not support elevators mapped as areas or split levels like e.g. 0.5. Even for ramps/steps/footways which connect two levels, this warning is issued";
    private static final String HOW_TO_FIX = "Currently, for most cases this should be fixed in OpenTripPlanner.";

    final String layerName;

    public LevelAmbiguous(String layerName, long osmId) {
        super(GraphBuilderOSMAnnotation.OsmType.WAY, osmId);
        this.layerName = layerName;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, layerName, osmId);
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, osmId, layerName, osmId);
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
        map.put("Layer name", layerName);
        return map;
    }
}
