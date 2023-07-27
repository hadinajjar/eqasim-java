package org.eqasim.examples.corsica_carpooling.conflicts;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictHandler;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.CoordUtils;


public class CarpoolingConflictLogic implements ConflictLogic{

	//private final Random random = new Random(0);

	@Override
	public void run(Population population, ConflictHandler handler) {

		Map<Id<Person>, List<TripInfo>> driverAgents = new HashMap<>();

		for (Person p : population.getPersons().values()) {
			var pId = p.getId();
			List<TripInfo> tripsList = new ArrayList<>();
			for (Trip trip : TripStructureUtils.getTrips(p.getSelectedPlan())) {
				String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));
				if (routingMode.equals("car")) {
					tripsList.add(new TripInfo(trip));
					driverAgents.put(pId, tripsList);
				}
			}
		}

		/*
		var driverAgents = new HashMap<Id<Person>, TripInfo>();
		for (Person p : population.getPersons().values()) {
			var pId = p.getId();
			for (Trip trip : TripStructureUtils.getTrips(p.getSelectedPlan())) {
				String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));
				if (routingMode.equals("car")){
					driverAgents.put(pId, new TripInfo(trip));
				}
			}
		}*/






		for (Person person : population.getPersons().values()) {
			int tripIndex = 0;
			List<Double> distancesList = new ArrayList<>();
			List<Double> timesList = new ArrayList<>();
			Map<Id<Person>, List<List<Double>>> tripsToTest = new HashMap<>();

			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				var personTripInfo = new TripInfo(trip);
				String routingMode = personTripInfo.getRoutingMode();
				Coord originActivityCoord = personTripInfo.getOriginActivityCoord();
				Coord destinationActivityCoord = personTripInfo.getDestinationActivityCoord();

				if (routingMode.equals("carpooling")) {

					/*-
					 * This is a carpooling leg. Do we find a matching "car" leg from another person?
					 * If not, we can reject the plan and let the agent re-plan again.
					 */
					for (Map.Entry<Id<Person>, List<TripInfo>> driver : driverAgents.entrySet()) {
						var driverId = driver.getKey();
						var driverTripsList = driver.getValue();

						for (TripInfo singleDriverTrip : driverTripsList) {
							var distanceOrigin = CoordUtils.calcEuclideanDistance(originActivityCoord, singleDriverTrip.getOriginActivityCoord());
							var distanceDestination = CoordUtils.calcEuclideanDistance(destinationActivityCoord, singleDriverTrip.getDestinationActivityCoord());
							var totalDistance = distanceOrigin + distanceDestination;
							double totalTime = Math.abs(((personTripInfo.getDepartureTime() - singleDriverTrip.getDepartureTime())));
							timesList.add(totalTime);
							distancesList.add(totalDistance);

							List<List<Double>> combinedTripsList = new ArrayList<>();
							Iterator<Double> distancesListIterator = distancesList.iterator();
							Iterator<Double> timesListIterator = timesList.iterator();

							while (distancesListIterator.hasNext() && timesListIterator.hasNext()) {
								combinedTripsList.add(Arrays.asList(distancesListIterator.next(), timesListIterator.next()));
							}
							tripsToTest.put(driverId, combinedTripsList);
						}
					}

					boolean foundMatch = false;
					for (Map.Entry<Id<Person>, List<List<Double>>> entry : tripsToTest.entrySet()) {

						var driverTrips = entry.getValue();
						for (var driverTrip : driverTrips) {
							var distDiff = driverTrip.get(0);
							var timeDiff = driverTrip.get(1);

							if (distDiff > 3000 || timeDiff >1800) {
								handler.addRejection(person.getId(), tripIndex, routingMode);
							}
							else {
								/*System.out.println("Person " + person.getId() +", trip " +
										tripIndex + " found a match with " + "Driver " + driverId);*/
								foundMatch = true;
								break;
							}
						}
						if (foundMatch) {
							break;
						}
					}
				}
			tripIndex++;
			}
		}
	}
}

class TripInfo {
	final private String routingMode;
	final private Coord originActivityCoord;
	final private Coord destinationActivityCoord;

	final private double departureTime;

	public TripInfo(Trip t){
		this.routingMode = TripStructureUtils.getRoutingMode(t.getLegsOnly().get(0));
		this.originActivityCoord = t.getOriginActivity().getCoord();
		this.destinationActivityCoord = t.getDestinationActivity().getCoord();
		this.departureTime = t.getLegsOnly().get(0).getDepartureTime().seconds();

	}

	public String getRoutingMode(){
		return this.routingMode;
	}

	public Coord getOriginActivityCoord(){
		return this.originActivityCoord;
	}

	public Coord getDestinationActivityCoord() {
		return this.destinationActivityCoord;
	}

	public double getDepartureTime() {
		return this.departureTime;
	}

	@Override
	public String toString(){
		return("DriverAgentInfo " + routingMode + " " + originActivityCoord + " " + destinationActivityCoord + " " + departureTime);
	}
}