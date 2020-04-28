# Bicycle Routing

The default routing algorithms and weights gives you decent results, but they are leaning a little towards being
more suitable for North American and urban street infrastructure and OSM tagging conventions.

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
   
 ## Printing turn costs
 
 OTP "costs" are abstract values. The numbers themselves are only meaningful when compared with other costs such as
 taking a detour, getting off the bike and pushing and going on a road that is very safe for cyclists.
 
 If you are interested in turn costs, then you can set the log level of `MfdzIntersectionTraversalCost` in `logback.xml`
 to `trace` and then you can see log lines like this:
 
 ```
Turning from StreetEdge(65141, Büsnauer Straße, <osm:node:241778520 lat,lng=48.7366516,9.097457> -> <osm:node:246830790 lat,lng=48.7369739,9.0970875> length=44.929 carSpeed=13.8889 permission=ALL ref=null NTT=false) to StreetEdge(42024, lcn, 57, <osm:node:246830790 lat,lng=48.7369739,9.0970875> -> <osm:node:1718440863 lat,lng=48.7369384,9.096977500000001> length=8.98 carSpeed=11.2 permission=PEDESTRIAN_AND_BICYCLE ref=null NTT=false) has a cost of 12.0
```

Note: This will print all *possible* turns so the log will be very noisy.
   
 ## Further work
 
 The algorithm describe above is an improvement of the standard OTP behaviour (at least in the German context) but we
 would like to develop it further with the following ideas:
 
  - Take type of roads and their speed into account when calculating turn costs. It should be penalised more heavily to
    turn left on (or cross) a street with a high speed limit.

