package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.I18NString;

/**
 * A vertex for a park and ride area. Connected to streets by ParkAndRideLinkEdge. Transition for
 * parking the car is handled by ParkAndRideEdge.
 *
 */
public class ParkAndRideVertex extends Vertex {

    private static final long serialVersionUID = 2L;

    private String id;

    private CarPark carPark;

    public ParkAndRideVertex(
            Graph g,
            String label,
            String id,
            double x,
            double y,
            I18NString name
    ) {
        super(g, label, x, y, name);
        setId(id);
    }

    public ParkAndRideVertex(Graph graph, CarPark carPark) {
        super(graph, carPark.id, carPark.x, carPark.y, carPark.name);
        this.carPark = carPark;
        setId(carPark.id);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CarPark getCarPark() {
        return carPark;
    }

    public void updateCapacity(int maxCapacity, int spacesAvailable) {
        carPark.maxCapacity = maxCapacity;
        carPark.spacesAvailable = spacesAvailable;
    }

    public boolean hasFewSpacesAvailable() {
        return carPark.hasFewSpacesAvailable();
    }

    public boolean hasOnlyDisabledSpaces() {
        return carPark != null && carPark.hasOnlyDisabledSpaces();
    }
}
