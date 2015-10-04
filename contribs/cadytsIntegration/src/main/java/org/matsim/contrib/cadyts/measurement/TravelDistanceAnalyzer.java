/* *********************************************************************** *
 * project: org.matsim.*
 * VolumesAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.cadyts.measurement;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;


// adapted from VolumesAnalyzer
public class TravelDistanceAnalyzer implements LinkLeaveEventHandler, PersonDepartureEventHandler {

	private final static Logger log = Logger.getLogger(TravelDistanceAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final Map<Id<Link>, int[]> links;
//	private final Map<Id<Measurement>, int[]> measurements;
	
	// for multi-modal support
	private final boolean observeModes;
	private final Map<Id<Person>, String> enRouteModes;
//	private final Map<Id<Link>, Map<String, int[]>> linksPerMode;
	
	// new
	private Map<Id, Id> personId2departureLinkId;
	private Map<Id, Double> personId2CarDistance;
	private int carTrips;
	private final Network network;
	// end new

	public TravelDistanceAnalyzer(final int timeBinSize, final int maxTime, final Network network) {
//	public TravelDistanceAnalyzer(final int timeBinSize, final int maxTime, final Measurements measurements) {
		this(timeBinSize, maxTime, network, true);
//		this(timeBinSize, maxTime, measurements, true);
	}
	
	public TravelDistanceAnalyzer(final int timeBinSize, final int maxTime, final Network network, boolean observeModes) {
//	public TravelDistanceAnalyzer(final int timeBinSize, final int maxTime, final Measurements measurements, boolean observeModes) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
//		this.measurements = new HashMap<>();
		this.links = new HashMap<>((int) (network.getLinks().size() * 1.1), 0.95f);
		
		// new
		this.network = network;
		// end new
		
		this.observeModes = observeModes;
		if (this.observeModes) {
			this.enRouteModes = new HashMap<>();
//			this.linksPerMode = new HashMap<>((int) (network.getLinks().size() * 1.1), 0.95f);
		} else {
			this.enRouteModes = null;
//			this.linksPerMode = null;
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (observeModes) {
			enRouteModes.put(event.getPersonId(), event.getLegMode());
		}

		
		// new; from "CarDistanceEventHandler"
		// the following is not neccessary any more...see below
		personId2departureLinkId.put(event.getPersonId(), event.getLinkId());

//		if (this.ptDriverIdAnalyzer.isPtDriver(event.getPersonId())){
//			// ptDriver!
//		} else {
			// calculating the number of trips...
			if(event.getLegMode().equals(TransportMode.car)){
				Id personId = event.getPersonId();
				int carTripsSoFar = carTrips;
				int carTripsAfter = carTripsSoFar + 1;
				carTrips = carTripsAfter;

				// in order to get the number of car users right...see below
				if(this.personId2CarDistance.get(personId) == null){
					this.personId2CarDistance.put(personId, 0.0);
				} else {
					// do nothing
				}
			} else {
				// other mode
			}
//		}
		// end new
	}
	
	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		// new; from "CarDistanceEventHandler"
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		Double linkLength_m = this.network.getLinks().get(linkId).getLength();
//		if (this.ptDriverIdAnalyzer.isPtDriver(personId)){
//			// pt vehicle!
//		} else {
			if(this.personId2CarDistance.get(personId) == null){
				this.personId2CarDistance.put(personId, linkLength_m);
			} else {
				double distanceSoFar = this.personId2CarDistance.get(personId);
				double distanceAfterEvent = distanceSoFar + linkLength_m;
				this.personId2CarDistance.put(personId, distanceAfterEvent);
			}
//		}
		// end new
		
		
//		int[] volumes = this.links.get(event.getLinkId());
//		if (volumes == null) {
//			volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
//			this.links.put(event.getLinkId(), volumes);
//		}
//		int timeslot = getTimeSlotIndex(event.getTime());
//		volumes[timeslot]++;
//		
//		if (observeModes) {
//			Map<String, int[]> modeVolumes = this.linksPerMode.get(event.getLinkId());
//			if (modeVolumes == null) {
//				modeVolumes = new HashMap<>();
//				this.linksPerMode.put(event.getLinkId(), modeVolumes);
//			}
//			String mode = enRouteModes.get(event.getPersonId());
//			volumes = modeVolumes.get(mode);
//			if (volumes == null) {
//				volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
//				modeVolumes.put(mode, volumes);
//			}
//			volumes[timeslot]++;
//		}
	}
	
	// new
	protected Map<Id, Double> getPersonId2CarDistance() {
		return this.personId2CarDistance;
	}
	// end new

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	/**
	 * @param linkId
	 * @return Array containing the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
//	public int[] getVolumesForLink(final Id<Link> linkId) {
//		return this.links.get(linkId);
//	}
	
	/**
	 * @param linkId
	 * @param mode
	 * @return Array containing the number of vehicles using the specified mode leaving the link 
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
//	public int[] getVolumesForLink(final Id<Link> linkId, String mode) {
//		if (observeModes) {
//			Map<String, int[]> modeVolumes = this.linksPerMode.get(linkId);
//			if (modeVolumes != null) return modeVolumes.get(mode);
//		} 
//		return null;
//	}

	/**
	 *
	 * @return The size of the arrays returned by calls to the {@link #getVolumesForLink(Id)} and the {@link #getVolumesForLink(Id, String)}
	 * methods.
	 */
	public int getVolumesArraySize() {
		return this.maxSlotIndex + 1;
	}
	
	/*
	 * This procedure is only working if (hour % timeBinSize == 0)
	 * 
	 * Example: 15 minutes bins
	 *  ___________________
	 * |  0 | 1  | 2  | 3  |
	 * |____|____|____|____|
	 * 0   900 1800  2700 3600
		___________________
	 * | 	  hour 0	   |
	 * |___________________|
	 * 0   				  3600
	 * 
	 * hour 0 = bins 0,1,2,3
	 * hour 1 = bins 4,5,6,7
	 * ...
	 * 
	 * getTimeSlotIndex = (int)time / this.timeBinSize => jumps at 3600.0!
	 * Thus, starting time = (hour = 0) * 3600.0
	 */
