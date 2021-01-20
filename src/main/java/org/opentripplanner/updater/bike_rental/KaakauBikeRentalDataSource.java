package org.opentripplanner.updater.bike_rental;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import com.google.common.base.Strings;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Kaakau (Kuopio, Finland) bike rental data source.
 * url: https://kaupunkipyorat.kuopio.fi/tkhs-export-map.html?format=xml
 */
public class KaakauBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private String networkName;

    public KaakauBikeRentalDataSource(String networkName) {
        super("result");
        this.networkName = Strings.isNullOrEmpty(networkName) ? "kaakau" : networkName;
    }

    public BikeRentalStation makeStation(JsonNode node) {
        BikeRentalStation station = new BikeRentalStation();
        station.networks = new HashSet<>(Collections.singleton(this.networkName));
	station.id = node.path("name").asText();
        station.name = new NonLocalizedString(station.id);
        station.state = "Station on";

        try {
            station.x = Double.parseDouble(node.path("coordinates").asText().split(",")[0].trim());
            station.y = Double.parseDouble(node.path("coordinates").asText().split(",")[1].trim());
        } catch (NumberFormatException e) {
            // E.g. coordinates is empty
            return null;
        }
        return station;
    }
}
