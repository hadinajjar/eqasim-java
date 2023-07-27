package org.eqasim.examples.corsica_carpooling.conflicts;

import java.util.*;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictHandler;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.locationtech.jts.io.InStream;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;


public class CarpoolingConflictLogic implements ConflictLogic{

	//private final Random random = new Random(0);

	@Override
	public void run(Population population, ConflictHandler handler) {

		//get all routing modes of agents
		var driverAgents = new HashMap<Id<Person>, TripInfo>();
		for (Person p : population.getPersons().values()) {
			var pId = p.getId();
			for (Trip trip : TripStructureUtils.getTrips(p.getSelectedPlan())) {
				String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));
				if (routingMode.equals("car")){
					driverAgents.put(pId, new TripInfo(trip));
				}
			}
		}
		/*Printing the HashMap
		System.out.println("========================================================================");
		System.out.println("===========         PRINTING HASHMAP PROMPT           ==================");
		System.out.println("========================================================================");
		driverAgents.forEach((key, value) -> System.out.println("Driver Id " + key + " with info  " + value.toString()));*/



		for (Person person : population.getPersons().values()) {
			int tripIndex = 0;
			List<Double> distancesList = new ArrayList<>();
			List<Double> timesList = new ArrayList<>();
			List<Id<Person>> driversList = new ArrayList<>();
			Map<Id<Person>, List<Double>> tripsToTest = new HashMap<>();
			//System.out.println(person.getId());

			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				var personTripInfo = new TripInfo(trip);
				String routingMode = personTripInfo.getRoutingMode();
				Coord originActivityCoord = personTripInfo.getOriginActivityCoord();
				Coord destinationActivityCoord = personTripInfo.getDestinationActivityCoord();



				//System.out.println("Trip for person "+ person.getId() + tripIndex + trip.toString());

				if (routingMode.equals("carpooling")) {

					/*-
					 * This is a carpooling leg. Do we find a matching "car" leg from another person?
					 * If not, we can reject the plan and let the agent re-plan again.
					 */
					for (Map.Entry<Id<Person>, TripInfo> p2 : driverAgents.entrySet()) {
						var driverId = p2.getKey();
						var driverTrip = p2.getValue();
						var distanceOrigin = CoordUtils.calcEuclideanDistance(originActivityCoord, driverTrip.originActivityCoord);
						var distanceDestination = CoordUtils.calcEuclideanDistance(destinationActivityCoord, driverTrip.destinationActivityCoord);
						var totalDistance = distanceOrigin + distanceDestination;
						double totalTime = Math.abs(((personTripInfo.getDepartureTime() - driverTrip.getDepartureTime())));

						timesList.add(totalTime);
						distancesList.add(totalDistance);
						driversList.add(driverId);
						/*System.out.println("[TEST] Person 1 " + personTripInfo);
						System.out.println("[TEST] Person 2 " + p2);*/
					}
					// This is working fine
					Iterator<Double> distancesIterator = distancesList.iterator();
					Iterator<Double> timeDifIterator = timesList.iterator();
					Iterator<Id<Person>> idIterator = driversList.iterator();
					while(distancesIterator.hasNext() && timeDifIterator.hasNext() && idIterator.hasNext()) {
						tripsToTest.put(idIterator.next(),
								Arrays.asList(distancesIterator.next(), timeDifIterator.next()));
					}



					int finalTripIndex = tripIndex;
					Optional<Map.Entry<Id<Person>, List<Double>>> matchingEntry = tripsToTest.entrySet().stream()
							.filter(entry -> {
								var dist2Time = entry.getValue();
								var distanceDiff = dist2Time.get(0);
								var timeDiff = dist2Time.get(1);
								return !(distanceDiff > 3000 || timeDiff > 1800);
							})
							.findFirst();

					matchingEntry.ifPresent(entry -> {
						var driverId = entry.getKey();
						var dist2Time = entry.getValue();
						var distanceDiff = dist2Time.get(0);
						var timeDiff = dist2Time.get(1);

						System.out.println("Person " + person.getId() + " found a match with Driver " + driverId +
								" for trip " + finalTripIndex +
								"\n" + "Distance difference = " + distanceDiff + " Time difference = " + timeDiff);
					});

					if (!matchingEntry.isPresent()) {
						handler.addRejection(person.getId(), finalTripIndex, routingMode);
					}


					/*int finalTripIndex = tripIndex;
					tripsToTest.entrySet().forEach(entry -> {
						var driverId = entry.getKey();
						var dist2Time = entry.getValue();
						var distanceDiff = dist2Time.get(0);
						var timeDiff = dist2Time.get(1);

						if (distanceDiff > 3000 || timeDiff > 1800) {
							handler.addRejection(person.getId(), finalTripIndex, routingMode);
						} else {
							System.out.println("Person " + person.getId() + " found a match with Driver " + driverId +
											" for trip " + finalTripIndex +
									"\n" + "Distance difference = " + distanceDiff + " Time difference = " + timeDiff);
						}


					});*/


					/*var minDistance = distancesList.stream()
							.mapToDouble(v -> v)
							.min().orElseThrow(NoSuchElementException::new);

					double minTime = timesList.stream()
							.mapToDouble(v -> v)
							.min().orElseThrow(NoSuchElementException::new);

					System.out.println("Minimum time split found is : " + minTime);*/

					/*if (minDistance > 3000 || minTime > 1800) {
						handler.addRejection(person.getId(), tripIndex, routingMode);
						//System.out.println("Person " + person.getId());
					}*/

					//System.out.println("Person " + person.getId() + " is accepted");
				}
				tripIndex++;
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
}