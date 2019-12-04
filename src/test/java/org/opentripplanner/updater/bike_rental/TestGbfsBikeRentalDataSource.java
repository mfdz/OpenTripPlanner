/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.bike_rental;

import junit.framework.TestCase;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.util.List;

public class TestGbfsBikeRentalDataSource extends TestCase {

        public void testGetStations() {

                GbfsBikeRentalDataSource gbfsBikeRentalDataSource = new GbfsBikeRentalDataSource(
                        "LeihLeeze");

                gbfsBikeRentalDataSource.setBaseUrl("file:src/test/resources/bike/gbfs-leihleeze");
                assertTrue(gbfsBikeRentalDataSource.update());
                List<BikeRentalStation> rentalStations = gbfsBikeRentalDataSource.getStations();
                assertEquals(1, rentalStations.size());
                for (BikeRentalStation rentalStation : rentalStations) {
                        System.out.println(rentalStation);
                }
                BikeRentalStation fahrradFunke = rentalStations.get(0);
                assertEquals("Fahrrad Funke", fahrradFunke.name.toString());
                assertEquals("de_leihleeze_station_42", fahrradFunke.id);
                assertEquals(6.855784, fahrradFunke.x);
                assertEquals(51.909579, fahrradFunke.y);
                assertEquals(1, fahrradFunke.spacesAvailable);
                assertEquals(1, fahrradFunke.bikesAvailable);
                assertEquals("https://buchen.leihleeze.de/verleiher/42/ausleihen/neu", fahrradFunke.rentalUriWeb);
        }
}