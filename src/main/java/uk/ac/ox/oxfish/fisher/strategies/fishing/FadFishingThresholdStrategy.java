/*
 *  POSEIDON, an agent-based model of fisheries
 *  Copyright (C) 2020  CoHESyS Lab cohesys.lab@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package uk.ac.ox.oxfish.fisher.strategies.fishing;

import com.google.common.util.concurrent.AtomicLongMap;
import ec.util.MersenneTwisterFast;
import org.jetbrains.annotations.NotNull;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.actions.ActionResult;
import uk.ac.ox.oxfish.fisher.actions.Arriving;
import uk.ac.ox.oxfish.fisher.actions.purseseiner.DeployFad;
import uk.ac.ox.oxfish.fisher.actions.purseseiner.PurseSeinerAction;
import uk.ac.ox.oxfish.fisher.actions.purseseiner.MakeFadSet;
import uk.ac.ox.oxfish.fisher.actions.purseseiner.MakeUnassociatedSet;
import uk.ac.ox.oxfish.fisher.equipment.fads.Fad;
import uk.ac.ox.oxfish.fisher.equipment.fads.FadManager;
import uk.ac.ox.oxfish.fisher.equipment.fads.FadManagerUtils;
import uk.ac.ox.oxfish.fisher.log.TripRecord;
import uk.ac.ox.oxfish.fisher.strategies.destination.fad.FadDestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.destination.fad.FadGravityDestinationStrategy;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.market.Market;
import uk.ac.ox.oxfish.model.regs.Regulation;
import uk.ac.ox.oxfish.utility.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.lang.StrictMath.exp;
import static java.util.Comparator.comparingDouble;
import static uk.ac.ox.oxfish.fisher.equipment.fads.FadManagerUtils.fadsHere;
import static uk.ac.ox.oxfish.fisher.equipment.fads.FadManagerUtils.priceOfFishHere;
import static uk.ac.ox.oxfish.utility.Measures.toHours;

public class FadFishingThresholdStrategy implements FishingStrategy, FadManagerUtils {

    private final AtomicLongMap<Class<? extends PurseSeinerAction>> consecutiveActionCounts = AtomicLongMap.create();
    final private double minFadValue;
    private Optional<? extends PurseSeinerAction> nextAction = Optional.empty();
    private double fadDeploymentsCoefficient;
    private double setsOnOtherFadsCoefficient;
    private double unassociatedSetsCoefficient;
    private double fadDeploymentsProbabilityDecay;

    public FadFishingThresholdStrategy(
        double unassociatedSetsCoefficient,
        double fadDeploymentsCoefficient,
        double setsOnOtherFadsCoefficient,
        double fadDeploymentsProbabilityDecay,
        double minFadValue
    ) {
        this.unassociatedSetsCoefficient = unassociatedSetsCoefficient;
        this.fadDeploymentsCoefficient = fadDeploymentsCoefficient;
        this.setsOnOtherFadsCoefficient = setsOnOtherFadsCoefficient;
        this.fadDeploymentsProbabilityDecay = fadDeploymentsProbabilityDecay;
        this.minFadValue = minFadValue;
    }

    @Override
    public boolean shouldFish(
        Fisher fisher, MersenneTwisterFast random, FishState model, TripRecord currentTrip
    ) {

//        if(consecutiveActionCounts.sum()>10)
//            return false;

        if (!nextAction.isPresent()) {
            nextAction = maybeDeployFad(model, fisher);
        }

        if (!nextAction.isPresent()) {
            if (random.nextDouble() < unassociatedSetsCoefficient)
                nextAction = maybeMakeUnassociatedSet(model, fisher);
        }
        if (!nextAction.isPresent()) {
            nextAction = maybeMakeFadSet(model, fisher);
        }

        return nextAction.isPresent();
    }

    private Optional<? extends PurseSeinerAction> maybeDeployFad(FishState model, Fisher fisher) {

        final Map<SeaTile, Double> deploymentLocationValues =
            fisher.getDestinationStrategy() instanceof FadDestinationStrategy ?
                ((FadDestinationStrategy) fisher.getDestinationStrategy())
                    .getFadDeploymentRouteSelector()
                    .getDeploymentLocationValues() :
                ((FadGravityDestinationStrategy) fisher.getDestinationStrategy())
                    .getFadDeploymentRouteSelector()
                    .getDeploymentLocationValues();

        return Optional
            .ofNullable(deploymentLocationValues.get(fisher.getLocation()))
            .map(value -> probability(fadDeploymentsCoefficient, value, consecutiveActionCounts.get(DeployFad.class), fadDeploymentsProbabilityDecay))
            .filter(p -> model.getRandom().nextDouble() < p)
            .map(__ -> new DeployFad(model, fisher))
            .filter(PurseSeinerAction::canHappen);
    }

    private Optional<? extends PurseSeinerAction> maybeMakeUnassociatedSet(FishState model, Fisher fisher) {
        return Optional.of(new MakeUnassociatedSet(model, fisher))
            .filter(PurseSeinerAction::canHappen);
    }

    private Optional<? extends PurseSeinerAction> maybeMakeFadSet(FishState model, Fisher fisher) {
        final FadManager manager = FadManagerUtils.getFadManager(fisher);
        return fadsHere(fisher)
            .filter(fad -> fad.getOwner() == manager || model.getRandom().nextDouble() < setsOnOtherFadsCoefficient)
            .map(fad -> new Pair<>(fad, setValue(fad, fisher)))
            .filter(fadDoublePair -> fadDoublePair.getSecond() > minFadValue)
            .sorted(comparingDouble(Pair::getSecond))
            .map(pair -> new MakeFadSet(model, fisher, pair.getFirst()))
            .filter(PurseSeinerAction::canHappen)
            .findFirst();
    }

    private double probability(
        double coefficient,
        double value,
        double numConsecutiveActions,
        double probabilityDecayCoefficient
    ) {
        return (1.0 - exp(-coefficient * (value + 1))) /
            (1.0 + (probabilityDecayCoefficient * numConsecutiveActions));
    }

    private double setValue(Fad fad, Fisher fisher) {
        return priceOfFishHere(fad.getBiology(), getMarkets(fisher));
    }

    private Collection<Market> getMarkets(Fisher fisher) {
        return fisher.getHomePort().getMarketMap(fisher).getMarkets();
    }

    @Override
    @NotNull
    public ActionResult act(
        FishState model, Fisher fisher, Regulation regulation, double hoursLeft
    ) {
        nextAction = nextAction.filter(action -> hoursLeft >= toHours(action.getDuration()));
        // If we have a next action, increment its counter
        nextAction.map(PurseSeinerAction::getClass).ifPresent(consecutiveActionCounts::incrementAndGet);
        if (!nextAction.isPresent()) consecutiveActionCounts.clear();
        final ActionResult actionResult = nextAction
            .map(action -> new ActionResult(action, hoursLeft))
            .orElse(new ActionResult(new Arriving(), 0));
        nextAction = Optional.empty();
        return actionResult;
    }

}
