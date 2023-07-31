package org.eqasim.examples.corsica_carpooling.conflicts;


import java.util.*;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictHandler;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;


public class CarpoolingConflictLogic implements ConflictLogic{

	private final Random random = new Random(0);
	private final List<DriverAgent> driverAgentList = new ArrayList<>();


	@Override
	public void run(Population population, ConflictHandler handler) {

		//Generating Drivers
		for (Person person : population.getPersons().values()) {
			var driver = new DriverAgent(person);

			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));
				if (routingMode.equals("car")) {
					driver.addTrip(trip);
				}
			}
			if (driver.getDriverTrips().size() >= 1) {
				driverAgentList.add(driver);
			}
		}

		for (Person person : population.getPersons().values()) {
			int tripIndex = 0;

			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));

				if (routingMode.equals("carpooling")) {
					int finalTripIndex = tripIndex;
					boolean foundMatch = driverAgentList.stream()
							.anyMatch(currentDriver -> {
								if (currentDriver.acceptTrip(trip)) {
									System.out.println("Driver " + currentDriver.getDriverId() + " accepted trip " +
											finalTripIndex + " of Person " + person.getId());
									return true;
								}
								return false;
							});

					if (!foundMatch) {
						handler.addRejection(person.getId(), tripIndex, routingMode);
					}
				}
				tripIndex++;
			}
		}
	}
}