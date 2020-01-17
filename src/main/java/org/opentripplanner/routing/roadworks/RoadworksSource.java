package org.opentripplanner.routing.roadworks;

import com.google.common.collect.Sets;
import org.opentripplanner.routing.edgetype.StreetEdge;

import java.util.*;

public class RoadworksSource {

    public RoadworksSource() {
        this.blockedEdgeIds = Sets.newHashSet();
    }

    public RoadworksSource(Set<Integer> blockedEdgeIds) {
        this.blockedEdgeIds = blockedEdgeIds;
    }

    private Set<Integer> blockedEdgeIds;

    public boolean isBlocked(StreetEdge edge){
      return blockedEdgeIds.contains(edge.getId());
    }
}
