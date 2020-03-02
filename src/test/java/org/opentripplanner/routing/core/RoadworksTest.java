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

package org.opentripplanner.routing.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.geotools.geojson.geom.GeometryJSON;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.roadworks.RoadworksSource;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class RoadworksTest {

    private static final Logger LOG = LoggerFactory.getLogger(RoadworksTest.class);
    AStar aStar = new AStar();

    static Graph graph;
    static Long blockedWayId = 30082004L;

    GenericLocation zeppelinstr = new GenericLocation(48.59654,8.86100);
    GenericLocation hildrizhauserStr = new GenericLocation(48.60008,8.88863);
    // needs to be in 2017 because that's when the Oslo GTFS feed is valid. probably want to get a slimmed down Herrenberg feed.
    long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2017, 10, 15, 7, 0, 0);
    GeometryJSON geojson = new GeometryJSON();


    @BeforeClass
    public static void setUp() {
        GraphBuilder graphBuilder = new GraphBuilder();

        List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
        OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(new File(ConstantsForTests.HERRENBERG_OSM));
        osmProviders.add(osmProvider);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
        osmModule.edgeFactory = new DefaultStreetEdgeFactory();
        osmModule.skipVisibility = true;
        graphBuilder.addModule(osmModule);
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        GtfsBundle gtfsBundle = new GtfsBundle(new File(ConstantsForTests.OSLO_MINIMAL_GTFS));
        gtfsBundles.add(gtfsBundle);
        GtfsModule gtfsModule = new GtfsModule(gtfsBundles);
        graphBuilder.addModule(gtfsModule);
        graphBuilder.addModule(new StreetLinkerModule());
        graphBuilder.serializeGraph = false;
        graphBuilder.run();

        graph = graphBuilder.getGraph();
        graph.roadworksSource = new RoadworksSource(blockedWayId);
        graph.index(new DefaultStreetVertexIndexFactory());
    }

    private RoutingRequest buildRoutingRequest(Graph graph) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = zeppelinstr;
        request.to = hildrizhauserStr;

        request.setNumItineraries(1);
        request.setRoutingContext(graph);

        request.modes = new TraverseModeSet(TraverseMode.CAR);
        return request;
    }

    @Test
    public void withStreetBlockedDueToRoadworks() {
        RoutingRequest options = buildRoutingRequest(graph);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPaths().get(0);

        LOG.info(geojson.toString(path.getGeometry()));

        List<Long> wayIds = path.edges.stream()
                .filter(e -> e instanceof StreetEdge)
                .map(e -> (StreetEdge) e)
                .map(e -> e.wayId)
                .collect(Collectors.toList());

        assertThat(wayIds, not(hasItem(blockedWayId)));
    }

}
