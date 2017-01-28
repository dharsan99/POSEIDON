package uk.ac.ox.oxfish.utility.bandit;

import ec.util.MersenneTwisterFast;
import org.junit.Test;
import uk.ac.ox.oxfish.model.data.Averager;
import uk.ac.ox.oxfish.model.data.ExponentialMovingAverage;
import uk.ac.ox.oxfish.model.data.IterativeAverage;
import uk.ac.ox.oxfish.model.data.MovingAverage;

import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Created by carrknight on 11/9/16.
 */
public class EpsilonGreedyBanditAlgorithmTest {


    @Test
    public void tenOptions() throws Exception
    {

        //option 10 is the best, you should pick it!
        MersenneTwisterFast random = new MersenneTwisterFast();
        EpsilonGreedyBanditAlgorithm bandit = new EpsilonGreedyBanditAlgorithm(
                new BanditAverage(10, IterativeAverage::new)
                , .2);
        for (int i = 0; i < 1000; i++) {
            int arm = bandit.chooseArm(random);
            double reward = random.nextGaussian() / 2 + arm;
            bandit.observeReward(reward, arm);
        }

        //now you should be playing most
        bandit.setExplorationProbability(0);
        assertEquals(9, bandit.chooseArm(random));

    }



    @Test
    public void tenOptionsEMA() throws Exception
    {

        //option 10 is the best, you should pick it!
        MersenneTwisterFast random = new MersenneTwisterFast();
        EpsilonGreedyBanditAlgorithm bandit = new EpsilonGreedyBanditAlgorithm(
                new BanditAverage(10, () -> new ExponentialMovingAverage<>(.8))
                , .2);
        for (int i = 0; i < 1000; i++) {
            int arm = bandit.chooseArm(random);
            double reward = random.nextGaussian() / 2 + arm;
            bandit.observeReward(reward, arm);
        }

        //now you should be playing most
        bandit.setExplorationProbability(0);
        assertEquals(9, bandit.chooseArm(random));

    }


    @Test
    public void tenOptionsMA() throws Exception
    {

        //option 10 is the best, you should pick it!
        MersenneTwisterFast random = new MersenneTwisterFast();
        EpsilonGreedyBanditAlgorithm bandit = new EpsilonGreedyBanditAlgorithm(
                new BanditAverage(10, () -> new MovingAverage<>(20))
                , .2);
        for (int i = 0; i < 1000; i++) {
            int arm = bandit.chooseArm(random);
            double reward = random.nextGaussian() / 2 + arm;
            bandit.observeReward(reward, arm);
        }

        //now you should be playing most
        bandit.setExplorationProbability(0);
        assertEquals(9, bandit.chooseArm(random));

    }
}