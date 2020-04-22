# Bicycle Routing

The default routing algorithms and weights gives you decent results, but they are leaning a little towards being
more suitable for North American street infrastructure and OSM tagging conventions.

MFDZ has developed their own modification to this algorithm, that works in two ways:

 - Introduces a `GermanyWayPropertySetSource` which tailors the speed limits and bicycle safety factors to the local
   ground truth in Germany. More specifically, relations with the tags `lcn` (local cycling network), `rcn` (regional 
   cycling network) and `ncn` (national cycling network) are favoured. This sometimes leads to routes with a longer
   distance but which should be more relaxing for cyclists as main roads are avoided.
   
 - Adds a bicycle-specific cost model (`MfdzIntersectionTraversalCostModel`) for turning from one street to another.
   Here right turns are somewhat avoided and left turns have a high penalty. This is to avoid strange routes where a
   road with heavy traffic is crossed several times in order to use bike paths alongside it.
   
 ## Further work
 
 The algorithm describe above is an improvement of the standard OTP behaviour (at least in the German context) but we
 would like to develop it further with the following ideas:
 
  - Take type of roads and their speed into account when calculating turn costs. It should be penalised more heavily to
    turn left on (or cross) a street with a high speed limit.
    
  - At the moment the lcn, rcn and ncn tags values are added cumulatively. This means if a street is part of all three
    types of networks, it is very heavily favoured and this leads to strange detours. We ought to use an OR condition
    when calculating the safety factor.

