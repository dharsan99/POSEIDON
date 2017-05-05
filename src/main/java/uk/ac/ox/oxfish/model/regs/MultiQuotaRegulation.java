package uk.ac.ox.oxfish.model.regs;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Preconditions;
import sim.engine.SimState;
import sim.engine.Steppable;
import uk.ac.ox.oxfish.biology.Species;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.equipment.Catch;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.StepOrder;
import uk.ac.ox.oxfish.utility.FishStateUtilities;

import java.util.Arrays;

/**
 * Yearly resetting biomass quotas (different for each specie), counted at landing. If any quota is at 0, no
 * other specie is tradeable
 * Created by carrknight on 10/7/15.
 */
public class MultiQuotaRegulation implements  QuotaPerSpecieRegulation,Steppable
{

    private final double[] yearlyQuota;

    private final double[] quotaRemaining;

    private final FishState state;

    private boolean respectMPA = true;

    public MultiQuotaRegulation(double[] yearlyQuota, FishState state) {
        this.yearlyQuota = Arrays.copyOf(yearlyQuota,yearlyQuota.length);
        this.quotaRemaining = Arrays.copyOf(yearlyQuota,yearlyQuota.length);
        for (double aQuotaRemaining : quotaRemaining) {
            assert aQuotaRemaining >= 0;
        }
        this.state = state;
        this.state.scheduleEveryYear(this, StepOrder.POLICY_UPDATE);
    }


    public boolean isFishingStillAllowed(){

        //all quotas must be at least 0 and at least one must be ABOVE 0
        boolean above0 = false;
        for(double quota : quotaRemaining) {
            if (quota < 0)
                return false;
            if(quota > FishStateUtilities.EPSILON)
                above0 = true;
        }

        return above0;

    }

    @Override
    public void start(FishState model, Fisher fisher) {

    }

    @Override
    public void turnOff(Fisher fisher) {

    }

    /**
     * how much of this species biomass is sellable. Zero means it is unsellable
     *
     * @param agent  the fisher selling its catch
     * @param species the species we are being asked about
     * @param model  a link to the model
     * @return a positive biomass if it sellable. Zero if you need to throw everything away
     */
    @Override
    public double maximumBiomassSellable(
            Fisher agent, Species species, FishState model) {
        return quotaRemaining[species.getIndex()] + FishStateUtilities.EPSILON/2;
    }


    /**
     * returns a copy of the regulation, used defensively
     *
     * @return
     */
    @Override
    public Regulation makeCopy() {
        return new MultiQuotaRegulation(yearlyQuota,state);
    }

    /**
     * ignored
     * @param where
     * @param who
     * @param fishCaught catch object
     * @param fishRetained
     * @param hoursSpentFishing
     */
    @Override
    public void reactToFishing(
            SeaTile where, Fisher who, Catch fishCaught, Catch fishRetained,
            int hoursSpentFishing) {
        //ignored
    }


    /**
     * Can this fisher be at sea?
     *
     * @param fisher the  fisher
     * @param model  the model
     * @return true if it can be out. When it's false the fisher can't leave port and ought to go back to port if he is
     * at sea
     */
    @Override
    public boolean allowedAtSea(Fisher fisher, FishState model) {
        return isFishingStillAllowed();

    }




    /**burn through quotas; because of "maximum biomass sellable"  method, I expect here that the biomass
     * sold is less or equal to the quota available
     *
     * @param species  the species of fish sold
     * @param seller  agent selling the fish
     * @param biomass how much biomass has been sold
     * @param revenue how much money was made off it
     */
    @Override
    public void reactToSale(Species species, Fisher seller, double biomass, double revenue) {
        double newQuota = quotaRemaining[species.getIndex()] - biomass;
        if(Log.TRACE)
            Log.trace("lowering quota for " + species + " owned by " + seller + "to " +
                              newQuota);
        setQuotaRemaining(species.getIndex(), newQuota);
        Preconditions.checkArgument(quotaRemaining[species.getIndex()]>=- FishStateUtilities.EPSILON, quotaRemaining[species.getIndex()]);
    }

    /**
     * can fish as long as it is not an MPA and still has all quotas
     *
     * @param agent the agent that wants to fish
     * @param tile  the tile the fisher is trying to fish on
     * @param model a link to the model
     * @return true if the fisher can fish
     */
    @Override
    public boolean canFishHere(
            Fisher agent, SeaTile tile, FishState model) {
        return isFishingStillAllowed() && (!tile.isProtected() || !respectMPA);
    }

    @Override
    public void setQuotaRemaining(int specieIndex, double newQuotaValue) {

        quotaRemaining[specieIndex] = newQuotaValue;
        Preconditions.checkArgument(newQuotaValue >= -FishStateUtilities.EPSILON);

    }

    public void setYearlyQuota(int specieIndex, double newQuotaValue) {
        yearlyQuota[specieIndex] = newQuotaValue;
        if(quotaRemaining[specieIndex]>=yearlyQuota[specieIndex])
            setQuotaRemaining(specieIndex,newQuotaValue);
        Preconditions.checkArgument(newQuotaValue >= -FishStateUtilities.EPSILON);

    }

    @Override
    public double getQuotaRemaining(int specieIndex) {
        return quotaRemaining[specieIndex];
    }

    @Override
    public void step(SimState simState) {
        System.arraycopy(yearlyQuota, 0, quotaRemaining, 0, quotaRemaining.length);

    }

    public double[] getYearlyQuota() {
        return yearlyQuota;
    }



    protected FishState getState() {
        return state;
    }

    public int getNumberOfSpeciesTracked(){
        return getYearlyQuota().length;
    }


    /**
     * Getter for property 'respectMPA'.
     *
     * @return Value for property 'respectMPA'.
     */
    public boolean isRespectMPA() {
        return respectMPA;
    }

    /**
     * Setter for property 'respectMPA'.
     *
     * @param respectMPA Value to set for property 'respectMPA'.
     */
    public void setRespectMPA(boolean respectMPA) {
        this.respectMPA = respectMPA;
    }
}
