package org.opentripplanner.graph_builder.annotation;

public class ParkAndRideUnlinked extends GraphBuilderOSMAnnotation {

    private static final long serialVersionUID = -6989148664166366954L;

    public static final String FMT = "Park and ride '%s' (%d) not linked to any streets; it will not be usable.";
    public static final String HTMLFMT = "Park and ride <a href='http://www.openstreetmap.org/way/%d'>'%s' (%d)</a> not linked to any streets; it will not be usable.";
    public static final String DESCRIPTION = "Park and ride not linked to any streets; it will not be usable.";
    public static final String WHY_PROBLEM = "OpenTripPlanner requires parking to be walk "
            + "accessible outwards, and car accessible inwards "
            + "(See https://github.com/opentripplanner/OpenTripPlanner/wiki/Park-and-Ride). ";
    public static final String HOW_TO_FIX = " If the P+R parking is alongside roads, there is "
            + "ongoing dispute wether it is correct to have 2D parkings connected to 1D streets "
            + "(see https://github.com/opentripplanner/OpenTripPlanner/issues/2168). "
            + "OpenTripPlanner decided against creating entrances automagically and requires "
            + "the areas to be extended so they are connected to the street."
            + "In other cases, properly tagged access ways need to be added.";

    final String name;

    public ParkAndRideUnlinked(String name, long osmId, double centerX, double centerY) {
        super(GraphBuilderOSMAnnotation.OsmType.WAY, osmId, centerX, centerY);
        this.name = name;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, name, osmId);
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, osmId, name, osmId);
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
