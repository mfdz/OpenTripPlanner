package org.opentripplanner.ridesharing;

import org.junit.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.api.model.Leg;

public class RidesharingTest extends GtfsTest {

	@Override
	public String getFeedName() {
		return "mfdz_gtfs.zip";
	}
	
	@Test
	public void testImport() {
		Leg[] legs = plan(+1478884800L, "6", "7", null, false, false, null, "", "", 1);

        validateLeg(legs[0], 1478885400000L, 1478885700000L, "7", "6", null);

	}
}
