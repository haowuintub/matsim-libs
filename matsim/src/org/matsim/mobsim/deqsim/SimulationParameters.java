/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.mobsim.deqsim;

import java.util.HashMap;
import org.matsim.events.Events;
import org.matsim.mobsim.deqsim.util.testable.PopulationModifier;
import org.matsim.mobsim.deqsim.util.testable.TestHandler;

public class SimulationParameters {

	// CONSTANTS
	public static final String START_LEG = "start leg";
	public static final String END_LEG = "end leg";
	public static final String ENTER_LINK = "enter link";
	public static final String LEAVE_LINK = "leave link";
	/**
	 * 
	 * the priorities of the messages. a higher priority comes first in the
	 * message queue (when same time) usage: for example a person has a enter
	 * road message at the same time as leaving the previous road (need to keep
	 * the messages in right order) for events with same time stamp: <br>
	 * leave < arrival < departure < enter especially for testing this is
	 * important
	 * 
	 */
	public static final int PRIORITY_LEAVE_ROAD_MESSAGE = 200;
	public static final int PRIORITY_ARRIVAL_MESSAGE = 150;
	public static final int PRIORITY_DEPARTUARE_MESSAGE = 125;
	public static final int PRIORITY_ENTER_ROAD_MESSAGE = 100;

	// INPUT
	private static double simulationEndTime = Double.MAX_VALUE; // in s
	private static long linkCapacityPeriod = 0; // in s
	private static double gapTravelSpeed = 15.0; // in m/s
	private static double flowCapacityFactor = 1.0; // 1.0 is default
	private static double storageCapacityFactor = 1.0; // 1.0 is default
	private static double carSize = 7.5; // in meter
	// in [vehicles/hour] per lane, can be scaled with flow capacity factor
	private static double minimumInFlowCapacity = 1800;
	/**
	 * stuckTime is used for deadlock prevention. when a car waits for more than
	 * 'stuckTime' for entering next road, it will enter the next. in seconds
	 */
	private static double squeezeTime = 1800;
	/**
	 * this must be initialized before starting the simulation! mapping:
	 * key=linkId used to find a road corresponding to a link
	 */
	private static HashMap<String, Road> allRoads = null;

	// SETTINGS
	// should garbage collection of messages be activated
	private static boolean GC_MESSAGES = false;

	// OUTPUT
	// The thread for processing the events
	private static Events processEventThread = null;

	// TESTING
	// test injection variables
	private static TestHandler testEventHandler = null;
	private static String testPlanPath = null;
	private static PopulationModifier testPopulationModifier = null;

	/**
	 * how far can the average usage of links differ for a unit test to pass in
	 * percent
	 */
	public static final double maxAbsLinkAverage = 0.01;

	// METHODS
	public static boolean isGC_MESSAGES() {
		return GC_MESSAGES;
	}

	public static void setGC_MESSAGES(boolean gc_messages) {
		GC_MESSAGES = gc_messages;
	}

	public static double getSimulationEndTime() {
		return simulationEndTime;
	}

	public static void setSimulationEndTime(double simulationEndTime) {
		SimulationParameters.simulationEndTime = simulationEndTime;
	}

	public static long getLinkCapacityPeriod() {
		return linkCapacityPeriod;
	}

	public static void setLinkCapacityPeriod(long linkCapacityPeriod) {
		SimulationParameters.linkCapacityPeriod = linkCapacityPeriod;
	}

	public static double getGapTravelSpeed() {
		return gapTravelSpeed;
	}

	public static void setGapTravelSpeed(double gapTravelSpeed) {
		SimulationParameters.gapTravelSpeed = gapTravelSpeed;
	}

	public static double getFlowCapacityFactor() {
		return flowCapacityFactor;
	}

	public static void setFlowCapacityFactor(double flowCapacityFactor) {
		SimulationParameters.flowCapacityFactor = flowCapacityFactor;
	}

	public static double getStorageCapacityFactor() {
		return storageCapacityFactor;
	}

	public static void setStorageCapacityFactor(double storageCapacityFactor) {
		SimulationParameters.storageCapacityFactor = storageCapacityFactor;
	}

	public static double getCarSize() {
		return carSize;
	}

	public static void setCarSize(double carSize) {
		SimulationParameters.carSize = carSize;
	}

	public static double getMinimumInFlowCapacity() {
		return minimumInFlowCapacity;
	}

	public static void setMinimumInFlowCapacity(double minimumInFlowCapacity) {
		SimulationParameters.minimumInFlowCapacity = minimumInFlowCapacity;
	}


	public static double getSqueezeTime() {
		return squeezeTime;
	}

	public static void setSqueezeTime(double squeezeTime) {
		SimulationParameters.squeezeTime = squeezeTime;
	}

	public static Events getProcessEventThread() {
		return processEventThread;
	}

	public static void setProcessEventThread(Events processEventThread) {
		SimulationParameters.processEventThread = processEventThread;
	}

	public static TestHandler getTestEventHandler() {
		return testEventHandler;
	}

	public static void setTestEventHandler(TestHandler testEventHandler) {
		SimulationParameters.testEventHandler = testEventHandler;
	}

	public static String getTestPlanPath() {
		return testPlanPath;
	}

	public static void setTestPlanPath(String testPlanPath) {
		SimulationParameters.testPlanPath = testPlanPath;
	}

	public static PopulationModifier getTestPopulationModifier() {
		return testPopulationModifier;
	}

	public static void setTestPopulationModifier(PopulationModifier testPopulationModifier) {
		SimulationParameters.testPopulationModifier = testPopulationModifier;
	}

	public static HashMap<String, Road> getAllRoads() {
		return allRoads;
	}

	public static void setAllRoads(HashMap<String, Road> allRoads) {
		SimulationParameters.allRoads = allRoads;
	}

}
