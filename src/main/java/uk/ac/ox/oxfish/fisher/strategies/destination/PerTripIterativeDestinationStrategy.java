package uk.ac.ox.oxfish.fisher.strategies.destination;

import com.google.common.base.Preconditions;
import ec.util.MersenneTwisterFast;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.actions.Action;
import uk.ac.ox.oxfish.fisher.selfanalysis.HourlyProfitInTripFunction;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.utility.maximization.*;

/**
 * Like the YearlyIterativeDestinationStrategy except that rather than doing it every
 * year this is done every x trips (x=1 by default). <br>
 *     In terms of code this strategy doesn't actually step but instead listen to the fisher for
 *     new trips
 * Created by carrknight on 6/19/15.
 */
public class PerTripIterativeDestinationStrategy implements DestinationStrategy {






    /**
     * should we not study trips that were cut short?
     */
    private boolean ignoreFailedTrips = false;


    private final Adaptation<SeaTile> algorithm;

    /**
     * this strategy works by modifying the "favorite" destination of its delegate
     */
    private final FavoriteDestinationStrategy delegate;

    /**
     * fisher I am listening to
     */
    private Fisher fisher;


    public PerTripIterativeDestinationStrategy(
            FavoriteDestinationStrategy delegate,
            AdaptationAlgorithm<SeaTile> algorithm,
            double randomizationProbability,
            double imitationProbability) {
        this.delegate = delegate;
        this.algorithm = new Adaptation<SeaTile>(
                fisher -> !(ignoreFailedTrips && fisher.getLastFinishedTrip().isCutShort()),
                algorithm,
                (fisher, change, model) -> delegate.setFavoriteSpot(change),
                fisher1 -> {
                    if(fisher1==fisher) //if we are sensing ourselves
                        //override to delegate
                        return delegate.getFavoriteSpot();
                    else
                    if(fisher1.getLastFinishedTrip() == null || !fisher1.getLastFinishedTrip().isCompleted() ||
                            fisher1.getLastFinishedTrip().getTilesFished().isEmpty())
                        return  null;
                    else
                        return fisher1.getLastFinishedTrip().getTilesFished().iterator().next();
                },
                new HourlyProfitInTripFunction(),randomizationProbability, imitationProbability);

    }



    /**
     * tell the startable to turnoff,
     */
    @Override
    public void turnOff() {
        delegate.turnOff();
    }

    /**
     * ignored
     */
    @Override
    public void start(FishState model)
    {
        delegate.start(model);
    }





    /**
     * decides where to go.
     *
     * @param fisher        the agent that needs to choose
     * @param random        the randomizer. It probably comes from the fisher but I make explicit it might be needed
     * @param model         the model link
     * @param currentAction what action is the fisher currently taking that prompted to check for destination   @return the destination
     */
    @Override
    public SeaTile chooseDestination(
            Fisher fisher, MersenneTwisterFast random, FishState model, Action currentAction) {
        if(this.fisher == null)
        {
            this.fisher = fisher;
            fisher.addPerTripAdaptation(algorithm);
        }
        else
        {
            Preconditions.checkArgument(fisher==this.fisher);
        }
        return delegate.chooseDestination(fisher, random, model, currentAction);
    }


    public Adaptation<SeaTile> getAlgorithm() {
        return algorithm;
    }
}
