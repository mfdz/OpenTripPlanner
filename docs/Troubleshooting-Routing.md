# Troubleshooting Routing

## Graph Builder Annotations

When you build a graph, OTP may encounter clearly incorrect or ambiguous data, or may detect less severe but potentially problematic situations in the input data. Most such problems result in a "graph builder annotation" being added to the graph. Many annotations are logged to the console while the graph, but some that might yield too many messages may be recorded as annotations without being logged. At the end of the graph build process, OTP prints a summary of all the annotations it added to the graph, like the following:

 ```
 11:35:57.515 INFO (Graph.java:970) Summary (number of each type of annotation):
 11:35:57.518 INFO (Graph.java:976)     TurnRestrictionBad - 560
 11:35:57.518 INFO (Graph.java:976)     TurnRestrictionException - 15
 11:35:57.518 INFO (Graph.java:976)     StopLinkedTooFar - 22
 11:35:57.518 INFO (Graph.java:976)     HopSpeedSlow - 22
 11:35:57.518 INFO (Graph.java:976)     Graphwide - 1
 11:35:57.518 INFO (Graph.java:976)     GraphConnectivity - 407
 11:35:57.519 INFO (Graph.java:976)     ParkAndRideUnlinked - 1
 11:35:57.519 INFO (Graph.java:976)     StopNotLinkedForTransfers - 31
 11:35:57.519 INFO (Graph.java:976)     NoFutureDates - 1
```

The full set of annotations can be written out to an HTML report for closer inspection. To enable the creation of these (potentially voluminous) HTML reports, add `"htmlAnnotations" : true` to your graph builder JSON configuration.

If the graph is saved to a file, these annotations are saved with it and can be examined later. Currently the only tool for doing this is the "Graph Visualizer", which is not particularly well maintained and is intended for use by software developers familiar with OTP who can patch up the code as needed.


## Debug layers

OpenTripplanner has option to ease debugging problems with graph. Older option is graph visualizer.
Which you can enable with `--visualize` parameter instead of `--server` when starting OTP.
There you can see whole graph. You can click on edges and vertices and see the metadata. It is
 useful to see if street has expected options. And if connections are where they are expected.

