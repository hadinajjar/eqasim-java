package org.eqasim.examples.idf_carpooling.conflicts;

import org.locationtech.jts.geom.Geometry;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;


import java.util.Objects;

public class TripFilter {

    private final String dptCode;
    private final String shapeFilePath;
    private Geometry dptGeometry;

    public TripFilter(String dpt, String path) {
        this.dptCode = dpt;
        this.shapeFilePath = path;
    }

    public void loadDptGeometry() {
        var features = ShapeFileReader.getAllFeatures(shapeFilePath);
        dptGeometry = features.stream()
                .filter(feature -> feature.getAttribute("code_dept").equals(dptCode))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .findAny()
                .orElseThrow();
    }

    public boolean isInDepartment(Trip t) {
        Objects.requireNonNull(dptGeometry);
        var originCoord = t.getOriginActivity().getCoord();
        var destCoord = t.getDestinationActivity().getCoord();
        if (dptGeometry.covers(MGC.coord2Point(originCoord)) && dptGeometry.covers(MGC.coord2Point(destCoord))) {
            return true;
        } else {
            return false;
        }
    }

}
