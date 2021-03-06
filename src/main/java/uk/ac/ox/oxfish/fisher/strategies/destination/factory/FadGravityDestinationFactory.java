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

package uk.ac.ox.oxfish.fisher.strategies.destination.factory;

import uk.ac.ox.oxfish.fisher.strategies.destination.fad.FadDeploymentRouteSelector;
import uk.ac.ox.oxfish.fisher.strategies.destination.fad.FadGravityDestinationStrategy;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.parameters.DoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;

public class FadGravityDestinationFactory implements AlgorithmFactory<FadGravityDestinationStrategy> {

    private DoubleParameter gravitationalConstraint = new FixedDoubleParameter(1d);

    @Override
    public FadGravityDestinationStrategy apply(FishState state) {
        return new FadGravityDestinationStrategy(
            gravitationalConstraint.apply(state.getRandom()),
            new FadDeploymentRouteSelector(state, 0, 1) // max travel time has to be set in scenario
        );
    }

    /**
     * Getter for property 'gravitationalConstraint'.
     *
     * @return Value for property 'gravitationalConstraint'.
     */
    public DoubleParameter getGravitationalConstraint() {
        return gravitationalConstraint;
    }

    /**
     * Setter for property 'gravitationalConstraint'.
     *
     * @param gravitationalConstraint Value to set for property 'gravitationalConstraint'.
     */
    public void setGravitationalConstraint(DoubleParameter gravitationalConstraint) {
        this.gravitationalConstraint = gravitationalConstraint;
    }
}
