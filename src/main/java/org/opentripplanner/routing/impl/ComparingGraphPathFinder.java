package org.opentripplanner.routing.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This variant of the GraphPathFinder "fans out" a routing request into potentially multiple ones and compares the
 * sets of results. It can then discard nonsensical or inferiour results.
 * <p>
 * This is useful for the mode PARK_RIDE because this mode forces you to drive to a P&R station and take public transport
 * even if it would be _a lot_ faster to just drive to the destination all the way.
 */
public class ComparingGraphPathFinder extends GraphPathFinder {

    private static final Logger LOG = LoggerFactory.getLogger(ComparingGraphPathFinder.class);

    public ComparingGraphPathFinder(Router router) {
        super(router);
    }

    @Override
    public List<GraphPath> graphPathFinderEntryPoint(RoutingRequest options) {

        List<GraphPath> results;
        if (options.parkAndRide) {
            LOG.debug("Detected a P&R routing request. Will execute two requests to also get car-only routes.");

            // in order to avoid race conditions we have to clone beforehand
            RoutingRequest clone = options.clone();
            // the normal P&R
            List<GraphPath> parkAndRide;

            try {
                parkAndRide = super.graphPathFinderEntryPoint(clone);
            } catch (PathNotFoundException e) {
                LOG.debug("Could not find park & ride trips.", e);
                parkAndRide = Lists.newArrayList();
            }
            // car-only
            List<GraphPath> carOnly = runCarOnlyRequest(options);

            results = filterOut(parkAndRide, carOnly);

            if(results == null || results.isEmpty()) {
                throw new PathNotFoundException();
            }

        } else {
            results = super.graphPathFinderEntryPoint(options);
        }

        return results;
    }


    private List<GraphPath> runCarOnlyRequest(RoutingRequest clone) {
        clone.parkAndRide = false;
        clone.setMode(TraverseMode.CAR);

        List<GraphPath> results;
        try {
             results = new GraphPathFinder(router).graphPathFinderEntryPoint(clone);
        } catch(PathNotFoundException e)  {
            LOG.debug("Could not find car-only trip.", e);
            results = Lists.newArrayList();
        }
        finally {
            clone.cleanup();
        }

        return results;
    }

    /**
     * We filter out unsuitable P+R routes.
     *
     * Right now "unsuitable" means that driving to the P+R is more than 50% of the distance of
     * driving all the way to the destination.
     */
    private List<GraphPath> filterOut(List<GraphPath> parkAndRide, List<GraphPath> carOnly) {
        if (carOnly.isEmpty()) {
            return parkAndRide;
        } else {
            double halfDistanceOfCarOnly = carOnly.get(0).streetMeters() / 2;
            List<GraphPath> onlyFastOnes = parkAndRide.stream().filter(graphPath -> graphPath.streetMeters() < halfDistanceOfCarOnly).collect(Collectors.toList());
            LOG.debug("Found only {} routes that had less than half the amount of driving than the car only one. Their driving distances are {}", onlyFastOnes.size(), onlyFastOnes.stream().map(g -> g.streetMeters()));
            LOG.debug("Car-only driving distance {}", carOnly.get(0).streetMeters());
            if (haveGraphsBeenFilteredOut(parkAndRide, onlyFastOnes)) {
                return Lists.newArrayList(Iterables.concat(onlyFastOnes, carOnly));
            } else return parkAndRide;
        }
    }

    private boolean haveGraphsBeenFilteredOut(List<GraphPath> parkAndRide, List<GraphPath> onlyFastOnes) {
        return onlyFastOnes.size() < parkAndRide.size();
    }
}
