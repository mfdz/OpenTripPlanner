package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;

import java.util.concurrent.atomic.AtomicLong;

public class CountingTraverseVisitor implements TraverseVisitor {

    private AtomicLong visitedVertices = new AtomicLong();
    @Override
    public void visitEdge(Edge edge, State state) { }

    @Override
    public void visitVertex(State state) {
        visitedVertices.incrementAndGet();
    }

    @Override
    public void visitEnqueue(State state) { }

    @Override
    public long visitedVertices() {
        return visitedVertices.longValue();
    }
}
