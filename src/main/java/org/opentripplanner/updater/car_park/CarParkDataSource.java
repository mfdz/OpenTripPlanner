package org.opentripplanner.updater.car_park;

import java.util.List;
import org.opentripplanner.routing.car_park.CarPark;


/**
 * A (static or dynamic) source of car-parks for park and ride.
 */
public interface CarParkDataSource {

    /**
     * Update the data from the source; returns true if there might have been changes
     */
    boolean update();

    List<CarPark> getCarParks();

}
