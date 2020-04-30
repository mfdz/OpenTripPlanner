package org.opentripplanner.routing.core;

import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.ComparingGraphPathFinder;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.TestUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class FanOutGraphPathFinderTest {

    static Graph graph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_OSM);

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 04, 23, 11, 0, 0);

    private static TripPlan getTripPlan(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.modes = new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK);
        request.parkAndRide = true;

        GraphPathFinder gpf = new ComparingGraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        return  GraphPathToTripPlanConverter.generatePlan(paths, request);
    }

    @Test
    public void fanOutParkAndRide() {
        var herrenbergRaistingerStr = new GenericLocation(48.5879, 8.8537);
        var herrenbergWilhelmstr = new GenericLocation(48.59586,8.87710);

        var plan = getTripPlan(graph, herrenbergRaistingerStr, herrenbergWilhelmstr);
        // one P&R, one driving all the way
        assertThat(plan.itinerary.size(), is(2));

        var parkAndRide = plan.itinerary.get(0).legs
                .stream()
                .map(l -> l.legGeometry.getPoints())
                .collect(Collectors.toList());

        // driving to P&R car park
        assertThatPolylinesAreEqual(parkAndRide.get(0), "iypgHuf`u@y@s@o@S_@Eq@I{APo@BYeBEOKq@KuAIqB@mB?m@?G@Q@cAA}AKiCQsBE]Eg@?GC}@?e@?{@AaE?OgAZWF}@b@MA]OW]Uc@c@w@S_@q@r@w@n@_@XuA~@oAeFCBA@");
        // walking the rest
        assertThatPolylinesAreEqual(parkAndRide.get(1), "ezqgHcyau@@ABCaBgHc@aB]_B_AaEYkACa@Ao@?_@Gc@QOOMMKQMk@o@KKIKMQIKGKKMEKAI?G?INULSHONa@FUJ_@Jo@DSBQJw@DkADi@D{@D_ATsDDu@Am@Ee@AUEi@Eu@C{@YBu@NEOS}@Y}ESsCMcBUyCi@oEAE");

        var driveOnly = plan.itinerary.get(1).legs.get(0).legGeometry.getPoints();
        // second request drives all the way there
        assertThatPolylinesAreEqual(driveOnly, "iypgHuf`u@y@s@o@S_@Eq@I{APo@BYeBEOKq@KuAIqB@mB?m@?G@Q@cAA}AKiCQsBE]Eg@?GC}@?e@?{@AaE?O?M?{BDmA@oA?cAEcBGa@EOOa@Ea@K_A[}D[cD[uD]qDYcDEWg@{FkAEmAIKAiCK]U}BaA]Q}@]uAs@[SAm@Ee@AUEi@Eu@C{@YBu@NEOS}@Y}ESsCMcBUyCi@oEAE");
    }

}