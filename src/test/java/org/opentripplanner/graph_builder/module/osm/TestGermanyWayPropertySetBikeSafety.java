package org.opentripplanner.graph_builder.module.osm;

import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * Test the bike safety ratings for GermanyWayPropertySet.
 *
 * @author hbruch
 */
public class TestGermanyWayPropertySetBikeSafety extends TestCase {
    
    /**
     * Test that bike safety factors are calculated accurately
     */
    @Test
    public void testBikeSafety () {
        WayPropertySet wps = new WayPropertySet();
        WayPropertySetSource source = new GermanyWayPropertySetSource();
        source.populateProperties(wps);

        OSMWithTags way;

        float epsilon = 0.01f;

        // way 361961158
        way = new OSMWithTags();
        way.addTag("bicycle", "yes");
        way.addTag("foot", "designated");
        way.addTag("footway", "sidewalk");
        way.addTag("highway", "footway");
        way.addTag("lit", "yes");
        way.addTag("oneway", "no");
        way.addTag("traffic_sign", "DE:239,1022-10");
        assertEquals(1.18, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);

        way = new OSMWithTags();
        way.addTag("cycleway", "opposite");
        way.addTag("highway", "residential");
        way.addTag("lit", "yes");
        way.addTag("maxspeed", "30");
        way.addTag("name", "Freibadstraße");
        way.addTag("oneway", "yes");
        way.addTag("oneway:bicycle", "no");
        way.addTag("parking:lane:left", "parallel");
        way.addTag("parking:lane:right", "no_parking");
        way.addTag("sidewalk", "both");
        way.addTag("source:maxspeed", "DE:zone:30");
        way.addTag("surface", "asphalt");
        way.addTag("width", "6.5");
        way.addTag("zone:traffic", "DE:urban");
        assertEquals(0.891, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);

        // way332589799 (Radschnellweg BW1)
        way = new OSMWithTags();
        way.addTag("bicycle", "designated");
        way.addTag("class:bicycle", "2");
        way.addTag("class:bicycle:roadcycling", "1");
        way.addTag("highway", "track");
        way.addTag("horse", "forestry");
        way.addTag("lcn", "yes");
        way.addTag("lit", "yes");
        way.addTag("maxspeed", "30");
        way.addTag("motor_vehicle", "forestry");
        way.addTag("name", "Römerstraße");
        way.addTag("smoothness", "excellent");
        way.addTag("source:maxspeed", "sign");
        way.addTag("surface", "asphalt");
        way.addTag("tracktype", "grade1");
        assertEquals(0.931, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);

        way = new OSMWithTags();
        way.addTag("highway", "track");
        way.addTag("motor_vehicle", "agricultural");
        way.addTag("surface", "asphalt");
        way.addTag("tracktype", "grade1");
        way.addTag("traffic_sign", "DE:260,1026-36");
        way.addTag("width", "2.5");
        assertEquals(1.3, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);
    }
}
