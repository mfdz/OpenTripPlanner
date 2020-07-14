package org.opentripplanner.routing.car_park;

import org.junit.Test;

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
}