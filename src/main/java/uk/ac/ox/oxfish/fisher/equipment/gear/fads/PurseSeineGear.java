/*
 *     POSEIDON, an agent-based model of fisheries
 *     Copyright (C) 2019  CoHESyS Lab cohesys.lab@gmail.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package uk.ac.ox.oxfish.fisher.equipment.gear.fads;

import ec.util.MersenneTwisterFast;
import uk.ac.ox.oxfish.biology.BiomassLocalBiology;
import uk.ac.ox.oxfish.biology.GlobalBiology;
import uk.ac.ox.oxfish.biology.LocalBiology;
import uk.ac.ox.oxfish.biology.VariableBiomassBasedBiology;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.equipment.Boat;
import uk.ac.ox.oxfish.fisher.equipment.Catch;
import uk.ac.ox.oxfish.fisher.equipment.fads.FadManager;
import uk.ac.ox.oxfish.fisher.equipment.gear.Gear;
import uk.ac.ox.oxfish.fisher.equipment.gear.HoldLimitingDecoratorGear;
import uk.ac.ox.oxfish.geography.SeaTile;

import javax.measure.Quantity;
import javax.measure.quantity.Time;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.Math.min;
import static tech.units.indriya.quantity.Quantities.getQuantity;
import static tech.units.indriya.unit.Units.HOUR;
import static uk.ac.ox.oxfish.utility.MasonUtils.oneOf;

public class PurseSeineGear implements Gear {

    private final FadManager fadManager;
    private final double minimumSetDurationInHours;
    private final double averageSetDurationInHours;
    private final double stdDevOfSetDurationInHours;
    private final double successfulSetProbability;
    private final double[][] unassociatedSetSamples;

    public PurseSeineGear(
        FadManager fadManager,
        double minimumSetDurationInHours,
        double averageSetDurationInHours,
        double stdDevOfSetDurationInHours,
        double successfulSetProbability,
        double[][] unassociatedSetSamples
    ) {
        this.fadManager = fadManager;
        this.minimumSetDurationInHours = minimumSetDurationInHours;
        this.averageSetDurationInHours = averageSetDurationInHours;
        this.stdDevOfSetDurationInHours = stdDevOfSetDurationInHours;
        this.successfulSetProbability = successfulSetProbability;
        this.unassociatedSetSamples = unassociatedSetSamples;
    }

    public double getMinimumSetDurationInHours() { return minimumSetDurationInHours; }

    public double getAverageSetDurationInHours() { return averageSetDurationInHours; }

    public double getStdDevOfSetDurationInHours() { return stdDevOfSetDurationInHours; }

    public double[][] getUnassociatedSetSamples() { return unassociatedSetSamples; }

    public double getSuccessfulSetProbability() {
        return successfulSetProbability;
    }

    public FadManager getFadManager() { return fadManager; }

    public Quantity<Time> nextSetDuration(MersenneTwisterFast rng) {
        final double duration = Math.max(
            minimumSetDurationInHours,
            rng.nextGaussian() * stdDevOfSetDurationInHours + averageSetDurationInHours
        );
        return getQuantity(duration, HOUR);
    }

    /**
     * This creates a new biology object that will be the target of an unassociated set.
     * It's called by the MakeUnassociatedSet action but the code lives here because the
     * gear knows how much fish it can catch. The fish is taken from the underlying sea tile.
     */
    public LocalBiology createUnassociatedSetBiology(
        GlobalBiology globalBiology,
        LocalBiology seaTileBiology,
        MersenneTwisterFast rng
    ) {
        final double[] biomasses = oneOf(unassociatedSetSamples, rng).clone();
        for (int i = 0; i < biomasses.length; i++) {
            final double biomassInTile = seaTileBiology.getBiomass(globalBiology.getSpecie(i));
            biomasses[i] = min(biomassInTile, biomasses[i]);
        }
        final VariableBiomassBasedBiology unassociatedSetBiology = new BiomassLocalBiology(biomasses, biomasses);
        // Remove the catches from the underlying biology:
        final Catch catchObject = new Catch(unassociatedSetBiology.getCurrentBiomass());
        seaTileBiology.reactToThisAmountOfBiomassBeingFished(catchObject, catchObject, globalBiology);
        return unassociatedSetBiology;
    }

    @Override public Catch fish(
        Fisher fisher,
        LocalBiology localBiology,
        SeaTile context,
        int hoursSpentFishing,
        GlobalBiology globalBiology
    ) {
        // Assume we catch *all* the biomass from the FAD
        final double[] catches = globalBiology.getSpecies().stream()
            .mapToDouble(localBiology::getBiomass).toArray();
        return HoldLimitingDecoratorGear.limitToHoldCapacity(new Catch(catches), fisher.getHold(), globalBiology);
    }

    @Override
    public double getFuelConsumptionPerHourOfFishing(Fisher fisher, Boat boat, SeaTile where) {
        // TODO: see if making a set should consume fuel
        return 0;
    }

    @Override
    public double[] expectedHourlyCatch(
        Fisher fisher, SeaTile where, int hoursSpentFishing, GlobalBiology modelBiology
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Gear makeCopy() {
        return new PurseSeineGear(
            fadManager,
            minimumSetDurationInHours,
            averageSetDurationInHours,
            stdDevOfSetDurationInHours,
            successfulSetProbability,
            unassociatedSetSamples
        );
    }

    @Override public boolean isSame(Gear o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurseSeineGear that = (PurseSeineGear) o;
        return Double.compare(that.minimumSetDurationInHours, minimumSetDurationInHours) == 0 &&
            Double.compare(that.averageSetDurationInHours, averageSetDurationInHours) == 0 &&
            Double.compare(that.stdDevOfSetDurationInHours, stdDevOfSetDurationInHours) == 0 &&
            Double.compare(that.successfulSetProbability, successfulSetProbability) == 0 &&
            Objects.equals(fadManager, that.fadManager) &&
            Arrays.equals(unassociatedSetSamples, that.unassociatedSetSamples);
    }

}
