package org.opentripplanner.updater.bike_rental;

import org.junit.Test;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;

import static org.junit.Assert.*;
import static org.opentripplanner.updater.bike_rental.BikeRentalUpdater.parseRentalType;

public class BikeRentalUpdaterTest {

    String name = "test network";

    @Test
    public void shouldParseRentalTypeValues() {
        var type = parseRentalType("station-based", name);
        assertEquals(type, BikeRentalStationService.RentalType.STATION_BASED);

        var type2 = parseRentalType(null, name);
        assertEquals(type2, BikeRentalStationService.RentalType.STATION_BASED);

        var type3 = parseRentalType("", name);
        assertEquals(type3, BikeRentalStationService.RentalType.STATION_BASED);

        var type4 = parseRentalType("free-floating", name);
        assertEquals(type4, BikeRentalStationService.RentalType.FREE_FLOATING);

        var type5 = parseRentalType("station-based-with-temporary-drop-off", name);
        assertEquals(type5, BikeRentalStationService.RentalType.STATION_BASED_WITH_TEMPORARY_DROP_OFF);

        var type6 = parseRentalType("hurz", name);
        assertEquals(type6, BikeRentalStationService.RentalType.STATION_BASED);
    }

}