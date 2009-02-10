package org.matsim.mobsim.deqsim;

import org.matsim.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class ConfigParameterTest extends MatsimTestCase {

	public void testParametersSetCorrectly() {
		String args[] = new String[] { "test/input/org/matsim/mobsim/deqsim/config.xml" };
		Controler controler = new Controler(args);
		controler.setOverwriteFiles(true);
		controler.run();
		/*
		 * make asserts sure all simulation parameters are set properly from
		 * config xml file
		 */

		assertEquals(360.0, SimulationParameters.getSimulationEndTime());
		assertEquals(2.0, SimulationParameters.getFlowCapacityFactor());
		assertEquals(3.0, SimulationParameters.getStorageCapacityFactor());
		assertEquals(3600.0, SimulationParameters.getMinimumInFlowCapacity());
		assertEquals(10.0, SimulationParameters.getCarSize());
		assertEquals(20.0, SimulationParameters.getGapTravelSpeed());
		assertEquals(9000.0, SimulationParameters.getSqueezeTime());
	}

	// TODO: assert, that simulation stops, when target time is reached

	// TODO: simulate using the parameters just one car and see the influence of
	// the scaling factors

}
