package org.opentripplanner.graph_builder.annotation;

import org.locationtech.jts.geom.Coordinate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public abstract class GraphBuilderOSMAnnotation extends GraphBuilderAnnotation {

    public enum OsmType {
        NODE, WAY, RELATION
    };

    private static final long serialVersionUID = 5962959074354225171L;

    private static final Map<String, Object> NO_ADDITIONAL_INFO = Collections
            .unmodifiableMap(new HashMap<>());

    protected final long osmId;
    protected final OsmType osmType;
    private double lon;
    private double lat;

    public GraphBuilderOSMAnnotation(OsmType osmType, long osmId) {
        this.osmType = osmType;
        this.osmId = osmId;
    }

    public GraphBuilderOSMAnnotation(OsmType osmType, long osmId, double lon, double lat) {
        this(osmType, osmId);
        this.lon = lon;
        this.lat = lat;
    }

    public abstract String getDescription();

    public abstract String getWhyProblem();

    public abstract String getHowToFix();

    public long getOsmId() {
        return osmId;
    }

    public OsmType getOsmType() {
        return osmType;
    }

    public void setCoordinates(Coordinate coords) {
        lon = coords.x;
        lat = coords.y;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public Map<String, Object> getAdditionalInfo() {
        return NO_ADDITIONAL_INFO;
    }
}
