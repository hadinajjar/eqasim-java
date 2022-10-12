package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class BlueZoneParking implements ParkingFacility {

    private final String parkingType = ParkingFacilityType.BlueZone.toString();
    private final double maxParkingDuration = 3600;

    private final Coord coordParking;

    public BlueZoneParking(Coord coordParking) {
        this.coordParking = coordParking;
    }

    @Override
    public String getParkingType() {
        return parkingType;
    }

    @Override
    public double getMaxParkingDuration() {
        return maxParkingDuration;
    }

    // free to park in blue zones
    @Override
    public double getParkingCost(double startTime, double endTime) {
        return 0;
    }

    // blue zones have specific time restrictions
    @Override
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId) {
        // round up start time to nearest half-hour
        double roundTimeWindow = 30 * 60.0;
        startTime = Math.ceil(startTime / roundTimeWindow) * roundTimeWindow;

        // max time limit by default is one hour
        double mustLeaveByTime = startTime + maxParkingDuration;

        // however, there are exceptions

        // early morning case, i.e., before 8:00
        if (startTime < 8 * 3600.0) {
            mustLeaveByTime = 9 * 3600.0; // must leave by 9:00
        }
        // afternoon case, i.e., between 11:30 and 13:30
        else if (startTime >= 11.5 * 3600.0 & startTime < 13.5 * 3600.0) {
            mustLeaveByTime = 14.5 * 3600.0; // must leave by 14:30
        }
        // evening case, i.e., after 18:00
        else if (startTime >= 18*3600.0) {
            mustLeaveByTime = (9 + 24) * 3600.0; // must leave by 9:00 the next day
        }

        return !(endTime > mustLeaveByTime);
    }

    @Override
    public Coord getCoord() {
        return coordParking;
    }

}
