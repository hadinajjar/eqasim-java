package org.eqasim.ile_de_france;

import com.google.common.io.Resources;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();

		URL configUrl = Resources.getResource("idf/ile_de_france_config.xml");

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(configUrl, configurator.getConfigGroups());
		//cmd.applyConfiguration(config);
		config.controler().setLastIteration(100);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));		
		
		controller.run();
	}
}