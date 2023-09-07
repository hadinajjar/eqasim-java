package org.eqasim.examples.idf_carpooling.conflicts;


import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictHandler;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class CarpoolingConflictLogic implements ConflictLogic{

	private final Network network;

	private final Random random = new Random(0);
	private final List<DriverAgent> driverAgentList = new ArrayList<>();

	private TripFilter parisFilter = new TripFilter("75", "C:\\Users\\mnajjar\\IdeaProjects\\eqasim-java\\gis\\idf_shape.shp");

	public CarpoolingConflictLogic(Network net) {
		this.network = net;
	}


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


		//prevent trips inside Paris to do carpooling
		parisFilter.loadDptGeometry();

		for (Person person : population.getPersons().values()) {
			int tripIndex = 0;
			var personAge = (int) person.getAttributes().getAttribute("age");

			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));

				if (routingMode.equals("carpooling")) {
					int finalTripIndex = tripIndex;

					if (personAge < 18) {
						handler.addRejection(person.getId(), tripIndex, routingMode);
						continue;
					}

					if (parisFilter.isInDepartment(trip)) {
						handler.addRejection(person.getId(), tripIndex, routingMode);
						System.out.println(trip.getOriginActivity().getCoord() +" , " + trip.getDestinationActivity().getCoord() + " refused in Paris");
						continue;
					}

					boolean foundMatch = driverAgentList.stream()
							.anyMatch(currentDriver -> {
								if (currentDriver.acceptTrip(trip)) {
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