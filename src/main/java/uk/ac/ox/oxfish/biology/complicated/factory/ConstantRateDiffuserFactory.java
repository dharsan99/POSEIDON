package uk.ac.ox.oxfish.biology.complicated.factory;

import uk.ac.ox.oxfish.biology.complicated.ConstantRateAbundanceDiffuser;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.parameters.DoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;

/**
 * Created by carrknight on 7/8/17.
 */
public class ConstantRateDiffuserFactory
        implements AlgorithmFactory<ConstantRateAbundanceDiffuser> {


    /**
     * % of differential that moves from here to there
     */
    private final DoubleParameter diffusingRate = new FixedDoubleParameter(.001);
    /**
     * max distance in cells fish can move within a day
     */
    private final DoubleParameter diffusingRange = new FixedDoubleParameter(1);


    /**
     * Applies this function to the given argument.
     *
     * @param state the function argument
     * @return the function result
     */
    @Override
    public ConstantRateAbundanceDiffuser apply(FishState state) {
        return new ConstantRateAbundanceDiffuser(
                diffusingRange.apply(state.getRandom()).intValue(),
                diffusingRate.apply(state.getRandom())
        );
    }


    /**
     * Getter for property 'diffusingRate'.
     *
     * @return Value for property 'diffusingRate'.
     */
    public DoubleParameter getDiffusingRate() {
        return diffusingRate;
    }

    /**
     * Getter for property 'diffusingRange'.
     *
     * @return Value for property 'diffusingRange'.
     */
    public DoubleParameter getDiffusingRange() {
        return diffusingRange;
    }
}