package org.eqasim.examples.corsica_carpooling.conflicts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.*;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DriverAgent {

    final private Id<Person> driverId;
    final private List<TripInfo> driverTrips;

    private int carCapacity = 2;

    public DriverAgent(Person p) {
        this.driverId = p.getId();
        this.driverTrips = new ArrayList<>();
    }

    public void addTrip(Trip t) {
        Objects.requireNonNull(t);
        driverTrips.add(new TripInfo(t));
    }


    public List<TripInfo> getDriverTrips() {
        return driverTrips;
    }

    public Id<Person> getDriverId() {
        return driverId;
    }

    public int getCarCapacity() {
        return carCapacity;
    }



    public boolean acceptTrip(Trip trip) {
        Coord originCoord = trip.getOriginActivity().getCoord();
        Coord destinationCoord = trip.getDestinationActivity().getCoord();
        double departureTime = trip.getLegsOnly().get(0).getDepartureTime().seconds();
        double tripDistance = trip.getLegsOnly().get(0).getRoute().getDistance();

        if (carCapacity == 0 || tripDistance < 2000) {
            return false;
        }
        for (TripInfo driverTrip : driverTrips) {
            double distDiffOrigin = CoordUtils.calcEuclideanDistance(driverTrip.originCoord, originCoord);
            double distDiffDestination = CoordUtils.calcEuclideanDistance(driverTrip.destinationCoord, destinationCoord);
            double departureTimeDiff = Math.abs((departureTime - driverTrip.departureTime));


            if (departureTimeDiff > 900) {
                return false;
            } else if (distDiffOrigin > 2000 && distDiffDestination > 2000) {
                return false;
            }
            carCapacity--;
            break;
        }
        return true;
    }



    @Override
    public String toString(){
        return("Driver Id " + this.driverId + "\n" + "Trips: " + driverTrips.toString());
    }

    static class TripInfo {
        private final String routingMode;
        private final Coord originCoord;
        private final Coord destinationCoord;
        private final double departureTime;

        public TripInfo(Trip t) {
            this.routingMode = TripStructureUtils.getRoutingMode(t.getLegsOnly().get(0));
            this.originCoord = t.getOriginActivity().getCoord();
            this.destinationCoord = t.getDestinationActivity().getCoord();
            this.departureTime = t.getLegsOnly().get(0).getDepartureTime().seconds();
        }

        @Override
        public String toString() {
            return(routingMode + " " + originCoord + " " + destinationCoord + " " + departureTime);
        }
    }
}
