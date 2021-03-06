package uk.ac.ox.oxfish.model.event;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ox.oxfish.biology.growers.SimpleLogisticGrowerFactory;
import uk.ac.ox.oxfish.biology.initializer.factory.DiffusingLogisticFactory;
import uk.ac.ox.oxfish.biology.initializer.factory.SingleSpeciesBoxcarFactory;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.scenario.FlexibleScenario;
import uk.ac.ox.oxfish.model.scenario.PrototypeScenario;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;

import static org.junit.Assert.*;

public class ExogenousInstantaneousMortalityCatchesTest {


    @Test
    public void biomassTest() {

        FlexibleScenario scenario = new FlexibleScenario();
        scenario.getFisherDefinitions().clear();

        ((DiffusingLogisticFactory) scenario.getBiologyInitializer()).
                setCarryingCapacity(new FixedDoubleParameter(1000));
        ((DiffusingLogisticFactory) scenario.getBiologyInitializer()).setMinInitialCapacity(new FixedDoubleParameter(1));
        ((DiffusingLogisticFactory) scenario.getBiologyInitializer()).setMaxInitialCapacity(new FixedDoubleParameter(1));
        ((SimpleLogisticGrowerFactory) ((DiffusingLogisticFactory) scenario.getBiologyInitializer()).getGrower()).setSteepness(new FixedDoubleParameter(0d));
        // work!
        FishState state = new FishState();
        state.setScenario(scenario);

        ExogenousInstantaneousMortalityCatchesFactory factory = new ExogenousInstantaneousMortalityCatchesFactory();
        factory.getExogenousMortalities().put("Species 0",.5);
        factory.setAbundanceBased(false);

        scenario.setExogenousCatches(factory);

        state.start();


        for (int i = 0; i < 363; i++) {
            state.schedule.step(state);
            Assert.assertEquals(
                    state.getMap().getSeaTile(2,2).getBiomass(state.getSpecies("Species 0")),
                    1000,.0001);
        }

        for (int i = 0; i < 30; i++) {
            state.schedule.step(state);

        }
        Assert.assertEquals(
                state.getMap().getSeaTile(2,2).getBiomass(state.getSpecies("Species 0")),
                606.5307,.0001);



    }



    @Test
    public void abundanceTest() {

        FlexibleScenario scenario = new FlexibleScenario();
        scenario.getFisherDefinitions().clear();


        final SingleSpeciesBoxcarFactory boxy = new SingleSpeciesBoxcarFactory();
        scenario.setBiologyInitializer(boxy);
           boxy.setInitialBtOverK(new FixedDoubleParameter(1));
        //   boxy.setYearlyMortality(new FixedDoubleParameter(0d));
        //   boxy.setK(new FixedDoubleParameter(0d));
        //  boxy.setSteepness(new FixedDoubleParameter(0.0001d));
        // work!
        FishState state = new FishState(0);
        state.setScenario(scenario);

        ExogenousInstantaneousMortalityCatchesFactory factory = new ExogenousInstantaneousMortalityCatchesFactory();
        factory.getExogenousMortalities().put("Red Fish",.5);
        factory.setAbundanceBased(true);


        state.start();


        //manual to get more control
        final ExogenousInstantaneousMortalityCatches catches = factory.apply(state);


        state.schedule.step(state);
        Assert.assertEquals(
                state.getMap().getSeaTile(2,2).getBiomass(state.getSpecies("Red Fish")),
                2266886,1);

    //    catches.start(state);
        catches.step(state);
        Assert.assertEquals(
                state.getMap().getSeaTile(2,2).getBiomass(state.getSpecies("Red Fish")),
                1374936,1);




    }
}