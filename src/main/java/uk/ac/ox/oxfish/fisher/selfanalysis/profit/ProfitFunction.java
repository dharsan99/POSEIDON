package uk.ac.ox.oxfish.fisher.selfanalysis.profit;

import uk.ac.ox.oxfish.biology.Species;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.log.TripRecord;
import uk.ac.ox.oxfish.fisher.selfanalysis.LameTripSimulator;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;

import java.util.LinkedList;
import java.util.function.Function;

/**
 * A container to judge the profits of a trip after the trip is over or to simulate a trip and guess profits that way
 * Created by carrknight on 7/13/16.
 */
public class ProfitFunction {


    /**
     * separate from the other because they are always there
     */
    private Cost oilCosts = new GasCost();



    /**
     * needed to predict profits from trip
     */
    private final LameTripSimulator simulator;

    private final double maxHours;



    public ProfitFunction(
            LameTripSimulator simulator, double maxHours) {
        this.simulator = simulator;
        this.maxHours = maxHours;
    }

    public ProfitFunction(
            double maxHours) {

        this(new LameTripSimulator(),maxHours);
    }

    /**
     * compute hourly profits for this trip given current information (so if prices of fish changed since this trip is over
     * the profits computed here and the profits recorded in the trip won't be the same)
     * @param fisher agent who made the trip or anyway whose point of view matters
     * @param trip the actual trip that took place
     * @param state the model
     * @return $/hr profits of this trip
     */
    public double hourlyProfitFromThisTrip(Fisher fisher, TripRecord trip, FishState state)
    {

        if(trip==null)
            return Double.NaN;

        double[] catches = trip.getSoldCatch();
        double earnings = 0;
        for(Species species : state.getSpecies())
            earnings += catches[species.getIndex()] * fisher.getHomePort().getMarginalPrice(species,fisher);
        double costs = 0;
        computeCosts(fisher, trip, state, earnings);



        return  trip.getProfitPerHour(true);
    }

    private void computeCosts(Fisher fisher, TripRecord trip, FishState state, double earnings) {

        double costs = oilCosts.cost(fisher,state,trip,earnings);
        for(Cost otherCost : fisher.getAdditionalTripCosts())
            costs+= otherCost.cost(fisher,state,trip,earnings);
        trip.recordCosts(costs);

        costs = 0;
        for(Cost opportunity : fisher.getOpportunityCosts())
            costs+= opportunity.cost(fisher,state,trip,earnings);
        trip.recordOpportunityCosts(costs);

    }

    public double hourlyProfitFromHypotheticalTripHere(
            Fisher fisher, SeaTile where, FishState state,
            Function<SeaTile, double[]> catchExpectations, boolean verbose)
    {
        return hourlyProfitFromThisTrip(fisher,
                                        simulator.simulateRecord(fisher,
                                                                 where,
                                                                 state,
                                                                 maxHours, catchExpectations.apply(where), verbose),
                                        state);
    }







}