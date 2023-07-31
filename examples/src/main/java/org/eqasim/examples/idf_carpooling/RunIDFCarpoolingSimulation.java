package org.eqasim.examples.idf_carpooling;

import com.google.common.io.Resources;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.idf_carpooling.conflicts.CarpoolingConflictLogic;
import org.eqasim.examples.idf_carpooling.mode_choice.CarpoolingModeAvailability;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictModule;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class RunIDFCarpoolingSimulation {

    static public void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .allowPrefixes("mode-parameters", "cost-parameter")
                .build();

        URL configUrl = Resources.getResource("idf/ile_de_france_config.xml");

        IDFConfigurator configurator = new IDFConfigurator();
        Config config = ConfigUtils.loadConfig(configUrl, configurator.getConfigGroups());

        config.controler().setLastIteration(30);
        config.qsim().setFlowCapFactor(1e9);
        config.qsim().setStorageCapFactor(1e9);
        cmd.applyConfiguration(config);

        {
            DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

            dmcConfig.setModeAvailability(CarpoolingModeAvailability.NAME);

            Set<String> cachedModes = new HashSet<>();
            cachedModes.addAll(dmcConfig.getCachedModes());
            cachedModes.add("carpooling");
            dmcConfig.setCachedModes(cachedModes);

            EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
            eqasimConfig.setCostModel("carpooling", "carpooling");
            eqasimConfig.setEstimator("carpooling", "carpooling");

            eqasimConfig.setAnalysisInterval(1);

            Set<String> networkModes = new HashSet<>(config.plansCalcRoute().getNetworkModes());
            networkModes.add("carpooling");
            config.plansCalcRoute().setNetworkModes(networkModes);


        }

        {
            ModeParams modeParams = new ModeParams("carpooling");
            config.planCalcScore().addModeParams(modeParams);
        }



        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        new CarpoolingConfigurator().configureNetwork(scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);
        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new IDFModeChoiceModule(cmd));


        {
            controller.addOverridingModule(new CarpoolingModule(cmd));
        }

        controller.addOverridingModule(new ConflictModule());
        ConflictModule.configure(DiscreteModeChoiceConfigGroup.getOrCreate(config));

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(ConflictLogic.class).toInstance(new CarpoolingConflictLogic());
            }
        });

        controller.run();







    }
}
