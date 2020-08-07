package org.opentripplanner.routing.car_park;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.opentripplanner.routing.car_park.CarPark.hasFewSpacesAvailable;

public class CarParkTest {

    @Test
    public void shouldCalculateFewSpacesAvailable() {
        // easy cases
        assertFalse(hasFewSpacesAvailable(99, 100));
        assertTrue(hasFewSpacesAvailable(1, 100));

        // low max capacity
        assertFalse(hasFewSpacesAvailable(2, 11));
        assertTrue(hasFewSpacesAvailable(1, 10));
        assertTrue(hasFewSpacesAvailable(1, 9));
        assertTrue(hasFewSpacesAvailable(1, 5));
        assertTrue(hasFewSpacesAvailable(1, 1));

        // high max capacity
        assertFalse(hasFewSpacesAvailable(11, 100));
        assertTrue(hasFewSpacesAvailable(20, 200));
        assertFalse(hasFewSpacesAvailable(20, 201));
        assertTrue(hasFewSpacesAvailable(19, 201));
        assertFalse(hasFewSpacesAvailable(20, 500));
    }

    @Test
    public void shouldCalculateIfCarParkIsOpen() {
        var carPark = new CarPark();
        carPark.openingHours = "Mo-Fr 09:00-12:00";
        var before12 = LocalDateTime.parse("2020-08-07T11:24:04");
        var after12 = LocalDateTime.parse("2020-08-07T12:24:04");
        assertTrue(carPark.isOpenAt(before12));
        assertFalse(carPark.isOpenAt(after12));

        // car parks with no opening hours should be always open
        var carPark2 = new CarPark();
        assertTrue(carPark2.isOpenAt(after12));

        var carPark3 = new CarPark();
        carPark3.openingHours = "";
        assertTrue(carPark3.isOpenAt(after12));
    }
}