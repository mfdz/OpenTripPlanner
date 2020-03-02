package org.opentripplanner.routing.roadworks;

import com.google.common.collect.Sets;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RoadworksSource {

    private Set<Long> blockedWayIds;
    private Set<Integer> blockedEdgeIds = new HashSet<>();

    public RoadworksSource() {
        this.blockedWayIds = Sets.newHashSet();
    }

    public RoadworksSource(Long ...blockedWayId) {
        this.blockedWayIds = Sets.newHashSet(blockedWayId);
    }


    public boolean isBlocked(StreetEdge edge){
      return blockedWayIds.contains(edge.wayId);
    }

    public void addBlockedEdges(Collection<Edge> edges) {
        Set<Integer> ids = edges.stream().map(Edge::getId).collect(Collectors.toSet());
        blockedEdgeIds.addAll(ids);
    }
}
