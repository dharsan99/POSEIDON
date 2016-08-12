package uk.ac.ox.oxfish.fisher.heatmap.regression.tripbased;

import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.heatmap.regression.numerical.GeographicalObservation;
import uk.ac.ox.oxfish.fisher.heatmap.regression.numerical.GeographicalRegression;
import uk.ac.ox.oxfish.fisher.log.TripRecord;
import uk.ac.ox.oxfish.fisher.selfanalysis.profit.ProfitFunction;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;

import java.util.function.Function;

/**
 * A geographical regression which is in reality a container of multiple geographical regressions, each predicting catches/hr
 * of a given species and this function actually simulating profits for each
 * Created by carrknight on 7/14/16.
 */
public class ProfitFunctionRegression implements Function<SeaTile, double[]>, GeographicalRegression<TripRecord> {


    private final ProfitFunction profit;

    private final GeographicalRegression<Double>[] catches;


    private Fisher fisher;

    private double currentTime;

    private FishState state;

    public ProfitFunctionRegression(ProfitFunction function,
                                    AlgorithmFactory<? extends  GeographicalRegression> regressionMaker,
                                    FishState state)
    {
        catches = new GeographicalRegression[state.getSpecies().size()];
        for(int i=0; i<catches.length; i++)
        {
            catches[i] = regressionMaker.apply(state);
        }
        this.profit=function;

    }

    public ProfitFunctionRegression(ProfitFunction function,
                                    GeographicalRegression<Double>[] catches)
    {
        this.catches=catches;
        this.profit=function;

    }

    @Override
    public double predict(
            SeaTile tile, double time, FishState state, Fisher fisher) {

        return this.predict(tile, time, state, fisher,false);


    }


    public double predict(
            SeaTile tile, double time, FishState state, Fisher fisher,boolean verbose) {

        this.state=state;
        this.currentTime=time;
        this.fisher=fisher;
        return profit.hourlyProfitFromHypotheticalTripHere(fisher, tile, state, this, verbose);


    }

    /**
     * Returns all the prediction of catches per hour in an array (indexed for species)
     *
     * @param tile the function argument
     * @return the function result
     */
    @Override
    public double[] apply(SeaTile tile) {

        double[] expectedHourlyCatches = new double[catches.length];
        for(int i=0; i<expectedHourlyCatches.length; i++)
            expectedHourlyCatches[i] = catches[i].predict(tile,currentTime,state,fisher);
        return expectedHourlyCatches;

    }

    @Override
    public void addObservation(
            GeographicalObservation<TripRecord> observation, Fisher fisher) {
        for(int i=0; i<catches.length; i++)
            catches[i].addObservation(
                    new GeographicalObservation<>(observation.getTile(),observation.getTime(),
                                                  observation.getValue().getTotalCatch()[i] /
                                                          observation.getValue().getEffort()),
                    fisher
            );

    }

    public GeographicalRegression<Double>[] catchesRegression() {
        return catches;
    }


    //ignored

    @Override
    public void start(FishState model) {
        for(GeographicalRegression reg : catches)
            reg.start(model);
    }

    //ignored

    @Override
    public void turnOff() {

    }
}