//	public double[] getVolumesPerHourForLink(final Id<Link> linkId) {
//		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
//		
//		double [] volumes = new double[24];
//		for (int hour = 0; hour < 24; hour++) {
//			volumes[hour] = 0.0;
//		}
//		
//		int[] volumesForLink = this.getVolumesForLink(linkId);
//		if (volumesForLink == null) return volumes;
//
//		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
//		for (int hour = 0; hour < 24; hour++) {
//			double time = hour * 3600.0;
//			for (int i = 0; i < slotsPerHour; i++) {
//				volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
//				time += this.timeBinSize;
//			}
//		}
//		return volumes;
//	}

//	public double[] getVolumesPerHourForLink(final Id<Link> linkId, String mode) {
//		if (observeModes) {
//			if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
//			
//			double [] volumes = new double[24];
//			for (int hour = 0; hour < 24; hour++) {
//				volumes[hour] = 0.0;
//			}
//			
//			int[] volumesForLink = this.getVolumesForLink(linkId, mode);
//			if (volumesForLink == null) return volumes;
//	
//			int slotsPerHour = (int)(3600.0 / this.timeBinSize);
//			for (int hour = 0; hour < 24; hour++) {
//				double time = hour * 3600.0;
//				for (int i = 0; i < slotsPerHour; i++) {
//					volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
//					time += this.timeBinSize;
//				}
//			}
//			return volumes;
//		}
//		return null;
//	}
	
	/**
	 * @return Set of Strings containing all modes for which counting-values are available.
	 */
//	public Set<String> getModes() {
//		Set<String> modes = new TreeSet<>();
//		
//		for (Map<String, int[]> map : this.linksPerMode.values()) {
//			modes.addAll(map.keySet());
//		}
//		
//		return modes;
//	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @return Set of Strings containing all link ids for which counting-values are available.
	 */
//	public Set<Id<Link>> getLinkIds() {
//		return this.links.keySet();
//	}

//	@Override
//	public void reset(final int iteration) {
//		this.links.clear();
//		if (observeModes) {
//			this.linksPerMode.clear();
//			this.enRouteModes.clear();
//		}
//	}
}