package uk.ac.ox.oxfish.biology;

import com.google.common.base.Preconditions;
import sim.engine.SimState;
import sim.engine.Steppable;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.Startable;
import uk.ac.ox.oxfish.model.StepOrder;

import java.util.Arrays;

/**
 * A simple local biology that has carrying capacity and actual biomass. It then grows each species,
 * each year through logistic regression. There is no link/movement to other biologies.
 * Created by carrknight on 5/8/15.
 */
public class IndependentLogisticLocalBiology implements LocalBiology, Steppable, Startable {

    /**
     * the current amount of biomass in this spot
     */
    private double[] currentBiomass;

    /**
     * the maximum amount of biomass
     */
    private double[] carryingCapacity;

    /**
     * the uninpeded growth rate of each species
     */
    private double[] malthusianParameter;

    /**
     * initialize the local biology
     * @param currentBiomass the biomass available
     * @param carryingCapacity the maximum amount of fish
     * @param malthusianParameter the unconstrained growth rate of each species
     */
    public IndependentLogisticLocalBiology(double[] currentBiomass, double[] carryingCapacity,
                                           double[] malthusianParameter) {
        Preconditions.checkArgument(currentBiomass.length==carryingCapacity.length);
        Preconditions.checkArgument(currentBiomass.length==malthusianParameter.length);

        this.currentBiomass = Arrays.copyOf(currentBiomass,currentBiomass.length);
        this.carryingCapacity =Arrays.copyOf(carryingCapacity,carryingCapacity.length);
        this.malthusianParameter = Arrays.copyOf(malthusianParameter,malthusianParameter.length);

    }


    /**
     * the biomass at this location for a single specie.
     *
     * @param specie the specie you care about
     * @return the biomass of this specie
     */
    @Override
    public Double getBiomass(Specie specie) {
        final int index = specie.getIndex();
        if(index>=this.currentBiomass.length)
            return 0d; //don't have it
        else
            return this.currentBiomass[index];
    }

    /**
     * the carrying capacity of this location for this specie
     * @param specie the specie
     * @return the carrying capacity for this specie at this location
     */
    public double getCarryingCapacity(Specie specie)
    {
        final int index = specie.getIndex();
        if(index>=this.carryingCapacity.length)
            return 0d; //don't have it
        else
            return this.carryingCapacity[index];
    }


    /**
     * Get the unconstrained growth rate of the biomass
     * @param specie the specie
     * @return the unconstrained growth rate.
     */
    public double getMalthusianParameter(Specie specie)
    {
        final int index = specie.getIndex();
        if(index>=this.malthusianParameter.length)
            return 0d; //don't have it
        else
            return this.malthusianParameter[index];
    }


    /**
     * Tells the local biology that a fisher (or something anyway) fished this much biomass from this location
     *
     * @param specie        the specie fished
     * @param biomassFished the biomass fished
     */
    @Override
    public void reactToThisAmountOfBiomassBeingFished(Specie specie, Double biomassFished) {

        final int specieIntex = specie.getIndex();
        Preconditions.checkArgument(specieIntex < currentBiomass.length || biomassFished == 0,
                                    "you can't fish species that aren't here");


        if(specieIntex < currentBiomass.length)
        {
            currentBiomass[specieIntex]-= biomassFished;
            Preconditions.checkState(currentBiomass[specieIntex] >=0, "fished more biomass than available");
        }

    }


    /**
     * grows logistically whenever stepped (supposedly once a year)
     * @param simState the model
     */
    @Override
    public void step(SimState simState) {

        assert (currentBiomass.length==carryingCapacity.length);
        assert (currentBiomass.length==malthusianParameter.length);

        //grow fish
        for(int i=0; i<currentBiomass.length; i++)
        {
            currentBiomass[i]=Math.min(carryingCapacity[i], currentBiomass[i] + malthusianParameter[i] *
                    (1d - currentBiomass[i] / carryingCapacity[i]) * currentBiomass[i]);
        }

    }


    /**
     *  set a new carrying capacity, might modify the current biomass
     * @param s the specie
     * @param newCarryingCapacity the new carrying capacity
     */
    public void setCarryingCapacity(Specie s, double newCarryingCapacity)
    {
        Preconditions.checkArgument(newCarryingCapacity > 0, "new carrying capacity must be positive");

        final int index = s.getIndex();
        if(index >=currentBiomass.length)
            growArrays(index +1);
        carryingCapacity[index] = newCarryingCapacity;
        //don't let currentbiomass be above carryingCapacity
        currentBiomass[index] = Math.min(currentBiomass[index],newCarryingCapacity);
    }


    /**
     * sets the new current biomass. Must be lower than carrying capacity
     * @param s the specie
     * @param newCurrentBiomass the new biomass in lbs
     */
    public void setCurrentBiomass(Specie s, double newCurrentBiomass)
    {

        Preconditions.checkArgument(newCurrentBiomass >= 0, "new biomass can't be negative!");
        final int index = s.getIndex();
        if(index >=currentBiomass.length)
            growArrays(index +1);
        Preconditions.checkArgument(currentBiomass[index] <= carryingCapacity[index],
                                    "the new current biomass is higher than carrying capacity!");

        currentBiomass[index] = newCurrentBiomass;
    }


    /**
     * set the new unconstrained growth rate for a specie
     * @param s the specie
     * @param newGrowth the new growth rate, must be not negative
     */
    public void setMalthusianParameter(Specie s, double newGrowth)
    {
        Preconditions.checkArgument(newGrowth >= 0, "growth can't be negative!");
        final int index = s.getIndex();
        if(index >=currentBiomass.length)
            growArrays(index +1);

        malthusianParameter[index] = newGrowth;

    }



    /**
     * schedule to act each year
     * @param model the model to schedule on
     */
    @Override
    public void start(FishState model) {
        model.scheduleEveryYear(this, StepOrder.BEFORE_FISHER_PHASE);
    }

    /**
     * pad with zeros
     * @param newSize new array size
     */
    private void growArrays(int newSize)
    {
        assert currentBiomass.length < newSize;
        currentBiomass = Arrays.copyOf(currentBiomass,newSize);
        malthusianParameter = Arrays.copyOf(malthusianParameter,newSize);
        carryingCapacity = Arrays.copyOf(carryingCapacity,newSize);
    }



}