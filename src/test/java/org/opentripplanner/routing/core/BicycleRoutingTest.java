package org.opentripplanner.routing.core;

import org.junit.BeforeClass;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class BicycleRoutingTest {

    static Graph herrenbergGraph;
    static Graph boeblingenGraph;

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 03, 10, 11, 0, 0);

    @BeforeClass
    public static void setUp() {
        herrenbergGraph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_AND_AROUND_OSM);
        boeblingenGraph = TestGraphBuilder.buildGraph(ConstantsForTests.BOEBLINGEN_OSM);
    }

    private static String calculatePolyline(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.modes = new TraverseModeSet(TraverseMode.BICYCLE);

        request.setNumItineraries(1);
        request.setRoutingContext(graph);
        request.setOptimize(OptimizeType.GREENWAYS); // this is the default setting in digitransit

        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);
        Stream<List<Coordinate>> points = plan.itinerary.get(0).legs.stream().map(l -> PolylineEncoder.decode(l.legGeometry));
        return PolylineEncoder.createEncodings(points.flatMap(List::stream).collect(Collectors.toList())).getPoints();
    }

    @Test
    public void useBikeNetworkRoutesFromNebringenToHerrenberg() {
        var nebringen = new GenericLocation(48.56494, 8.85318);
        var herrenbergWilhelmstr = new GenericLocation(48.59586,8.87710);
        var polyline = calculatePolyline(herrenbergGraph, nebringen, herrenbergWilhelmstr);
        assertThatPolylinesAreEqual(polyline, "{ilgHgc`u@IGC[y@?@qAZaCAKEMGKSAe@CWEOe@GGgCqNa@mEWqCo@eHI_Ay@wIYeDo@kHe@yEu@gGm@aFWmBkHgEKCeC\\gFn@M_@NoAWAOGSWRi@zAsFYKIQc@aAEIo@yAGIG\\IZ]p@e@v@u@|@m@f@cCn@?pBGt@Kx@[fB]bBYx@c@`Ag@x@g@b@S@YEa@Ge@OeBw@eAa@a@Cc@JiBv@_@P]FS@o@KsAg@qAe@e@Co@HyBh@MFMHKLEPCLi@Po@PqAT_@N_Ad@c@Nk@LYJ]JU@i@?w@Cg@?G@K?G??[COGOkAwBs@cBK]]oAa@mAKu@]{ByAtAULg@Hq@Aq@EsCQa@CC?qB@]BkAJ}B^eCCsBGqAE[[o@GM_DQ_EK{Be@wCm@wDAGqB`AwB`AmB|@GDg@~@e@l@y@t@e@Rk@JsCh@?IQkBQ?YBu@NEOS}@Y}ESsCMcBUyCi@oEAE");

    }

    @Test
    public void dontUseCycleNetworkInsideHerrenberg() {
        var herrenbergErhardtstBismarckstr = new GenericLocation(48.59247, 8.86811);
        var herrenbergMarkusstrMarienstr = new GenericLocation(48.59329, 8.87253);
        var polyline = calculatePolyline(herrenbergGraph, herrenbergErhardtstBismarckstr, herrenbergMarkusstrMarienstr);
        assertThatPolylinesAreEqual(polyline, "}uqgHs`cu@AK?Am@kEIo@?eD]UFc@c@iD]mB_@sAAACIRQ");
    }

    @Test
    public void useDesignatedTrackNearHorberStr() {
        var herrenbergPoststr = new GenericLocation(48.59370,8.86446);
        var herrenberGueltsteinerStr = new GenericLocation(48.58935, 8.86991);

        var polyline = calculatePolyline(herrenbergGraph, herrenbergPoststr, herrenberGueltsteinerStr);
        assertThatPolylinesAreEqual(polyline, "q}qgHwibu@VQHIBG@BBDJLBMDMBIFGHIFEHCHENEd@MPEZK^K^I^K`@KrD?pA@h@ArBW~A[V@VIxAYi@oFaBwNQ}AZ@");
    }

    @Test
    public void useCorrectPathOutsideGaertringen() {
        var gaertringen = new GenericLocation(48.64159, 8.90378);
        var ibm = new GenericLocation(48.60487, 8.87472);

        var polyline = calculatePolyline(herrenbergGraph, gaertringen, ibm);
        assertThatPolylinesAreEqual(polyline, "{h{gHq_ju@@OJu@Fq@F@D@|CXtBAtBFlCTdALF@~ATHCAyCTKrD`@jALdALf@HRIFCbACRHFBNJPJ\\^`@R??ZHdBh@lAj@~@h@JD|@l@vA~@`Ar@`Az@DVLCPLh@^z@t@HJN@JLj@d@BBvBfBnDzCnDdD|@`AJN@RB|@bA@b@L|AzAFRLVLHZDh@^JHPX^|An@dBz@lBp@v@l@t@XHJH@TXHt@^rAn@f@XPBT??B@@B?B?@A`@dAHNLd@FR@LdB{@d@Ul@p@t@p@nAnBzAbCDH\\`@@Nb@x@RZLJl@fA~A`DRd@JRfA~BLVdCrF~BlERZz@bBDHnAdCJHb@Tz@^hBt@f@Rz@\\RXTLd@Vh@PpAb@|@f@PJFBJFJJDCVJNHF?J@J@TFTJLHNNVZHRjAlAp@t@fBbBpAdAbAv@VZPRd@l@Nb@LVNJ\\NNN^b@^b@r@p@p@n@lAlAHMLSNGT?\\v@r@dArAxAhB`BtArAlBzB|BrCtA`Br@r@d@p@b@^NH");
    }

    @Test
    public void dontTakeLandhausstrWhenCrossingBoeblingen() {
        var boeblingenHerrenbergerStr = new GenericLocation(48.68456, 9.00749);
        var boeblingenThermalbad = new GenericLocation(48.69406, 9.02869);

        var polyline = calculatePolyline(boeblingenGraph, boeblingenHerrenbergerStr, boeblingenThermalbad);
        assertThatPolylinesAreEqual(polyline, "ouchHwg~u@CQN?BSD_@U}@i@qBMaAIk@COa@_AG?E?GQEKKBCAa@IUSKFED?JCD@Nm@EWC[AYIYGg@QSQQKKGMAEIGCGIGGWg@QYOQO_@IWSa@I]GSGUEUGUYqAGYHEYqAWiAEQc@}Bi@{Bk@mESkBEi@QiBSqAYuA]{@c@{@g@}@SAEIEMC[AS?QCQYc@k@y@MSc@WaAwA{A}B]k@QWWi@Oa@[cAUw@U{@EQOJIUI]OkAm@iF?UWuB[cC]}BUgAQu@Oq@Gc@Cc@]cAYs@Qa@_@e@MUHMCC");
    }

    @Test
    public void takeScenicRouteFromNufringenToNagold() {
        var nufringenBahnhof = new GenericLocation(48.62039, 8.88977);
        var nagold = new GenericLocation(48.55131, 8.72866);

        var polyline = calculatePolyline(herrenbergGraph, nufringenBahnhof, nagold);
        assertThatPolylinesAreEqual(polyline, "mdwgH_hgu@Td@???y@DGLOFMLQ??Rd@JRfA~BLVdCrF~BnERZz@bBDHnAdCJFb@Tz@^hBt@f@Rz@\\TXTLd@Vh@PpAb@|@f@PJFBJFJJDCVJNHF?H@J@TFTJLHNNVZHRjAlAp@t@hBbBpAdAbAv@VZPRd@l@Nb@LVNJ\\NNN^b@^b@p@p@r@n@lAlAHMLSNGT?\\v@p@dArAxAhB`BtArAlBzB|BrCtA`Br@r@d@p@b@^VNT@JCEd@ENGRGHB\\C`AAl@?~@PvA\\fB`@pAFTBJFLHH\\d@j@n@FDp@l@~@v@jBzATP~DfDvAdBAPHVFTBAJAF@FBDB\\ZZ`@^r@Xt@\\rAZzAPnAPtAHvAHhBB`D?~I?lDAb@A~@?hBANKH?RA`@JPP@P?@EDGBCBA^?b@Cj@IZKn@YxA_AXM\\OLOXQXId@IV?~@D~@Jf@@dABJTH?P?J?Fe@`@Wd@Sh@_@HGN?JIFG@I`@yABGJEJGDEl@UHJ?Dl@e@LIXSVS@AJDL@Mg@@EXXBAS}@AE?CEg@LS~@o@FKBGLNXpAJXT\\hBpAfA|@t@bARf@R\\b@v@Td@VZ\\PL?|@c@VGfA[?N@bE?x@?d@B~@?FDf@DZPrBJhC@|AAbAAP?F?l@AnBHpBJrAJp@DNXdBG~@UjBKfAk@`@QTYb@SVSXGR?THXf@h@eBxGs@tBw@bCQn@e@nBBFAXETQl@W~@a@|AYzAUzA?r@JzHFdABHHDbAI~BHHFg@|PLd@Fn@DxAFnHBhD@lECtBpDxBf@XdBt@fCfAZH^Ll@FZCt@?t@DdAPZPVZ^~@v@pBXl@VXTPLHTCNKF]XgCNUNGJ?NFXNTT|Az@?DCNJHKx@Kv@Kx@El@Cr@ApCGp@Np@F\\?NDp@ZbER~DJnAF~@@`AC`@G`AWbC@f@JvADhBDv@H`@Tj@Nf@PpB?fCA~AEn@iAnKg@jEMnAQ~@[dAYr@{ApCYp@KVkAxCw@vA?b@Br@`@pBL~ADRXdBXrBZ`AR`@fAdBJt@`@~BL|ANtCRhEJvDJhFD`EMrDK|BQzBeB`SYxCDEjByApBaBf@Wh@SXGZCP@RB^L^LfAH|@CbBY|@I~@AzC^fAPt@hHRfBTlBb@nD|AhHDN^bBVv@Th@fApBtAdClCrEh@b@f@Z\\LTDRBhADL?lDDPFrDbDzE~DhBz@fFlCfAp@~BpAfBz@fDdBnAp@J?f@hAj@rAb@dAHd@BHDXDb@XzA?P`@bDXhC\\jCD`@Dh@@FArAKl@Mp@M\\GTGfAA~@X`Dj@fGh@vEHb@Hv@NdB@VBVVlER~FjFMXGj@zG`@|CXjD@DnAdSv@jJdCzNxAjLnC?RBcAjO?A?nADFDJ@JPL??@Ib@Pd@FhA`@fAr@p@p@GLIPn@x@pAjCnAvEZv@`@bAd@f@\\tB~@JrBHv@Pl@l@PpAZpCApBDjBr@vCVdAtAbIn@nDnBrD`DvEdAh@zAN`DHtA_@h@Gt@PlBbB~A~BzBfDd@f@DNVCRLPfB@j@?RI~BArACdBIlAa@tC_@zAEXa@BMLIVOfBKr@@v@BtCDfG?tCAf@Ax@IzAWdCWrBW~AMj@Uv@K^Ed@_@`DQ|ACd@@VDTFPh@_@VOR[LSFIDGBJBJ@HDFFBD@FLBRHNAK");
    }
}