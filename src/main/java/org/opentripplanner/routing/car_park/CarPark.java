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

package org.opentripplanner.routing.car_park;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Strings;
import io.leonard.OpeningHoursEvaluator;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.util.I18NString;

import javax.xml.bind.annotation.XmlAttribute;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class CarPark implements Serializable {
    private static final long serialVersionUID = 8311460609708089384L;

    /**
     * Unique ID of the car park. Creator should ensure the ID is unique server-wide (prefix by a
     * source ID if there are several sources)
     */
    @XmlAttribute
    @JsonSerialize
    public String id;

    @XmlAttribute
    @JsonSerialize
    public I18NString name;

    /** Note: x = Longitude, y = Latitude */
    @XmlAttribute
    @JsonSerialize
    public double x, y;

    @XmlAttribute
    @JsonSerialize
    public int spacesAvailable = Integer.MAX_VALUE;

    @XmlAttribute
    @JsonSerialize
    public int maxCapacity = Integer.MAX_VALUE;

    @XmlAttribute
    @JsonSerialize
    public String openingHours;

    @XmlAttribute
    @JsonSerialize
    public String url;

    /**
     * Whether this parking has space available information updated in real-time. If no real-time
     * data, users should take spacesAvailable with a pinch of salt, as they are a crude estimate.
     */
    @XmlAttribute
    @JsonSerialize
    public boolean realTimeData = true;

    public Geometry geometry;

    private List<Rule> parsedOpeningHours = null;

    public boolean equals(Object o) {
        if (!(o instanceof CarPark)) {
            return false;
        }
        CarPark other = (CarPark) o;
        return other.id.equals(id);
    }

    public int hashCode() {
        return id.hashCode() + 1;
    }

    public String toString () {
        return String.format(Locale.US, "Car park %s at %.6f, %.6f", name, y, x);
    }

    public boolean hasFewSpacesAvailable() {
        return hasFewSpacesAvailable(spacesAvailable, maxCapacity);
    }

    public boolean isClosedAt(LocalDateTime time) {
        parseOpeningHours();
        if(parsedOpeningHours == null) return false;
        else return !OpeningHoursEvaluator.isOpenAt(time, parsedOpeningHours);
    }

    private void parseOpeningHours() {
        if(parsedOpeningHours == null && ! Strings.isNullOrEmpty(openingHours)) {
            var parser = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes()));
            try {
                parsedOpeningHours = parser.rules(true);
            } catch (OpeningHoursParseException e) {
                parsedOpeningHours = Collections.emptyList();
            }
        }
    }

    public LocalDateTime opensNext(LocalDateTime time) {
        parseOpeningHours();
        if(parsedOpeningHours == null) {
            return LocalDateTime.MIN;
        }
        else return OpeningHoursEvaluator.isOpenNext(time, parsedOpeningHours).orElse(LocalDateTime.MIN);
    }

    public static boolean hasFewSpacesAvailable(int spacesAvailable, int maxCapacity) {
        // special handling if it is a very small car park
        if(maxCapacity < 10) {
            return spacesAvailable <= 1;
        // special handling if it is a large one: 20 parking spaces is enough
        } else if(maxCapacity > 200){
            return spacesAvailable < 20;
        // for everything in the middle the cutoff is 10 percent
        } else {
            var percentFree = ((float) spacesAvailable / maxCapacity);
            return !(Double.isNaN(percentFree)) && percentFree <= 0.1f;
        }
    }

}