It can be hard to use on large graphs since, whole graph is displayed at once. And it can be hard
 to search for specific streets since only street graph is shown without the rest of information.
 
 Another option is to use debug layers, which shows extra layers on top of normal map.
 To enable them you need to add `?debug_layers=true` to URL. For example 
 [http://localhost:8080/?debug_layers=true](http://localhost:8080/?debug_layers=true).
  This adds debug layers to layer choosing dialog. Currently you can choose between:

- Wheelchair access (which colors street edges red if they don't allow wheelchair or green otherwise)
- Bike Safety (colors street edges based on how good are for cycling [smaller is better])
- Traversal permissions (colors street edges based on what types of transit modes are allowed to
 travel on them (Pedestrian, cycling, car are currently supported)) Traversal permissions layer also
 draws links from transit stops/bike rentals and P+R to graph. And also draws transit stops, bike rentals
  and P+R vertices with different color.

### Interpretation Traversal permissions layer

A sample traversal permissions layer looks like the following 
![screen shot 2015-06-26 at 11 45 22](https://cloud.githubusercontent.com/assets/4493762/8374829/df05c438-1bf8-11e5-8ead-c1dea41af122.png)
* Yellow lines is the link between a stop and the street graph.
* Grey lines are streets one can travel with the mode walk, bike, or car
* Green lines are paths one can travel with the mode walk only
* Red lines are streets one can travel with the mode car only
* Grey dots vertices where edges are connected. If two edges are crossing w/o a vertice at the intersection point, users will not be able to go from one street to the other. But this can be valid in case of over/under pass for 
example. If it's an error, it's usually caused by improperly connected OSM data (a shared OSM node is required). 

## OpenStreetMap Data

### Tags Affecting Permissions

Access tags (such as bicycle/foot = yes/no/designated) can be used to override default graph-building parameters. 

As a default, foot and bicycle traffic is ''not'' allowed on `highway=trunk`, `highway=trunk_link`, `highway=motorway`, `highway=motorway_link`, or `highway=construction`. 

Both *are* allowed on `highway=pedestrian`, `highway=cycleway`, and `highway=footway`. 

Finally, bicycles are *not*allowed on *highway=footway* when any of the following tags appear on a footway: `footway=sidewalk`, `public_transport=platform`, or `railway=platform`.

Other access tags (such as `access=no` and `access=private` affect routing as well, and can be overridden similarly. While `access=no` prohibits all traffic, `access=private` disallows through traffic.

See [osmWayPropertySet config attribute](Configuration#Way-property-sets)

### Railway Platforms

OTP users in Helsinki have documented their best practices for coding railway platforms in OpenStreetMap. These guidelines are available [in the OSM Wiki.](https://wiki.openstreetmap.org/wiki/Digitransit#Editing_railway_platforms)

### Bicycle Routing

The default routing algorithms and weights gives you decent results, but they are leaning a little towards being
more suitable for North American OSM tagging conventions and urban street infrastructure.

MFDZ has developed their own modification to this algorithm, that works in two ways:

 - Introduces a `GermanyWayPropertySetSource` which tailors the speed limits and bicycle safety factors to the local
   ground truth in Germany. More specifically, relations with the tags `lcn` (local cycling network), `rcn` (regional 
   cycling network) and `ncn` (national cycling network) are favoured. This sometimes leads to routes with a longer
   distance but which should be more relaxing for cyclists as main roads are avoided.
   This is particulary effective when travelling in between towns rather than in a larger city.
   
 - Adds a bicycle-specific cost model (`MfdzIntersectionTraversalCostModel`) for turning from one street to another.
   Here right turns are somewhat avoided and left turns have a high penalty. This is to avoid strange routes where a
   road with heavy traffic is crossed several times in order to use bike paths alongside it.
   You can find the code [here](https://github.com/mfdz/OpenTripPlanner/blob/26e8c8a1764ecc23787d0250e4a4cce0764e625c/src/main/java/org/opentripplanner/routing/core/MfdzIntersectionTraversalCostModel.java#L91-L103).
   
#### Printing turn costs
 
OTP "costs" are abstract values. The numbers themselves are only meaningful when compared with other costs such as
taking a detour, getting off the bike and pushing and going on a road that is very safe for cyclists.
 
If you are interested in turn costs, then you can set the log level of `MfdzIntersectionTraversalCost` in `logback.xml`
to `trace` and then you can see log lines like this:
 
 ```
Turning from StreetEdge(65141, Büsnauer Straße, <osm:node:241778520 lat,lng=48.7366516,9.097457> -> <osm:node:246830790 lat,lng=48.7369739,9.0970875> length=44.929 carSpeed=13.8889 permission=ALL ref=null NTT=false) to StreetEdge(42024, lcn, 57, <osm:node:246830790 lat,lng=48.7369739,9.0970875> -> <osm:node:1718440863 lat,lng=48.7369384,9.096977500000001> length=8.98 carSpeed=11.2 permission=PEDESTRIAN_AND_BICYCLE ref=null NTT=false) has a cost of 12.0
```

Note: This will print all *possible* turns so the log will be very noisy.

#### Unit tests with micro graphs

In order to fine-tune the routing results we wrote a set of unit tests in `BicycleRoutingTest` that first builds a very 
small graph from OSM data for a single town and then sends routing requests through the graph.

If you notice an undesirable result you can add a test in this file and modify for example the turn costs or the
weighting of the triangle factors to check what result this would have on your example and the already existing existing
routes.

If you combine this with printing the turn costs (see above) you can e
   
##### Further work
 
 The algorithm describe above is an improvement of the standard OTP behaviour (at least in the German context) but we
 would like to develop it further with the following ideas:
 
  - Take type of roads and their speed into account when calculating turn costs. It should be penalised more heavily to
    turn left on (or cross) a street with a high speed limit.


### Further information
* [General information](https://github.com/opentripplanner/OpenTripPlanner/wiki/GraphBuilder#graph-concepts)
* [Bicycle routing](http://wiki.openstreetmap.org/wiki/OpenTripPlanner#Bicycle_routing)
* [Indoor mapping](https://github.com/opentripplanner/OpenTripPlanner/wiki/Indoor-mapping)
* [Elevators](http://wiki.openstreetmap.org/wiki/OpenTripPlanner#Elevators)
