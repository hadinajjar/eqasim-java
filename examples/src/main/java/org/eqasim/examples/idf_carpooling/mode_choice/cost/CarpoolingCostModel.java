package org.eqasim.examples.idf_carpooling.mode_choice.cost;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.idf_carpooling.mode_choice.parameters.CarpoolingCostParameters;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
public class CarpoolingCostModel extends AbstractCostModel{

    private final CarpoolingCostParameters parameters;

    public CarpoolingCostModel(CarpoolingCostParameters parameters) {
        super("carpooling");
        this.parameters = parameters;
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double tripDistance_km = getInVehicleDistance_km(elements);
        return parameters.carpoolingCost_EUR_km * tripDistance_km;
    }
}
