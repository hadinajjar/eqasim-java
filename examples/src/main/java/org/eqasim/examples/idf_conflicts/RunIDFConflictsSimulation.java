package org.eqasim.examples.idf_conflicts;

import com.google.common.io.Resources;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictModule;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.vdf.VDFConfigGroup;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;

public class RunIDFConflictsSimulation {

    static public void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .allowPrefixes("mode-parameter", "cost-parameter")
                .build();

        IDFConfigurator configurator = new IDFConfigurator();
        configurator.getQSimModules().removeIf(m -> m instanceof EqasimTrafficQSimModule);

        URL configURL = Resources.getResource("idf/ile_de_france_config.xml");
        Config config = ConfigUtils.loadConfig(configURL, configurator.getConfigGroups());

        config.controler().setLastIteration(10);
        config.addModule(new VDFConfigGroup());

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);
        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new IDFModeChoiceModule(cmd));

        controller.addOverridingModule(new VDFModule());
        controller.addOverridingQSimModule(new VDFQSimModule());

        controller.addOverridingModule(new ConflictModule());
        ConflictModule.configure(DiscreteModeChoiceConfigGroup.getOrCreate(config));

        controller.addOverridingModule(new AbstractModule() {
                                           @Override
                                           public void install() {
                                               bind(ConflictLogic.class).toInstance(new AvoidTransitLogic());
                                           }
                                       }
        );
    }
}
