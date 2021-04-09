package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Vertex;

import java.io.Serializable;
import java.util.Objects;

/**
 * A class that determines when one search branch prunes another at the same Vertex, and ultimately which solutions
 * are retained. In the general case, one branch does not necessarily win out over the other, i.e. multiple states can
 * coexist at a single Vertex.
 * 
 * Even functions where one state always wins (least weight, fastest travel time) are applied within a multi-state
 * shortest path tree because bike rental, car or bike parking, and turn restrictions all require multiple incomparable 
 * states at the same vertex. These need the graph to be "replicated" into separate layers, which is achieved by 
 * applying the main dominance logic (lowest weight, lowest cost, Pareto) conditionally, only when the two states
 * have identical bike/car/turn direction status.
 * 
 * Dominance functions are serializable so that routing requests may passed between machines in different JVMs, for instance
 * in OTPA Cluster.
 */
public abstract class DominanceFunction implements Serializable {
    private static final long serialVersionUID = 1;

    /**
     * The OTP algorithm tries hard to never visit the same node twice. This is generally a good idea because it avoids
     * useless loops in the traversal leading to way faster processing time.
     *
     * However there is are certain rare pathological cases where through a series of turn restrictions and roadworks
     * you absolutely must visit a vertex twice if you want to produce a result. One example would be a route like this:
     *   https://tinyurl.com/ycqux93g (Note: At the time of writing this Hindenburgstr. is closed due to roadworks.)
     *
     * Therefore, if we are close to the start or the end of a route we allow this.
     *
     * The following variable determines how close you have to be to the start or the end point for it to be
     * considered "close enough for a loop".
     */
    private static final float MAX_LOOP_EUCLIDIAN_DISTANCE = 0.005f;

    /**
     * Return true if the first state "defeats" the second state or at least ties with it in terms of suitability. 
     * In the case that they are tied, we still want to return true so that an existing state will kick out a new one.
     * Provide this custom logic in subclasses. You would think this could be static, but in Java for some reason 
     * calling a static function will call the one on the declared type, not the runtime instance type. 
     */
    protected abstract boolean betterOrEqual(State a, State b);

    /**
     * For bike rental, parking, and approaching turn-restricted intersections states are incomparable:
     * they exist on separate planes. The core state dominance logic is wrapped in this public function and only
     * applied when the two states have all these variables in common (are on the same plane).
     */
    public boolean betterOrEqualAndComparable(State a, State b) {

        // States before boarding transit and after riding transit are incomparable.
        // This allows returning transit options even when walking to the destination is the optimal strategy.
        if (a.isEverBoarded() != b.isEverBoarded()) {
            return false;
        }

        // The result of a SimpleTransfer must not block alighting normally from transit. States that are results of
        // SimpleTransfers are incomparable with states that are not the result of SimpleTransfers.
        if ((a.backEdge instanceof SimpleTransfer) != (b.backEdge instanceof SimpleTransfer)) {
            return false;
        }

        // A TimedTransferEdge cannot be trusted to dominate another state, since its resulting state can be invalidated
        // when checking the specificity of the transfer
        if ((a.backEdge instanceof TimedTransferEdge) || (b.backEdge instanceof TimedTransferEdge)) {
            return false;
        }

        // If already boarded both via TimedTransfer the state should dominate boarding not via TimedTransfer. It is
        // important that this check is done after boarding, as the transfer may fail its specificity check
        if (a.backEdge instanceof TransitBoardAlight && b.backEdge instanceof TransitBoardAlight) {
            if (a.getBackState().backEdge instanceof TimedTransferEdge) {
                return true;
            }
        }

        // Does one state represent riding a rented bike and the other represent walking before/after rental?
        if (a.isBikeRenting() != b.isBikeRenting()) {
            return false;
        }

        // In case of bike renting, different networks (ie incompatible bikes) are not comparable
        if (a.isBikeRenting()) {
            if (!Objects.equals(a.getBikeRentalNetworks(), b.getBikeRentalNetworks()))
                return false;
        }

        // Does one state represent driving a car and the other represent walking after the car was parked?
        if (a.isCarParked() != b.isCarParked()) {
            return false;
        }

        // Does one state represent riding a bike and the other represent walking after the bike was parked?
        if (a.isBikeParked() != b.isBikeParked()) {
            return false;
        }

        // Are the two states arriving at a vertex from two different directions where turn restrictions apply?
        if (a.backEdge != b.getBackEdge() && (a.backEdge instanceof StreetEdge)) {
            if (backEdgeHasTurnRestrictions(a)
                /**
                 * {@link DominanceFunction.MAX_METERS_ROUTE_LOOPS}
                 */
                || a.getBackMode() != TraverseMode.WALK && isCloseToStartOrEnd(a.getVertex(), a.getOptions())) {
                return false;
            }
        }

        // These two states are comparable (they are on the same "plane" or "copy" of the graph).
        return betterOrEqual(a, b);
        
    }

