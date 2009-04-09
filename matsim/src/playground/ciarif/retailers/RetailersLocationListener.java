/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesLoadCalculator.java.java
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

package playground.ciarif.retailers;

/**
 *
 *
 * @author ciarif
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.utils.collections.QuadTree;


public class RetailersLocationListener implements StartupListener, BeforeMobsimListener {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_POP_SUM_TABLE = "populationSummaryTable";
	public final static String CONFIG_RET_SUM_TABLE = "retailersSummaryTable";
	public final static String CONFIG_RETAILERS = "retailers";
	
	private Retailers retailers;
	private RetailersSummaryWriter rs = null;
	private PlansSummaryTable pst = null;
	private MakeATableFromXMLFacilities txf = null;
	private LinksRetailerReader lrr = null;
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	private PlansCalcRoute pcrl = null;
	private String facilityIdFile = null;
	private Object[] links = null;
	
	public RetailersLocationListener() {
	}

	public void notifyStartup(StartupEvent event) {

		Controler controler = event.getControler();
		preprocess.run(controler.getNetwork());
		pcrl = new PlansCalcRoute(controler.getNetwork(),timeCostCalc, timeCostCalc, new AStarLandmarksFactory(preprocess));
		String popOutFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_POP_SUM_TABLE);
		if (popOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_POP_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		this.pst = new PlansSummaryTable (popOutFile);
		this.lrr = new LinksRetailerReader (controler);
		this.links = lrr.ReadLinks(); 
		this.txf = new MakeATableFromXMLFacilities("output/facilities_table2.txt");
		FacilitiesImpl facs = (FacilitiesImpl) controler.getFacilities();
		txf.write(facs);
		String retailersOutFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_RET_SUM_TABLE);
		if (retailersOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RET_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		this.rs = new RetailersSummaryWriter (retailersOutFile);
		this.facilityIdFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_RETAILERS);
		if (this.facilityIdFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RETAILERS+" in module = "+CONFIG_GROUP+" not defined!");
		}
		else {
			try {
				this.retailers = new Retailers();
				FileReader fr = new FileReader(this.facilityIdFile);
				BufferedReader br = new BufferedReader(fr);
				// Skip header
				String curr_line = br.readLine();
				while ((curr_line = br.readLine()) != null) {
					String[] entries = curr_line.split("\t", -1);
					// header: r_id  f_id  strategy
					// index:     0     1      2
					Id rId = new IdImpl(entries[0]);
					if (this.retailers.getRetailers().containsKey(rId)) { // retailer exists already
						Id fId = new IdImpl (entries[1]);
						Facility f = controler.getFacilities().getFacilities().get(fId);
						this.retailers.getRetailers().get(rId).addFacility(f);
					}
					else { // retailer does not exists yet
						Retailer r = new Retailer(rId, null);
						System.out.println("The strategy " + entries[2] + " will be added to the retailer = " + rId);
						r.addStrategy(controler, entries[2], this.links);
						Id fId = new IdImpl (entries[1]);
						Facility f = controler.getFacilities().getFacilities().get(fId);
						r.addFacility(f);
						this.retailers.addRetailer(r);
					}
				}
			} 
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		} 
		Utils.setPersonQuadTree(this.createPersonQuadTree(controler));
	}
	
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		//System.out.println("Veryfing if the retailers need to be relocated " + controler.getIteration()%5);
		if (controler.getIteration()%1==0) {
			Map<Id,Facility> movedFacilities = new TreeMap<Id,Facility>();
			
			// works, but it is not nicely programmed. shouldn't be a global container, should be
			// controlled by the controler (or actually added to the population)
			
			Utils.setFacilityQuadTree(this.createFacilityQuadTree(controler));
			
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
			
			for (Retailer r : this.retailers.getRetailers().values()) {
				Map<Id,Facility> facs = r.runStrategy();
				movedFacilities.putAll(facs); //fc TODO this is not true!!!! Only some of this facilities will really be moved!!!!!!!!!!! 
				// probably is only slower and not incorrect but should be changed
				System.out.println("moved facilities =" + facs);
			}
			
			int iter = controler.getIteration();
			this.rs.write(this.retailers);
			
			for (Person p : controler.getPopulation().getPersons().values()) {
				pst.run(p,iter);
				for (Plan plan : p.getPlans()) {
					
					boolean routeIt = false;
					Iterator<?> actIter = plan.getIteratorAct();
					while (actIter.hasNext()) {
						
						Activity act = (Activity)actIter.next();
						if (movedFacilities.containsKey(act.getFacilityId())) {
							act.setLink(act.getFacility().getLink());
							routeIt = true;
						}
					}
					if (routeIt) {
						pcrl.run(plan);
					}
				}
			}
		}
	}	
	
	private final QuadTree<Person> createPersonQuadTree(Controler controler) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		//ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>();
		for (Facility f : controler.getFacilities().getFacilities().values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		
		QuadTree<Person> personQuadTree = new QuadTree<Person>(minx, miny, maxx, maxy);
		for (Person p : controler.getPopulation().getPersons().values()) {
			Coord c = p.getSelectedPlan().getFirstActivity().getFacility().getCoord();
			personQuadTree.put(c.getX(),c.getY(),p);
		}
		return personQuadTree;
	}
	
	private final QuadTree<Facility> createFacilityQuadTree(Controler controler) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		//ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>();
		for (Link l : controler.getNetwork().getLinks().values()) {
			if (l.getCoord().getX() < minx) { minx = l.getCoord().getX(); }
			if (l.getCoord().getY() < miny) { miny = l.getCoord().getY(); }
			if (l.getCoord().getX() > maxx) { maxx = l.getCoord().getX(); }
			if (l.getCoord().getY() > maxy) { maxy = l.getCoord().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		
		QuadTree<Facility> facilityQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (Facility f : controler.getFacilities().getFacilities().values()) {
			Coord c = f.getCoord();
			facilityQuadTree.put(c.getX(),c.getY(),f);
		}
		return facilityQuadTree;
	}
}
