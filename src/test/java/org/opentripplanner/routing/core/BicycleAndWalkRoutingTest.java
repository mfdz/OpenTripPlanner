package org.opentripplanner.routing.core;

import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.TestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class BicycleAndWalkRoutingTest {

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 04, 23, 11, 0, 0);

    public static Graph getDefaultGraph() {
        return TestGraphBuilder.buildGraph(
                ConstantsForTests.HERRENBERG_AND_AROUND_OSM,
                ConstantsForTests.BOEBLINGEN_OSM,
                ConstantsForTests.VAIHINGEN_NORD_OSM
        );
    }

    private static String calculatePolyline(Graph graph, GenericLocation from, GenericLocation to, GenericLocation... intermediatePlaces) {
        return calculatePolyline(graph, from, to, TraverseMode.BICYCLE, intermediatePlaces);
    }

    private static String calculatePolyline(Graph graph, GenericLocation from, GenericLocation to, TraverseMode mode, GenericLocation... intermediatePlaces) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;
        request.intermediatePlaces = Arrays.asList(intermediatePlaces);

        request.modes = new TraverseModeSet(mode);

        request.setNumItineraries(5);
        request.setRoutingContext(graph);
        request.setOptimize(OptimizeType.TRIANGLE); // this is the default setting in our digitransit
        request.setTriangleTimeFactor(0.3);
        request.setTriangleSafetyFactor(0.4);
        request.setTriangleSlopeFactor(0.3);
        request.walkSpeed = 1.2;
        request.bikeSpeed = 5;
        request.bikeSwitchCost = 200;
        request.walkReluctance = 20;
        request.maxWalkDistance = 100000;
        request.itineraryFiltering = 1.5;
        request.transferPenalty = 0;
        request.walkBoardCost = 600;
        request.setWalkReluctance(2);
        request.wheelchairAccessible = false;

        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        //paths.forEach( p -> System.out.println(GraphPathToGeoJson.toGeoJson(p)));

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);
        Stream<List<Coordinate>> points = plan.itinerary.get(0).legs.stream().map(l -> PolylineEncoder.decode(l.legGeometry));
        return PolylineEncoder.createEncodings(points.flatMap(List::stream).collect(Collectors.toList())).getPoints();
    }

    @Test
    public void useBikeNetworkRoutesFromNebringenToHerrenberg() {
        var nebringen = new GenericLocation(48.56494, 8.85318);
        var herrenbergWilhelmstr = new GenericLocation(48.59586,8.87710);
        var polyline = calculatePolyline(getDefaultGraph(), nebringen, herrenbergWilhelmstr);
        assertThatPolylinesAreEqual(polyline, "{ilgHgc`u@IGC[y@?@qAZaCAKEMGKSAe@CWEOe@GGgCqNa@mEWqCo@eHI_Ay@wIYeDo@kHe@yEu@gGm@aFWmBkHgEKCeC\\gFn@M_@NoAWAOGSWRi@zAsFYKIQc@aAEIo@yAGIG\\IZ]p@e@v@u@|@m@f@cCn@?pBGt@Kx@[fB]bBYx@c@`Ag@x@g@b@S@YEa@Ge@OeBw@eAa@a@Cc@JiBv@_@P]FS@o@KsAg@qAe@e@Co@HyBh@MFMHKLEPCLi@Po@PqAT_@N_Ad@c@Nk@LYJ]JU@i@?w@Cg@?G@K?G??[COGOkAwBs@cBK]]oAa@mAKu@]{ByAtAULg@Hq@Aq@EsCQa@CC?qB@]BkAJ}B^eCCsBGqAE[[o@Go@ToCUkAEmAIKAiCK]UFe@c@iD]kB_@sAACe@Rk@JsCh@?IQkBQ?YBu@NEOS}@Y}ESsCMcBUyCi@oEAE");
    }

    @Test
    public void dontUseCycleNetworkInsideHerrenberg() {
        var graph = getDefaultGraph();
        var herrenbergErhardtstBismarckstr = new GenericLocation(48.59247, 8.86811);
        var herrenbergMarkusstrMarienstr = new GenericLocation(48.59329, 8.87253);
        var hildrizhauserStr = new GenericLocation(48.5944599, 8.874989748);
        var brahmsStr  = new GenericLocation(48.59353, 8.87731);
        var schillerStr = new GenericLocation(48.592625, 8.865054);

        var polyline1 = calculatePolyline(graph, herrenbergErhardtstBismarckstr, herrenbergMarkusstrMarienstr);
        assertThatPolylinesAreEqual(polyline1, "}uqgHs`cu@AK?Am@kEIo@?eD]UFc@c@iD]mB_@sAAACIRQ");

        var polyline2 = calculatePolyline(graph, herrenbergErhardtstBismarckstr, hildrizhauserStr);
        assertThatPolylinesAreEqual(polyline2, "}uqgHs`cu@AK?Am@kEIo@?eD]UFc@c@iD]mB_@sAAACIKScAuCc@_AUi@GMQJODGm@QsCGi@");

        var polyline3 = calculatePolyline(graph, herrenbergErhardtstBismarckstr, brahmsStr);
        assertThatPolylinesAreEqual(polyline3, "}uqgHs`cu@AK?Am@kEIo@?eD]UFc@c@iD]mB_@sAAACIKScAuCc@_AUi@|A}BZc@GMUe@AUa@uAOs@EAAAYgATi@b@u@j@yAF{@");

        // here we make sure that we don't take Schulstraße (https://www.openstreetmap.org/way/26403221) just because it is part of several cycle networks
        var polyline4 = calculatePolyline(graph, schillerStr, hildrizhauserStr);
        assertThatPolylinesAreEqual(polyline4, "{vqgHqmbu@??Is@Gq@_@{DUiCUiCSgCIoA?MDI@E`BOIo@?eD]UFe@c@iD]kB_@sAACCIKScAuCc@_AUi@GKQHOFGo@QsCGg@");
    }

    @Test
    public void useDesignatedTrackNearHorberStr() {
        var graph = getDefaultGraph();
        var herrenbergPoststr = new GenericLocation(48.59370,8.86446);
        var herrenberGueltsteinerStr = new GenericLocation(48.58935, 8.86991);

        var polyline = calculatePolyline(graph, herrenbergPoststr, herrenberGueltsteinerStr);
        assertThatPolylinesAreEqual(polyline, "q}qgHwibu@VQHIBG@BBDJLBMDMBIFGHIFEHCHENEd@MPEZK^K^I^K`@KrD?pA@h@ArBW~A[V@VIxAYi@oFaBwNQ}AZ@");
    }

    @Test
    public void useCorrectPathOutsideGaertringen() {
        var graph = getDefaultGraph();
        var gaertringen = new GenericLocation(48.64159, 8.90378);
        var ibm = new GenericLocation(48.60487, 8.87472);

        var polyline = calculatePolyline(graph, gaertringen, ibm);
        assertThatPolylinesAreEqual(polyline, "{h{gHq_ju@@OJu@Fq@F@D@|CXtBAtBFlCTdALF@~ATHCAyCTKrD`@jALdALf@HRIFCbACRHFBNJPJ\\^`@R??ZHdBh@lAj@~@h@JD|@l@vA~@`Ar@`Az@DVLCPLh@^z@t@HJN@JLj@d@BBvBfBnDzCnDdD|@`AJN@RB|@bA@b@L|AzAFRLVLHZDh@^JHPX^|An@dBz@lBp@v@l@t@XHJH@TXHt@^rAn@f@XPBT??B@@B?B?@A`@dAHNLd@FR@LdB{@d@Ul@p@t@p@nAnBzAbCDH\\`@@Nb@x@RZLJl@fA~A`DRd@JRfA~BLVdCrF~BlERZz@bBDHnAdCJHb@Tz@^hBt@f@Rz@\\RXTLd@Vh@PpAb@|@f@PJFBJFJJDCVJNHF?J@J@TFTJLHNNVZHRjAlAp@t@fBbBpAdAbAv@VZPRd@l@Nb@LVNJ\\NNN^b@^b@r@p@p@n@lAlAHMLSNGT?\\v@r@dArAxAhB`BtArAlBzB|BrCtA`Br@r@d@p@b@^NH");
    }

    @Test
    public void dontTakeLandhausstrWhenCrossingBoeblingen() {
        var graph = getDefaultGraph();
        var boeblingenHerrenbergerStr = new GenericLocation(48.68456, 9.00749);
        var boeblingenThermalbad = new GenericLocation(48.69406, 9.02869);

        var polyline = calculatePolyline(graph, boeblingenHerrenbergerStr, boeblingenThermalbad);
        assertThatPolylinesAreEqual(polyline, "ouchHwg~u@CQN?BSD_@U}@i@qBMaAIk@COEI[u@G?E?GQEKKBCAa@IUSKFED?JCDC?[A]CUEKAu@MWKUI[WQMMKc@_@GSUa@QAO_@KWQa@K]GSGUEUGUYqAEYHEYqAWiAGQc@}Bi@{Bi@mESkBEi@QiBSqAYuA]{@c@{@g@}@SAEIEMC[AS?QCQYc@k@y@MSc@WaAwA{A}B]k@QWWi@Oa@[cAUw@U{@EQOJIUI]OkAm@iF?UWuB[cC]}BUgAQu@Oq@Gc@Cc@]cAYs@Qa@_@e@MUHMCC");
    }

    @Test
    public void takeScenicRouteFromNufringenToNagold() {
        var graph = getDefaultGraph();
        var nufringenBahnhof = new GenericLocation(48.62039, 8.88977);
        var nagold = new GenericLocation(48.55131, 8.72866);

        var polyline = calculatePolyline(graph, nufringenBahnhof, nagold);
        assertThatPolylinesAreEqual(polyline, "mdwgH_hgu@Td@@BBF\\t@LQJMpGjMp@pARh@XbALTPQVW_AsB?CRZz@bBDHnAdCJHb@Tz@^hBt@f@Rz@\\RXTLd@Vh@PpAb@|@f@PJFBJFJJDCVJNHF?J@J@TFTJLHNNVZHRjAlAp@t@fBbBpAdAbAv@VZPRd@l@Nb@LVNJ\\NNN^b@^b@r@p@p@n@lAlAHMLSNGT?\\v@r@dArAxAhB`BtArAlBzB|BrCtA`Br@r@d@p@b@^VNT@JCEd@ENGRGHB\\C`AAl@?~@PvA\\fB`@pADTBJFLHH\\d@j@n@FDp@l@~@v@jBzATP~DfDvAdBAPHVFTBAJAF@FBDB\\ZZ`@^r@Xt@\\rAZzAPnAPtAHvAHhBB`D?~I?lDAb@A~@?hBANKH?RA`@JPP@P?@EDGBCBA^?b@Cj@KZKn@YxA_AXM\\OLOXOXKd@GVA~@D~@Lf@@dABJTH?P?J?Fe@`@Wd@Sh@_@HGN?JIFGVbAj@xBDXVbB`@hCBNRbADZTf@~C~Ab@f@LPJXHZDA^MHBHBLGxBuAHE^d@AB?d@AHN`A`Ac@bAyANt@Pv@NRR_@b@Yz@e@x@?d@Cd@Hf@J`@NlABXATCVEFAf@tBb@xAXxAVdC\\zC\\`CNOp@`BDTBPDPNV^Lp@Bn@@L@V?b@BTHb@^Zh@XbAZfA\\rA\\hA^p@\\`@n@d@NTJZNv@L^CP_@Ni@XGLET@bA@b@Hr@\\nBRfAJf@Ff@B|@@VBp@LdCL|BBd@C~@GxC?~@@t@Dn@NzARnABl@XjDJlBChGCbH@fAHHTVXZZh@Rd@z@xATv@P`EFrALnABl@CbEBpBB|B?RHxD?b@BxPDvBFz@f@|Dv@vEl@~GFvCFfBH|BNbCThCZvBf@pBVfBp@pEFP`@jB^fCd@`Dx@i@\\KvBUfI}@fKaADxKRtJ^zB^nC`@rFRLr@]n@Wp@QAL@PFp@h@jE\\jBHAj@OzFcBhBg@bAW`Bg@Hz@Bt@b@rCn@tDn@bD\\hB`@rBl@tCNv@Jh@Lv@TxAHl@P`BVlCLxAN`CJ`BRzEAHFpBFzALhBRlBVnB\\hBRp@`@bAn@~AnB`En@|Ah@`B\\lAPnAFXJh@j@xBf@vAv@jADT@^EtFC`HL`IHpHCpAAp@@^@^@JHd@BTh@vCRdATnBPtB@nGBt@@DB|@HfCTlGLjD?HH|BDpBPrDuDHSBj@zG`@|CXjD@DnAdSv@jJdCzNxAjLnC?UvC?L]tDCd@OpCC\\EJEFEFIFCHCB?DARCRFJBJJXHBLDFBXYLIv@NxAp@z@f@r@j@n@z@pAhClAvEZx@`@bAf@f@ZtB`AJrBHv@Pl@l@PnAZpCCpBFjBr@vCVdAtAbIl@nDnBrDbDvEdAh@zAN`DHtA_@h@Gt@PjBbB`B~BxBfDd@f@FNVCRLPfB@j@?RI~BArACdBIlAa@tC_@zAEXa@BMLIVOfBKr@@v@BtCDfG?tCAf@Ax@IzAWdCWrBY~AMj@Uv@K^Ed@_@`DQ|ACd@BVDTFPh@_@VOR[LSFIDGBJBJ@HDFFBDBFLBRHNAK");
    }

    @Test
    public void takeCycleLaneFromHerrenbergToJettingen() {
        var graph = getDefaultGraph();
        var herrenberg = new GenericLocation(48.594789, 8.86094570);
        var jettingen = new GenericLocation(48.570700, 8.77964);

        var polyline = calculatePolyline(graph, herrenberg, jettingen);
        assertThatPolylinesAreEqual(polyline, "odrgHysau@^xAF\\T~A`@hCBNPbAF\\Vj@~C`Bn@r@JXHX@FHd@Fv@D@@@LC\\IH?HPRt@^rANd@FHXn@^jA`@jAT`ALp@TbB@@XtAVz@Ld@t@|Ad@`@HLEj@BRHJJ?f@h@eBxGs@tBu@bCQn@g@nBBFAXETQl@W~@a@|AYzAUzA?r@J|HFbABHHDbAI~BHHHg@zPLd@Fn@DzAFlHBhD@lECvBpDxBf@XdBt@fCfAZH^Ll@F\\Ct@?t@DbAP\\PVZ^~@v@pBXl@VXTPLHTCLKF]XgCNUNGJ?NFXNTT|Az@?DCNJHKx@Kv@Kx@El@Cr@ApCGp@Np@F\\?NDp@ZbER~DJnAF~@@`AC`@G`AWbC@f@JvADhBDv@H`@Tj@Nf@PpB?fCA~AEn@iAnKg@jEMnAQ~@[dAYr@{ApCYp@KVkAxCw@vA?b@Br@`@pBL~ADRXdBXrBZ`AT`@fAdBHt@`@~BL|ANtCRhEJvDJhFD`EMrDK|BQzBeB`SYxCFEjByApBaBf@Wh@SXGZCP@RB^L^LfAH|@CbBY|@I~@AzC^fAPt@hHRfBTlBb@nD|AhHDN^bBVv@Th@fApBtAdCjCrEh@b@f@Z\\LTDRBhADN?jDDPFrDbD|E~DhBz@fFlCfAp@~BpAfBz@fDdBnAp@J?aAnDaAvDa@~EY~De@xFU?AZEl@Ej@Gj@@f@Hd@MHs@h@q@l@u@h@GD");
    }

    @Test
    public void dontCrossBuesnauerStrToGetToCycleLane() {
        var graph = getDefaultGraph();
        var holderbuschweg = new GenericLocation(48.73613, 9.09802);
        var allmandRing = new GenericLocation(48.74037, 9.09548);

        var polyline = calculatePolyline(graph, holderbuschweg, allmandRing);
        assertThatPolylinesAreEqual(polyline, "ywmhHs}ov@QT]^[^[Z_AhAIHi@z@e@`AUj@EJMMICs@C{AAyA?uD?OFIBGBHn@?N?H@\\E@k@PWFWD");
    }

    @Test
    public void takeTrackhOutsideUnterjettingen() {
        var graph = getDefaultGraph();
        var southOfUnterjettingen = new GenericLocation(48.558041429, 8.784744);
        var wielandStr = new GenericLocation(48.56160, 8.78256);

        var polyline = calculatePolyline(graph, southOfUnterjettingen, wielandStr);
        assertThatPolylinesAreEqual(polyline, "w{jgHswrt@?fA@dHKCI?K@IBK@IAKCMEMCMEMCa@K[KUGUGQEQA[CYESAC?CAI@I?S?g@ASAQASAQAa@A[@W?I@I?K?KAICCCEGCAK??h@@tA?r@iA?s@BC?");
    }

    @Test
    public void shouldWalkOnResidentialRoundabouts() {
        var graph = getDefaultGraph();
        var start = new GenericLocation(48.59190, 8.85884);
        var end = new GenericLocation(48.59270, 8.86068);

        var polyline = calculatePolyline(graph, start, end, TraverseMode.WALK);
        assertThatPolylinesAreEqual(polyline, "grqgHufau@?Ee@cA?ECACAC@?DCBABC?CACCAE?CCKiAsESy@BC");
    }

    @Test
    public void shouldNotWalkOnRoundaboutsOutsideTowns() {
        var graph = getDefaultGraph();
        var start = new GenericLocation(48.56270, 8.75747);
        var end = new GenericLocation(48.56215, 8.75728);

        var polyline = calculatePolyline(graph, start, end, TraverseMode.WALK);
        assertThatPolylinesAreEqual(polyline, "{{kgHammt@N?CRARFJBJJXHBLDFBXYLI?MBQ??");
    }

    @Test
    @Ignore
    public void shouldWalkHindenburgStrasseUnderConstruction() {
        var graph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_HINDENBURG_UNDER_CONSTRUCTION_OSM);

        var hindenburgStr = new GenericLocation(48.59468, 8.87210);
        var nagolderStr = new GenericLocation(48.59567, 8.86637);
        var horberStr = new GenericLocation(48.59522, 8.86716);

        var polyline1 = calculatePolyline(graph, hindenburgStr, nagolderStr, TraverseMode.WALK);
        assertThatPolylinesAreEqual(polyline1, "scrgHsycu@@JDh@@TFd@@l@Et@UrDE~@Gz@Eh@Cn@AZKv@CPADCLGPKVIVOXMVS^GLIFGHGDIHGFGDIFCV?REJFLHP?VA^?h@?L");

        var polyline2 = calculatePolyline(graph, hindenburgStr, horberStr, TraverseMode.WALK);
        assertThatPolylinesAreEqual(polyline2, "scrgHsycu@@JDh@@TFd@@l@Et@UrDE~@Gz@Eh@Cn@AZKv@CPADCLKn@K^ETO`@INMROT?H?F@HDJJLFJ@B");
    }

    @Test
    public void shouldAvoidErhardtStr() {
        var graph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_HINDENBURG_UNDER_CONSTRUCTION_OSM);

        var guelsteinerStr = new GenericLocation(48.59291, 8.87040);
        var bahnhof = new GenericLocation(48.59364, 8.86331);

        var polyline1 = calculatePolyline(graph, guelsteinerStr, bahnhof, TraverseMode.BICYCLE);
        assertThatPolylinesAreEqual(polyline1, "sxqgH}ncu@AB\\ThCJ@pCBxAD`AJz@n@~GeBf@aCp@^zDFp@Hr@[JQDe@LMDKDIBGDIFEHCHELCLKMCEAC??CFIHiAt@??\\~ABJ\\nA");

        var polyline2 = calculatePolyline(graph, bahnhof, guelsteinerStr, TraverseMode.BICYCLE);
        assertThatPolylinesAreEqual(polyline2, "c}qgHwbbu@@F~@o@FKBGBMCKMg@GWCOCEAIAIAK?M?OBMDMBIDIHGFEHCJELEd@MPEZKIs@Gq@_@{D`Cq@dBg@o@_HI{@GaACyAAqCiCK]UBC");
    }

    @Test
    public void shouldBeAbleToCycleMarkusJohannesStrBothDirections() {
        var gültsteinerStr = new GenericLocation(48.59277, 8.87027);
        var hildrizhauserStr = new GenericLocation(48.59435, 8.87407);
        var hauffstr = new GenericLocation(48.59207, 8.86929);
        var marienStr = new GenericLocation(48.59244, 8.87343);

        var polyline1 = calculatePolyline(getDefaultGraph(), gültsteinerStr, hildrizhauserStr);
        assertThatPolylinesAreEqual(polyline1, "ywqgHcncu@[UFc@c@iD]mB_@sAAACIKScAuCc@_AUi@GMQJODGm@AG");

        var polyline2 = calculatePolyline(getDefaultGraph(), hildrizhauserStr, gültsteinerStr);
        assertThatPolylinesAreEqual(polyline2, "sargH}edu@@FFl@NEPKFLTh@b@~@bAtCJRBH@B^rA\\jBb@hDGd@ZT");

        var polyline3 = calculatePolyline(getDefaultGraph(), hauffstr, marienStr);
        assertThatPolylinesAreEqual(polyline3, "isqgHahcu@Ac@AqCJ@B]I]G_@s@yEa@kCSmAOcA?GAEt@a@");

        var polyline4 = calculatePolyline(getDefaultGraph(), marienStr, hauffstr);
        assertThatPolylinesAreEqual(polyline4, "uuqgHyadu@u@`@@D?FNbARlA`@jCr@xEF^H\\C\\KA@pC@b@");
    }
}