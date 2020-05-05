package org.opentripplanner.routing.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.*;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphPathToGeoJson {

    static String toGeoJson(GraphPath graphPath) {
        var features = graphPath.states.stream()
                .map(state -> {
                    var maybeBackEdge = Optional.ofNullable(state.getBackEdge());
                    var lineGeometry = backEdgeToLineString(state, maybeBackEdge);
                    var point = vertexToPoint(state);

                    lineGeometry.add(point);
                    return lineGeometry;
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());

        FeatureCollection featureCollection = new FeatureCollection();
        featureCollection.addAll(features);
        try {
            return new ObjectMapper().writeValueAsString(featureCollection);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Feature vertexToPoint(State state) {
        var vertex = state.getVertex();
        var point = new Point(vertex.getLon(), vertex.getLat());
        var feature = new Feature();
        feature.setGeometry(point);
        feature.setProperty("accumulatedWeight", state.weight);
        feature.setProperty("label", vertex.getLabel());
        feature.setProperty("class", vertex.getClass().getSimpleName());
        if(vertex instanceof IntersectionVertex) {
            var intersection = (IntersectionVertex) vertex;
            feature.setProperty("trafficLight", String.valueOf(intersection.trafficLight));
            feature.setProperty("freeFlowing", String.valueOf(intersection.inferredFreeFlowing()));
        }
        return feature;
    }

    private static List<Feature> backEdgeToLineString(State state, Optional<Edge> maybeBackEdge) {
        return maybeBackEdge
                .flatMap(backEdge -> {
                    return toLineString(backEdge)
                            .map(ls -> {
                                var f = new Feature();
                                f.setGeometry(ls);

                                var turnCost = calculateTurnCost(state, (StreetEdge) backEdge);

                                f.setProperty("name", backEdge.getName());
                                f.setProperty("weight", String.valueOf(state.getWeightDelta()));
                                f.setProperty("class", backEdge.getClass().getSimpleName());
                                f.setProperty("turnCost", turnCost);
                                f.setProperty("stroke-width", 5);
                                if(backEdge instanceof StreetEdge) {
                                    var streetEdge = (StreetEdge) backEdge;
                                    f.setProperty("bicycleSafetyFactor", streetEdge.getBicycleSafetyFactor());
                                }
                                return f;
                            });
                })
                .stream()
                .collect(Collectors.toList());
    }

    private static double calculateTurnCost(State state, StreetEdge toEdge) {
        var maybeFromEdge = Optional
                .ofNullable(state.backState.getBackEdge())
                .filter(e -> e instanceof StreetEdge)
                .map(e -> (StreetEdge) e);

        var maybeIntersection = Optional
                .ofNullable(state.backState.getVertex())
                .filter(v -> v instanceof IntersectionVertex)
                .map(v -> (IntersectionVertex) v);

        if(maybeFromEdge.isPresent() && maybeIntersection.isPresent()) {
            var fromSpeed = maybeFromEdge.get().calculateSpeed(state.getOptions(), state.getBackMode());
            var toSpeed = toEdge.calculateSpeed(state.getOptions(), state.getBackState().getBackMode());

            return state.getOptions().getIntersectionTraversalCostModel()
                    .computeTraversalCost(
                            maybeIntersection.get(),
                            maybeFromEdge.get(),
                            toEdge,
                            state.getBackMode(),
                            state.getOptions(),
                            (float) fromSpeed,
                            (float) toSpeed);
        } else return 0;
    }

    private static Optional<LineString> toLineString(Edge edge) {
        return Optional.ofNullable(edge.getGeometry())
                .map(org.locationtech.jts.geom.LineString::getCoordinates)
                .map(Arrays::stream)
                .map(coords -> {
                    return coords.map(c -> new LngLatAlt(c.x, c.y))
                            .toArray(LngLatAlt[]::new);
                })
                .map(org.geojson.LineString::new);
    }
}
