package uk.ac.ox.oxfish.demoes;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ox.oxfish.biology.Specie;
import uk.ac.ox.oxfish.biology.initializer.factory.IndependentLogisticFactory;
import uk.ac.ox.oxfish.fisher.strategies.destination.factory.PerTripImitativeDestinationFactory;
import uk.ac.ox.oxfish.fisher.strategies.destination.factory.PerTripIterativeDestinationFactory;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.network.EmptyNetworkBuilder;
import uk.ac.ox.oxfish.model.network.EquidegreeBuilder;
import uk.ac.ox.oxfish.model.scenario.PrototypeScenario;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;


public class FunctionalFriendsDemo {


    //one result I have is that exploration-imitation works better than exploration alone
    //as long as imitation probability is low and social networks not too big:
    @Test
    public void functionalFriends() throws Exception {


        long seed = System.currentTimeMillis();
        int stepsAlone = stepsItTook(Double.NaN,0,3500, seed);
        int stepsWithFewFriends = stepsItTook(.9,3,3500, seed);
        int stepsWithManyFriends = stepsItTook(.9,20,3500, seed);


        //the first is a little bit finicky. It would work better as an average of 5 runs, but it takes very long to perform
        Assert.assertTrue(stepsAlone + " ---- " + stepsWithFewFriends, stepsAlone > stepsWithFewFriends);
        Assert.assertTrue(stepsWithFewFriends + " ---- " + stepsWithManyFriends, stepsWithManyFriends >  stepsWithFewFriends);


    }


    public static int stepsItTook(
            double explorationProbability,
            int friends,
            int maxSteps, final long seed) {


        PrototypeScenario scenario = new PrototypeScenario();
        scenario.setBiologyInitializer(new IndependentLogisticFactory()); //skip migration which should make this faster.
        scenario.setFishers(300);
        if (friends == 0) {
            scenario.setNetworkBuilder(new EmptyNetworkBuilder());
            scenario.setDestinationStrategy(new PerTripIterativeDestinationFactory());
        } else {
            final EquidegreeBuilder networkBuilder = new EquidegreeBuilder();
            networkBuilder.setDegree(friends);
            scenario.setNetworkBuilder(networkBuilder);
            final PerTripImitativeDestinationFactory destinationStrategy = new PerTripImitativeDestinationFactory();
            destinationStrategy.setExplorationProbability(new FixedDoubleParameter(explorationProbability));
            destinationStrategy.setIgnoreEdgeDirection(false);
            scenario.setDestinationStrategy(destinationStrategy);
        }
        FishState state = new FishState(seed, 1);
        state.setScenario(scenario);
        state.start();
        Specie onlySpecie = state.getBiology().getSpecie(0);
        final double minimumBiomass = state.getTotalBiomass(
                onlySpecie) * .05; //how much does it take to eat 95% of all the fish?


        int steps = 0;
        for (steps = 0; steps < maxSteps; steps++) {
            state.schedule.step(state);
            if (state.getTotalBiomass(onlySpecie) <= minimumBiomass)
                break;
        }
        //   System.out.println(steps + " -- " + state.getTotalBiomass(onlySpecie));
        return steps;


    }
}