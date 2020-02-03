/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for German roads

 * Speed limits where adjusted to German regulation and some bike safety settings tweaked,
 * especially including tracktype's grade and preference of bicycle networks.
 *
 * @author hbruch
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class GermanyWayPropertySetSource implements WayPropertySetSource {

	@Override
	public void populateProperties(WayPropertySet props) {
        // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements

        /*
         * Automobile speeds in Germany. General speed limit is 50kph in settlements, 100kph outside settlements.
         * For motorways, there (currently still) is no limit. Nevertheless 120kph is assumed to reflect varying
         * traffic conditions
         *
         */
        props.setCarSpeed("highway=motorway", 33.33f); // = 120kph. Varies between 80 - 120 kph depending on road and season.
        props.setCarSpeed("highway=motorway_link", 15); // = 54kph
        props.setCarSpeed("highway=trunk", 27.27f); // 100kph
        props.setCarSpeed("highway=trunk_link", 15); // = 54kph
        props.setCarSpeed("highway=primary", 27.27f); // 100kph
        props.setCarSpeed("highway=primary_link", 15); // = 54kph

        props.setProperties("highway=residential;maxspeed=30", StreetTraversalPermission.ALL, 0.9, 0.9);
        props.setProperties("highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
        // Default was 2.5, we want to favor using mixed footways somewhat
        props.setProperties("footway=sidewalk;highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);

        /** safety overrides */
        // track should be safer than 1.3
        props.setProperties("highway=track", StreetTraversalPermission.ALL, 1.0, 1.0);

        /** tracktype */
        props.setProperties("tracktype=grade1", StreetTraversalPermission.ALL, 1.0, 1.0, true); // Solid
        props.setProperties("tracktype=grade2", StreetTraversalPermission.ALL, 1.05, 1.05, true); // Solid but unpaved.
        props.setProperties("tracktype=grade3", StreetTraversalPermission.ALL, 1.15, 1.15, true); // Mostly solid.
        props.setProperties("tracktype=grade4", StreetTraversalPermission.ALL, 1.3, 1.3, true); // Mostly soft.
        props.setProperties("tracktype=grade5", StreetTraversalPermission.ALL, 1.5, 1.5, true); // Soft.

        /** We assume highway/cycleway of a cycle network to be safer */
        props.setProperties("lcn=yes", StreetTraversalPermission.ALL, 0.95, 0.95, true); // local cycle network
        props.setProperties("rcn=yes", StreetTraversalPermission.ALL, 0.95, 0.95, true); // regional cycle network
        props.setProperties("ncn=yes", StreetTraversalPermission.ALL, 0.95, 0.95, true); // national cycle network

        props.setProperties("lit=yes", StreetTraversalPermission.ALL, 0.99, 0.99, true); // lit increases safety
        props.setProperties("lit=no", StreetTraversalPermission.ALL, 1.05, 1.05, true); // not lit decreases safety


        // Read the rest from the default set
		new DefaultWayPropertySetSource().populateProperties(props);
	}
}
