package org.opentripplanner.graph_builder.annotation;

public class StreetCarSpeedZero extends GraphBuilderOSMAnnotation {

    private static final long serialVersionUID = 6872784791854835184L;

    public static final String FMT = "Way %s has car speed zero";
    public static final String HTMLFMT = "Way <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a> has car speed zero";
    public static final String DESCRIPTION = "Way has car speed zero";
    public static final String WHY_PROBLEM = "OpenTripPlanner won't use streets with zero speed for routing";
    public static final String HOW_TO_FIX = "Depending on the real world situation, the 'maxspeed' or 'highway' tag possibly should be added/fixed. Or these tags are misspelled an need to be corrected. In rare cases, e.g. for new highway types, OpenTripPlanner needs to be extended.";

    public StreetCarSpeedZero(long osmId) {
        super(GraphBuilderOSMAnnotation.OsmType.WAY, osmId);
    }

    @Override
    public String getHTMLMessage() {
        if (osmId > 0) {
            return String.format(HTMLFMT, osmId, osmId);
            // If way is lower then 0 it means it is temporary ID and so useless
            // to link to OSM
        } else {
            return getMessage();
        }
    }

    @Override
    public String getMessage() {
        return String.format(FMT, osmId);
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
}
