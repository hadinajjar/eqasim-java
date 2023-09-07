package org.eqasim.examples.idf_carpooling.analysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.matsim.core.router.TripStructureUtils.Trip;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DptPlansExtractor {

    //private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:3857", "EPSG:27572");


    public static void main(String[] args) {

        var features = ShapeFileReader.getAllFeatures("C:\\Users\\mnajjar\\IdeaProjects\\eqasim-java\\gis\\idf_shape.shp");
        var population = PopulationUtils.readPopulation("C:\\Users\\mnajjar\\IdeaProjects\\eqasim-java\\simulation_output\\output_plans.xml.gz");
        //var network = NetworkUtils.readNetwork("C:\\Users\\mnajjar\\IdeaProjects\\eqasim-java\\simulation_output\\output_network.xml");
        var dep75 = getDeparmentGeometry("75", features);
        var dep92 = getDeparmentGeometry("92", features);
        var dep93 = getDeparmentGeometry("93", features);
        var dep94 = getDeparmentGeometry("94", features);
        var dep95 = getDeparmentGeometry("95", features);
        var dep77 = getDeparmentGeometry("77", features);
        var dep78 = getDeparmentGeometry("78", features);
        var dep91 = getDeparmentGeometry("91", features);
        Map<Id<Person>, Map<Integer, String>> personToTrips = new HashMap<>();


        for (var person : population.getPersons().values()) {
            var tripsMap = new HashMap<Integer, String>();
            int tripIndex = 0;
            var personId = person.getId();
            for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
                var tripOrigin = trip.getOriginActivity().getCoord();
                var tripDestination = trip.getDestinationActivity().getCoord();
                if ((isInDeparmtent(tripOrigin, dep95) || isInDeparmtent(tripOrigin, dep91))
                        && (isInDeparmtent(tripDestination, dep95) || isInDeparmtent(tripDestination, dep91))) {
                    String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));
                    tripsMap.put(tripIndex, routingMode);
                }
                tripIndex++;
            }
            personToTrips.put(personId, tripsMap);
        }
        writeMapToCSV(personToTrips, "95_entre_91.csv");
    }

    private static Geometry getDeparmentGeometry(String identifier, Collection<SimpleFeature> features) {
        return features.stream()
                .filter(feature -> feature.getAttribute("code_dept").equals(identifier))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .findAny()
                .orElseThrow();
    }

    private static boolean isInDeparmtent(Coord coord, Geometry geometry) {
       // var transformed = transformation.transform(coord);
        return geometry.covers(MGC.coord2Point(coord));
    }

    public static void writeMapToCSV(Map<Id<Person>, Map<Integer, String>> data, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the CSV header
            writer.append("PersonId;TripId;Mode\n");

            // Iterate through the map and write the data
            for (Map.Entry<Id<Person>, Map<Integer, String>> entry : data.entrySet()) {
                Id<Person> personId = entry.getKey();
                Map<Integer, String> tripData = entry.getValue();

                for (Map.Entry<Integer, String> tripEntry : tripData.entrySet()) {
                    Integer tripId = tripEntry.getKey();
                    String mode = tripEntry.getValue();
                    // Write the data row
                    writer.append(personId.toString()).append(";").append(tripId.toString()).append(";").append(mode).append("\n");
                }
            }
            // Flush and close the writer
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

