package org.eqasim.switzerland.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissPersonPredictor extends CachedVariablePredictor<SwissPersonVariables> {

	@Override
	public SwissPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
										List<? extends PlanElement> elements) {
		int age_a = PredictorUtils.getAge(person);
		Coord homeLocation = SwissPredictorUtils.getHomeLocation(person);
		boolean hasGeneralSubscription = SwissPredictorUtils.hasGeneralSubscription(person);
		boolean hasHalbtaxSubscription = SwissPredictorUtils.hasHalbtaxSubscription(person);
		boolean hasRegionalSubscription = SwissPredictorUtils.hasRegionalSubscription(person);
		int statedPreferenceRegion = SwissPredictorUtils.getStatedPreferenceRegion(person);

		int age = (int) person.getAttributes().getAttribute("age");
		boolean isFemale = false;
		if (person.getAttributes().getAttribute("sex").equals("f")){
			isFemale = true ;
		}

		return new SwissPersonVariables(age_a, homeLocation,
				hasGeneralSubscription, hasHalbtaxSubscription, hasRegionalSubscription, statedPreferenceRegion,
				isFemale,age);
	}
}
