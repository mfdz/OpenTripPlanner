package org.opentripplanner.graph_builder.annotation;

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.routing.graph.Vertex;

public class GraphConnectivity extends GraphBuilderOSMAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Removed/depedestrianized disconnected subgraph containing vertex '%s' at (%f, %f), with %d edges";
    public static final String HTMLFMT = "Removed/depedestrianized disconnected subgraph containing vertex <a href='http://www.openstreetmap.org/node/%s'>'%s'</a>, with %d edges";
    public static final String DESCRIPTION = "Disconnected subgraph containing this vertex.";
    public static final String WHY_PROBLEM = "The subgraph containing this representative vertex was removed from OTP graph and is not reachable."+
    " This may happen especially at the borders of the imported area.";
    public static final String HOW_TO_FIX = "Especially if inside the imported area, check if the subgraph should be connected and if yes, fix OSM accordingly.";

    final Vertex vertex;
    final int size;
    
    public GraphConnectivity(Vertex vertex, int size){
        super(GraphBuilderOSMAnnotation.OsmType.NODE, asOsmId(vertex), vertex.getLon(), vertex.getLat());
    	this.vertex = vertex;
    	this.size = size;
    }

    private static long asOsmId(Vertex vertex) {
        try {
            String label = vertex.getLabel();
            if (label.startsWith("osm:")) {
                String osmNodeId = label.split(":")[2];
                return Long.parseLong(osmNodeId);
            } else {
                return 0;            
            }            
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String getMessage() {
        return String.format(FMT, vertex, vertex.getCoordinate().x, vertex.getCoordinate().y, size);
    }

    @Override
    public String getHTMLMessage() {
        if (osmId != 0L) {
            return String.format(HTMLFMT, osmId, osmId, size);
        } else {
            return this.getMessage();
        }
    }

    @Override
    public Vertex getReferencedVertex() {
        return vertex;
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
        map.put("Subgraph size", Integer.toString(size));
        return map;
    }

}
