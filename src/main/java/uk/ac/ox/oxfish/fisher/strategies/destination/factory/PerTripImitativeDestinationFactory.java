package uk.ac.ox.oxfish.fisher.strategies.destination.factory;


import ec.util.MersenneTwisterFast;
import uk.ac.ox.oxfish.fisher.log.TripRecord;
import uk.ac.ox.oxfish.fisher.strategies.destination.FavoriteDestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.destination.PerTripIterativeDestinationStrategy;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.maximization.DefaultBeamHillClimbing;
import uk.ac.ox.oxfish.utility.maximization.ExplorationOrImitationMovement;
import uk.ac.ox.oxfish.utility.maximization.HillClimbingMovement;
import uk.ac.ox.oxfish.utility.maximization.IterativeMovement;
import uk.ac.ox.oxfish.utility.parameters.DoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;

/**
 * creates a trip strategy that has imitates friends when not exploring
 */
public class PerTripImitativeDestinationFactory implements AlgorithmFactory<PerTripIterativeDestinationStrategy>
{

    private DoubleParameter stepSize = new FixedDoubleParameter(5d);

    private DoubleParameter explorationProbability = new FixedDoubleParameter(0.8d);


    private boolean ignoreEdgeDirection = true;


    /**
     * Applies this function to the given argument.
     *
     * @param state the function argument
     * @return the function result
     */
    @Override
    public PerTripIterativeDestinationStrategy apply(FishState state) {

        MersenneTwisterFast random = state.random;
        NauticalMap map = state.getMap();


        final DefaultBeamHillClimbing algorithm = new DefaultBeamHillClimbing(stepSize.apply(random).intValue(),
                                                                              10);
        return new PerTripIterativeDestinationStrategy(
                new FavoriteDestinationStrategy(map, random), algorithm,explorationProbability.apply(random),1d);


    }

    public DoubleParameter getStepSize() {
        return stepSize;
    }

    public void setStepSize(DoubleParameter stepSize) {
        this.stepSize = stepSize;
    }

    public DoubleParameter getExplorationProbability() {
        return explorationProbability;
    }

    public void setExplorationProbability(DoubleParameter explorationProbability) {
        this.explorationProbability = explorationProbability;
    }

    public boolean isIgnoreEdgeDirection() {
        return ignoreEdgeDirection;
    }

    public void setIgnoreEdgeDirection(boolean ignoreEdgeDirection) {
        this.ignoreEdgeDirection = ignoreEdgeDirection;
    }
}
