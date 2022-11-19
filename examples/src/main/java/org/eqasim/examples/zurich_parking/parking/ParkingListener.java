package org.eqasim.examples.zurich_parking.parking;

import com.google.inject.Inject;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ParkingListener implements StartParkingSearchEventHandler, LinkLeaveEventHandler,
        ActivityStartEventHandler, IterationEndsListener {

    private final Map<Id<Person>, Double> personIdParkingSearchStartTime = new HashMap<>();
    private final Map<Id<Person>, Coord> personIdParkingSearchStartCoord = new HashMap<>();
    private final Map<Id<Person>, Double> personIdParkingSearchTime = new HashMap<>();
    private final Map<Id<Person>, Double> personIdParkingSearchDistance = new HashMap<>();
    private final Map<Id<Person>, Double> personIdMaxSearchRadius = new HashMap<>();
    private final Map<Id<Person>, Double> personIdParkingTime = new HashMap<>();
    private final Map<Id<Person>, Coord> personIdParkingCoord = new HashMap<>();

    private final Map<Id<Node>, double[]> parkingSearchTimes = new HashMap<>();
    private final Map<Id<Node>, double[]> parkingSearchDistances = new HashMap<>();
    private final Map<Id<Node>, double[]> egressDistances = new HashMap<>();

    private final Map<Id<Node>, double[]> arrivalCount = new HashMap<>();

    private final Vehicle2DriverEventHandler vehicle2DriverEventHandler;
    private final RoadNetwork network;
    private final double startTime;
    private final double endTime;
    private final double interval;
    private final int numberOfBins;
    private double minQueryRadius;

    @ Inject
    public ParkingListener(Vehicle2DriverEventHandler vehicle2DriverEventHandler, RoadNetwork network,
                           double startTime, double endTime, double interval, double minQueryRadius) {
        this.vehicle2DriverEventHandler = vehicle2DriverEventHandler;

        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.numberOfBins = (int) Math.floor((endTime - startTime) / interval);
        this.network = network;
        this.minQueryRadius = minQueryRadius;

        for (Id<Node> nodeId : network.getNodes().keySet()) {
            parkingSearchTimes.putIfAbsent(nodeId, new double[numberOfBins]);
            parkingSearchDistances.putIfAbsent(nodeId, new double[numberOfBins]);
            egressDistances.putIfAbsent(nodeId, new double[numberOfBins]);
            arrivalCount.putIfAbsent(nodeId, new double[numberOfBins]);
        }
    }

    @Override
    public void handleEvent(StartParkingSearchEvent event) {
        double time = event.getTime();
        Coord coord = this.network.getLinks().get(event.getLinkId()).getCoord();
        Id<Person> personId = vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId());
        personIdParkingSearchStartTime.putIfAbsent(personId, time);
        personIdParkingSearchDistance.putIfAbsent(personId, 0.0);
        personIdParkingSearchStartCoord.putIfAbsent(personId, coord);
        personIdMaxSearchRadius.putIfAbsent(personId, 0.0);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId());
        if (personIdParkingSearchStartTime.containsKey(personId)) {
            double linkLength = this.network.getLinks().get(event.getLinkId()).getLength();
            double previousDistance = personIdParkingSearchDistance.get(personId);
            personIdParkingSearchDistance.put(personId, previousDistance + linkLength);

            // update max search radius
            Coord currentCoord = this.network.getLinks().get(event.getLinkId()).getToNode().getCoord();
            double maxSearchRadius = this.personIdMaxSearchRadius.get(personId);
            double currentSearchRadius = CoordUtils.calcEuclideanDistance(currentCoord, personIdParkingSearchStartCoord.get(personId));
            this.personIdMaxSearchRadius.put(personId, Math.max(maxSearchRadius, currentSearchRadius));
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        Id<Person> personId = event.getPersonId();

        // if person is interacting with parked vehicle
        if (event.getActType().equals(ParkingUtils.PARKACTIVITYTYPE)) {

            // check if person is in parking search phase
            if (personIdParkingSearchStartTime.containsKey(personId)) {
                double searchTime = event.getTime() - personIdParkingSearchStartTime.get(personId);
                personIdParkingSearchTime.put(personId, searchTime);

                // record the parking time
                personIdParkingTime.put(personId, event.getTime());

                // record the parking location
                Coord parkingCoord = network.getLinks().get(event.getLinkId()).getToNode().getCoord();
                personIdParkingCoord.put(personId, parkingCoord);
            }
        }
        // any other activity
        else {
            // check if person was previously searching for parking and parked
            if (personIdParkingSearchStartTime.containsKey(personId) &&
                    personIdParkingSearchTime.containsKey(personId)) {
                // extract relevant search variables
                double parkingSearchStartTime = personIdParkingSearchStartTime.remove(personId);
                double parkingSearchTime = personIdParkingSearchTime.remove(personId);
                double parkingSearchDistance = personIdParkingSearchDistance.remove(personId);
                Coord parkingSearchStartCoord = personIdParkingSearchStartCoord.remove(personId);

                // egress distance
                double egressDistance = CoordUtils.calcEuclideanDistance(parkingSearchStartCoord, personIdParkingCoord.remove(personId));

                // get query radius
                double maxSearchRadius = personIdMaxSearchRadius.remove(personId) + 1.0; // +1 to be sure to include node
                double queryRadius = Math.max(minQueryRadius, maxSearchRadius);

                // get affected nodes around parking search start coordinate within query radius
                Collection<Node> nodes = network.getNearestNodes(parkingSearchStartCoord, queryRadius);
                for (Node node : nodes) {
                    Id<Node> nodeId = node.getId();

                    // compute time bin
                    int timeBin = getTimeBin(startTime, parkingSearchStartTime, interval);

                    // population maps
                    parkingSearchTimes.get(nodeId)[timeBin] += parkingSearchTime;
                    parkingSearchDistances.get(nodeId)[timeBin] += parkingSearchDistance;
                    egressDistances.get(nodeId)[timeBin] += egressDistance;
                    arrivalCount.get(nodeId)[timeBin] += 1;
                }
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {

        // compute average values
        for (Id<Node> nodeId : arrivalCount.keySet()) {
            for (int i = 0; i<numberOfBins; i++) {
                if (arrivalCount.get(nodeId)[i] > 0) {
                    parkingSearchTimes.get(nodeId)[i] /= arrivalCount.get(nodeId)[i];
                    parkingSearchDistances.get(nodeId)[i] /= arrivalCount.get(nodeId)[i];
                    egressDistances.get(nodeId)[i] /= arrivalCount.get(nodeId)[i];
                }
            }
        }
    }

    @Override
    public void reset(int iteration) {
        // clear all person maps
        personIdParkingSearchStartTime.clear();
        personIdParkingSearchStartCoord.clear();
        personIdParkingSearchTime.clear();
        personIdParkingSearchDistance.clear();
        personIdMaxSearchRadius.clear();
        personIdParkingTime.clear();
        personIdParkingCoord.clear();

        // clear all node maps
        parkingSearchTimes.clear();
        parkingSearchDistances.clear();
        egressDistances.clear();
        arrivalCount.clear();

        for (Id<Node> nodeId : network.getNodes().keySet()) {
            parkingSearchTimes.putIfAbsent(nodeId, new double[numberOfBins]);
            parkingSearchDistances.putIfAbsent(nodeId, new double[numberOfBins]);
            egressDistances.putIfAbsent(nodeId, new double[numberOfBins]);
            arrivalCount.putIfAbsent(nodeId, new double[numberOfBins]);
        }
    }

    public Map<Id<Node>, double[]> getParkingSearchTimes() {
        return parkingSearchTimes;
    }

    public double[] getParkingSearchTimesAtCoord(Coord coord) {
        Id<Node> nodeId = network.getNearestNode(coord).getId();
        return parkingSearchTimes.get(nodeId);
    }

    public double getParkingSearchTimeAtCoordAtTime(Coord coord, double time) {
        int timeBin = getTimeBin(startTime, time, interval);
        return getParkingSearchTimesAtCoord(coord)[timeBin];
    }

    public double[] getParkingSearchDistancesAtCoord(Coord coord) {
        Id<Node> nodeId = network.getNearestNode(coord).getId();
        return parkingSearchDistances.get(nodeId);
    }

    public double getParkingSearchDistanceAtCoordAtTime(Coord coord, double time) {
        int timeBin = getTimeBin(startTime, time, interval);
        return getParkingSearchDistancesAtCoord(coord)[timeBin];
    }

    public double[] getEgressDistancesAtCoord(Coord coord) {
        Id<Node> nodeId = network.getNearestNode(coord).getId();
        return egressDistances.get(nodeId);
    }

    public double getEgressDistanceAtCoordAtTime(Coord coord, double time) {
        int timeBin = getTimeBin(startTime, time, interval);
        return getEgressDistancesAtCoord(coord)[timeBin];
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    private int getTimeBin(double startTime, double endTime, double interval) {
        // in case the end time is at the upper limit
        if (endTime >= this.endTime) {
            endTime = this.endTime - 1.0;
        }
        return (int) Math.floor((endTime - startTime) / interval);
    }

}
