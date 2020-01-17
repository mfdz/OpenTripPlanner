package org.opentripplanner.routing.roadworks;

import com.google.common.collect.Sets;
import org.opentripplanner.routing.edgetype.StreetEdge;

import java.util.Set;

public class RoadworksSource {

    public RoadworksSource() {
        this.blockedWayIds = Sets.newHashSet();
    }

    public RoadworksSource(Long ...blockedWayId) {
        this.blockedWayIds = Sets.newHashSet(blockedWayId);
    }

    public RoadworksSource(Set<Long> wayIds) {
        this.blockedWayIds = wayIds;
    }

    private Set<Long> blockedWayIds;

    public boolean isBlocked(StreetEdge edge){
      return blockedWayIds.contains(edge.wayId);
    }
}
