package org.opentripplanner.graph_builder.annotation;

import java.util.HashMap;
import java.util.Map;

public class TurnRestrictionUnknown extends GraphBuilderOSMAnnotation {

    private static final long serialVersionUID = -4608335336301535828L;

    public static final String FMT = "Invalid turn restriction tag %s in turn restriction %d";
    public static final String HTMLFMT = "Invalid turn restriction tag %s in  <a href=\"http://www.openstreetmap.org/relation/%d\">\"%d\"</a>";
    public static final String DESCRIPTION = "OTP currently supports the following restriction tags: [no|only]_[right|left|u]_turn and [no|only]_straight_on. It does not recognize restriction=no_entry or conditional restriction tags like restriction:hgv.";
    public static final String WHY_PROBLEM = "This turn restriction currently is not supported by OpenTripPlanner. OTP's routes will not respect it.";
    public static final String HOW_TO_FIX = "Probably, OTP needs to be extended to support this tag (like no_entry) or to ignore it silently (like restricition:hgv). Otherwise, check if tag restriction tag contains a typo. ";

    final String tagval;

    public TurnRestrictionUnknown(long relationId, String tagval) {
        super(GraphBuilderOSMAnnotation.OsmType.RELATION, relationId);
        this.tagval = tagval;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(FMT, tagval, osmId, osmId);
    }

    @Override
    public String getMessage() {
        return String.format(FMT, tagval, osmId);
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
        map.put("Tag value", tagval);
        return map;
    }
}