    private static boolean backEdgeHasTurnRestrictions(State a) {
        return ! a.getOptions().getRoutingContext().graph.getTurnRestrictions(a.backEdge).isEmpty();
    }

    private static boolean isCloseToStartOrEnd(Vertex v, RoutingRequest req) {
        var from = req.from.getCoordinate();
        var to = req.to.getCoordinate();
        var currentLocation = v.getCoordinate();
        var allNonNull = from != null && to != null && currentLocation != null;
        if(allNonNull) {
            return currentLocation.distance(req.from.getCoordinate()) < MAX_LOOP_EUCLIDIAN_DISTANCE
                    ||
                    currentLocation.distance(req.to.getCoordinate()) < MAX_LOOP_EUCLIDIAN_DISTANCE;
        }
        else return false;
    }

    /**
     * Create a new shortest path tree using this function, considering whether it allows co-dominant States.
     * MultiShortestPathTree is the general case -- it will work with both single- and multi-state functions.
     */
     public ShortestPathTree getNewShortestPathTree(RoutingRequest routingRequest) {
        return new ShortestPathTree(routingRequest, this);
     }

    public static class MinimumWeight extends DominanceFunction {
        /** Return true if the first state has lower weight than the second state. */
        @Override
        public boolean betterOrEqual (State a, State b) { return a.weight <= b.weight; }
    }

    /**
     * This approach is more coherent in Analyst when we are extracting travel times from the optimal
     * paths. It also leads to less branching and faster response times when building large shortest path trees.
     */
    public static class EarliestArrival extends DominanceFunction {
        /** Return true if the first state has lower elapsed time than the second state. */
        @Override
        public boolean betterOrEqual (State a, State b) { return a.getElapsedTimeSeconds() <= b.getElapsedTimeSeconds(); }
    }
    
    /**
     * A dominance function that prefers the least walking. This should only be used with walk-only searches because
     * it does not include any functions of time, and once transit is boarded walk distance is constant.
     * 
     * It is used when building stop tree caches for egress from transit stops.
     */
    public static class LeastWalk extends DominanceFunction {

        @Override
        protected boolean betterOrEqual(State a, State b) {
            return a.getWalkDistance() <= b.getWalkDistance(); 
        }

    }

    /** In this implementation the relation is not symmetric. There are sets of mutually co-dominant states. */
    public static class Pareto extends DominanceFunction {

        @Override
        public boolean betterOrEqual (State a, State b) {

            // The key problem in pareto-dominance in OTP is that the elements of the state vector are not orthogonal.
            // When walk distance increases, weight increases. When time increases weight increases.
            // It's easy to get big groups of very similar states that don't represent significantly different outcomes.
            // Our solution to this is to give existing states some slack to dominate new states more easily.
            
            final double EPSILON = 1e-4;
            return (a.getElapsedTimeSeconds() <= (b.getElapsedTimeSeconds() + EPSILON)
                    && a.getWeight() <= (b.getWeight() + EPSILON));
            
        }

    }

}
