package org.opentripplanner.updater.car_park;

import org.junit.Test;

import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ParkApiDataSourceTest {

    @Test
    public void testParser() {
        ParkApiDataSource source = new ParkApiDataSource();
        source.setUrl("file:src/test/resources/park_api.json");
        assertTrue(source.update());
        var carParks = source.getCarParks();

        assertEquals(19, carParks.size());

        carParks.forEach(lot -> assertNotNull(lot.geometry));

        var first = carParks.get(0);
        assertEquals(Integer.MAX_VALUE, first.spacesAvailable);
        assertEquals(Integer.MAX_VALUE, first.maxCapacity);

        var epsilon = 0.02;
        assertEquals(8.865461, first.x, epsilon);
        assertEquals(48.596319, first.y, epsilon);

        var urls = carParks.stream().map(p -> p.url).collect(Collectors.toList());
        assertEquals(urls.get(0), "https://www.herrenberg.de/de/Stadtleben/Erlebnis-Herrenberg/Service/Parkplaetze/Parkplatz?view=publish&item=parkingLocation&id=1016");
        assertNull(urls.get(urls.size() - 1));
    }

}