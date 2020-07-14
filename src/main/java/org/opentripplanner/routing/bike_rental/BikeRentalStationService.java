package org.opentripplanner.routing.bike_rental;

import java.io.Serializable;
import java.util.*;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;

public class BikeRentalStationService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    private Set<BikeRentalStation> bikeRentalStations = new HashSet<>();

    private Set<BikePark> bikeParks = new HashSet<>();

    private Map<String, RentalType> networkRentalTypes = Maps.newConcurrentMap();

    public Collection<BikeRentalStation> getBikeRentalStations() {
        return bikeRentalStations;
    }

    public void addBikeRentalStation(BikeRentalStation bikeRentalStation) {
        // Remove old reference first, as adding will be a no-op if already present
        bikeRentalStations.remove(bikeRentalStation);
        bikeRentalStations.add(bikeRentalStation);
    }

    public void removeBikeRentalStation(BikeRentalStation bikeRentalStation) {
        bikeRentalStations.remove(bikeRentalStation);
    }

    public Collection<BikePark> getBikeParks() {
        return bikeParks;
    }

    public void addBikePark(BikePark bikePark) {
        // Remove old reference first, as adding will be a no-op if already present
        bikeParks.remove(bikePark);
        bikeParks.add(bikePark);
    }

    public void removeBikePark(BikePark bikePark) {
        bikeParks.remove(bikePark);
    }

    public void setNetworkType(String network, RentalType type) {
        networkRentalTypes.put(network, type);
    }

    public boolean networksAllowsFreeFloatingDropOff(Set<String> networks) {
        return networks.stream().anyMatch(n -> {
            var type = networkRentalTypes.getOrDefault(n, RentalType.STATION_BASED);
            return type == RentalType.FREE_FLOATING || type == RentalType.STATION_BASED_WITH_TEMPORARY_DROP_OFF;
        });
    }

    public boolean shouldAddFreeFloatingAlertForNetworks(Set<String> networks) {
        return networks.stream().anyMatch(n -> networkRentalTypes.get(n) == RentalType.STATION_BASED_WITH_TEMPORARY_DROP_OFF);
    }

    public enum RentalType {
        // bikes can only be dropped off at designated docks or areas
        STATION_BASED("station-based"),
        // bikes can be dropped off anywhere (note: business areas are not implemented)
        FREE_FLOATING("free-floating"),
        // bikes can be dropped off anywhere but response will contain an alert that the rental has not ended
        // and that extra charges may be levied
        STATION_BASED_WITH_TEMPORARY_DROP_OFF("station-based-with-temporary-drop-off");

        public final String name;

        RentalType(String name) {
            this.name = name;
        }

        public static Optional<RentalType> fromString(String name) {
            var cleaned = Strings.nullToEmpty(name).trim().toLowerCase();
            return Arrays.stream(values()).filter(t -> t.name.equals(cleaned)).findFirst();
        }
    }
}
