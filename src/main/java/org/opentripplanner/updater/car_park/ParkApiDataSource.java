package org.opentripplanner.updater.car_park;

import com.fasterxml.jackson.databind.JsonNode;
import org.geotools.xml.xsi.XSISimpleTypes;
import org.locationtech.jts.geom.*;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load car parks from a ParkAPI (https://github.com/offenesdresden/ParkAPI) JSON file.
 */
public class ParkApiDataSource extends GenericJsonCarParkDataSource{

    private static final Logger LOG = LoggerFactory.getLogger(ParkApiDataSource.class);

    private String url;
    private GeometryFactory gf = new GeometryFactory();

    public ParkApiDataSource() {
        super("lots");
    }

    @Override
    public CarPark makeCarPark(JsonNode node) {

        var carPark = new CarPark();
        carPark.name = new NonLocalizedString(node.path("name").asText());

        var coords = node.path("coords");
        carPark.x = coords.path("lng").asDouble();
        carPark.y = coords.path("lat").asDouble();

        var fallbackId = url + ":" + carPark.x + "," + carPark.y;
        carPark.id = node.path("id").asText(fallbackId);

        carPark.spacesAvailable = node.path("free").asInt(Integer.MAX_VALUE);
        carPark.maxCapacity = node.path("total").asInt(Integer.MAX_VALUE);
        carPark.realTimeData = true;

        carPark.geometry = gf.createPoint(new Coordinate(carPark.x, carPark.y));

        carPark.openingHours = node.path("opening_hours").asText();
        carPark.url = node.path("url").asText(null);

        return carPark;
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode jsonNode) {
        super.configure(graph, jsonNode);
        String url = jsonNode.path("url").asText(); // path() returns MissingNode not null.
        if (url != null && !url.isEmpty()) {
            this.url = url;
        } else {
            LOG.error("Could read URL for retrieving ParkAPI data.");
        }
    }

    @Override
    public String toString() {
        return "ParkApiDataSource{" +
                "url='" + url + '\'' +
                '}';
    }
}

