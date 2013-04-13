package playground.pieter.singapore.utils.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.singapore.utils.Sample;
import playground.pieter.singapore.utils.events.listeners.TrimEventsWithPersonIds;

public class EventsStripper {

	private class FindTransitDriverIdsFromVehicleIds implements
			PersonEntersVehicleEventHandler {
		HashSet<String> transitDriverIds = new HashSet<String>();
		HashSet<String> transitVehicleIds;

		public FindTransitDriverIdsFromVehicleIds(
				HashSet<String> transitVehicleIds) {
			this.transitVehicleIds = transitVehicleIds;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			String driver = event.getPersonId().toString();
			String car = event.getVehicleId().toString();
			for (String vehId : transitVehicleIds) {
				if (car.equals(vehId))
					transitDriverIds.add(driver);

			}

		}

		public HashSet<String> getTransitDriverIds() {
			return transitDriverIds;
		}
	}

	String[] choiceSet;
	private EventsManager events;
	Scenario scenario = ScenarioUtils
			.createScenario(ConfigUtils.createConfig());

	public EventsStripper(List<String> ids) {

		this.populateList(ids);
	}

	public EventsStripper(String plansFile) {
		this.populateList(plansFile);
	}

	private void populateList(List<String> ids) {

		choiceSet = new String[ids.size()];
		for (int i = 0; i < choiceSet.length; i++) {
			choiceSet[i] = ids.get(i).toString();
		}

		scenario = null;
	}

	private void populateList(String plansFile) {
		MatsimPopulationReader pn = new MatsimPopulationReader(scenario);
		pn.readFile(plansFile);
		ArrayList<Id> ids = new ArrayList<Id>();
		CollectionUtils.addAll(ids, scenario.getPopulation().getPersons()
				.keySet().iterator());
		choiceSet = new String[ids.size()];
		for (int i = 0; i < choiceSet.length; i++) {
			choiceSet[i] = ids.get(i).toString();
		}
	}

	public void stripEvents(String inFileName, String outfileName,
			double frequency, boolean listenForTransitDrivers) {
		this.events = EventsUtils.createEventsManager();
		int N = choiceSet.length;
		int M = (int) ((double) N * frequency);
		HashSet<String> sampledIds = new HashSet<String>();
		for (int i : Sample.sampleMfromN(M, N)) {
			sampledIds.add(choiceSet[i]);
		}
		TrimEventsWithPersonIds filteredWriter = new TrimEventsWithPersonIds(
				outfileName, sampledIds, listenForTransitDrivers);
		events.addHandler(filteredWriter);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inFileName);
		filteredWriter.closeFile();
		if (listenForTransitDrivers
				&& filteredWriter.getTransitVehicleIds() != null) {
			FindTransitDriverIdsFromVehicleIds transitDriverFinder = new FindTransitDriverIdsFromVehicleIds(
					filteredWriter.getTransitVehicleIds());
			events = EventsUtils.createEventsManager();
			events.addHandler(transitDriverFinder);
			reader = new EventsReaderXMLv1(events);
			reader.parse(inFileName);

			sampledIds.addAll(transitDriverFinder.transitDriverIds);
			events = EventsUtils.createEventsManager();
			filteredWriter = new TrimEventsWithPersonIds(outfileName,
					sampledIds, false);
			events.addHandler(filteredWriter);
			reader = new EventsReaderXMLv1(events);
			reader.parse(inFileName);
			filteredWriter.closeFile();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ArrayList<String> ids = new ArrayList<String>();
		ids.add("4101962"); //transit user
		ids.add("77878"); //car user
		EventsStripper stripper = new EventsStripper(ids);
		stripper.stripEvents(args[1], args[2], 1.0, true);
	}

}
