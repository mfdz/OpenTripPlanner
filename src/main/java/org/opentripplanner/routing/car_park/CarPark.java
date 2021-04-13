package org.opentripplanner.routing.car_park;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Locale;
import javax.xml.bind.annotation.XmlAttribute;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.util.I18NString;


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

    /**
     * Note: x = Longitude, y = Latitude
     */
    @XmlAttribute
    @JsonSerialize
    public double x, y;

    @XmlAttribute
    @JsonSerialize
    public int spacesAvailable = Integer.MAX_VALUE;

    @XmlAttribute
    @JsonSerialize
    public int maxCapacity = Integer.MAX_VALUE;

    public int disabledSpacesAvailable = Integer.MAX_VALUE;
    public int maxDisabledCapacity = Integer.MAX_VALUE;

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

    public static boolean hasFewSpacesAvailable(int spacesAvailable, int maxCapacity) {
        // special handling if it is a very small car park
        if (maxCapacity < 10) {
            return spacesAvailable <= 1;
            // special handling if it is a large one: 20 parking spaces is enough
        }
        else if (maxCapacity > 200) {
            return spacesAvailable < 20;
            // for everything in the middle the cutoff is 10 percent
        }
        else {
            var percentFree = ((float) spacesAvailable / maxCapacity);
            return !(Double.isNaN(percentFree)) && percentFree <= 0.1f;
        }
    }

    public int hashCode() {
        return id.hashCode() + 1;
    }

    public boolean equals(Object o) {
        if (!(o instanceof CarPark)) {
            return false;
        }
        CarPark other = (CarPark) o;
        return other.id.equals(id);
    }

    public String toString() {
        return String.format(Locale.US, "Car park %s at %.6f, %.6f", name, y, x);
    }

    public boolean hasFewSpacesAvailable() {
        return hasFewSpacesAvailable(spacesAvailable, maxCapacity);
    }

    public boolean hasOnlyDisabledSpaces() {
        return maxCapacity == Integer.MAX_VALUE && maxDisabledCapacity != Integer.MAX_VALUE;
    }
}
