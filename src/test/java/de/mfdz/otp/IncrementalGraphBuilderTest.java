package de.mfdz.otp;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.standalone.CommandLineParameters;

public class IncrementalGraphBuilderTest {

	@Test
	public void test() {
		//GraphBuilder baseGraphBuilder = GraphBuilder.forDirectory(new CommandLineParameters(), new File("C:/Users/Holger/Documents/systect/016-MFG/GTFS/mannheim/"));
		//baseGraphBuilder.run();
		File formerGraph = new File("C:/Users/Holger/Documents/systect/016-MFG/GTFS/mannheim/mfdz/Graph.obj");
		if (formerGraph.canWrite()) {
			formerGraph.delete();
		}
		GraphBuilder graphBuilder = GraphBuilder.forDirectory(new CommandLineParameters(), new File("C:/Users/Holger/Documents/systect/016-MFG/GTFS/mannheim/mfdz"));
		//graphBuilder.setBaseGraph("C:/Users/Holger/Documents/systect/016-MFG/GTFS/mannheim/Graph.obj");
		graphBuilder.run();
		
	}

}
