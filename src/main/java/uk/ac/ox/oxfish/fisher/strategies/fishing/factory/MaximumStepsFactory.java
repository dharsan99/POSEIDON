package uk.ac.ox.oxfish.fisher.strategies.fishing.factory;

import uk.ac.ox.oxfish.fisher.strategies.fishing.MaximumDaysStrategy;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.parameters.DoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;

/**
 * The factory of MaximumDaysStrategy. For the factory instead of focusing on steps I focus on days
 * Created by carrknight on 6/23/15.
 */
public class MaximumStepsFactory implements AlgorithmFactory<MaximumDaysStrategy>
{

    /**
     * how many DAYS (not steps) after which the fisher refuses to fish
     */
    private DoubleParameter daysAtSea = new FixedDoubleParameter(10);


    /**
     * Applies this function to the given argument.
     *
     * @param state the function argument
     * @return the function result
     */
    @Override
    public MaximumDaysStrategy apply(FishState state) {

        int steps = (int) Math.round(daysAtSea.apply(state.random) * state.getStepsPerDay());

        return new MaximumDaysStrategy(steps);

    }

    public DoubleParameter getDaysAtSea() {
        return daysAtSea;
    }

    public void setDaysAtSea(DoubleParameter daysAtSea) {
        this.daysAtSea = daysAtSea;
    }
}
