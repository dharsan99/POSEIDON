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

package uk.ac.ox.oxfish.fisher.equipment.gear.fads;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ox.oxfish.biology.GlobalBiology;
import uk.ac.ox.oxfish.fisher.equipment.fads.FadManager;
import uk.ac.ox.oxfish.fisher.equipment.gear.FixedProportionGear;
import uk.ac.ox.oxfish.model.FishState;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PurseSeineGearTest {

    private PurseSeineGear purseSeineGear;

    @Before
    public void setUp() {
        final FishState fishState = mock(FishState.class);
        when(fishState.getBiology()).thenReturn(new GlobalBiology());
        purseSeineGear = new PurseSeineGear(
            mock(FadManager.class),
            1, 1, 0.1, 1,
            new double[0][]
        );
    }

    @Test
    public void expectedHourlyCatchNotSupported() {
        assertThrows(UnsupportedOperationException.class, () ->
            purseSeineGear.expectedHourlyCatch(null, null, 0, null)
        );
    }

    @Test
    public void isSame() {
        assertTrue(purseSeineGear.isSame(purseSeineGear));
        assertFalse(purseSeineGear.isSame(null));
        assertFalse(purseSeineGear.isSame(new FixedProportionGear(0)));
        assertTrue(purseSeineGear.isSame(purseSeineGear.makeCopy()));
        assertTrue(purseSeineGear.isSame(new PurseSeineGear(
            purseSeineGear.getFadManager(),
            purseSeineGear.getMinimumSetDurationInHours(),
            purseSeineGear.getAverageSetDurationInHours(),
            purseSeineGear.getStdDevOfSetDurationInHours(),
            purseSeineGear.getSuccessfulSetProbability(),
            purseSeineGear.getUnassociatedSetSamples()
        )));
        assertFalse(purseSeineGear.isSame(new PurseSeineGear(
            mock(FadManager.class),
            purseSeineGear.getMinimumSetDurationInHours(),
            purseSeineGear.getAverageSetDurationInHours(),
            purseSeineGear.getStdDevOfSetDurationInHours(),
            purseSeineGear.getSuccessfulSetProbability(),
            purseSeineGear.getUnassociatedSetSamples()
        )));
        assertFalse(purseSeineGear.isSame(new PurseSeineGear(
            purseSeineGear.getFadManager(),
            2,
            purseSeineGear.getAverageSetDurationInHours(),
            purseSeineGear.getStdDevOfSetDurationInHours(),
            purseSeineGear.getSuccessfulSetProbability(),
            purseSeineGear.getUnassociatedSetSamples()
        )));
        assertFalse(purseSeineGear.isSame(new PurseSeineGear(
            purseSeineGear.getFadManager(),
            purseSeineGear.getMinimumSetDurationInHours(),
            2,
            purseSeineGear.getStdDevOfSetDurationInHours(),
            purseSeineGear.getSuccessfulSetProbability(),
            purseSeineGear.getUnassociatedSetSamples()
        )));
        assertFalse(purseSeineGear.isSame(new PurseSeineGear(
            purseSeineGear.getFadManager(),
            purseSeineGear.getMinimumSetDurationInHours(),
            purseSeineGear.getAverageSetDurationInHours(),
            0.2,
            purseSeineGear.getSuccessfulSetProbability(),
            purseSeineGear.getUnassociatedSetSamples()
        )));
        assertFalse(purseSeineGear.isSame(new PurseSeineGear(
            purseSeineGear.getFadManager(),
            purseSeineGear.getMinimumSetDurationInHours(),
            purseSeineGear.getAverageSetDurationInHours(),
            2,
            purseSeineGear.getSuccessfulSetProbability(),
            purseSeineGear.getUnassociatedSetSamples()
        )));
        assertFalse(purseSeineGear.isSame(new PurseSeineGear(
            purseSeineGear.getFadManager(),
            purseSeineGear.getMinimumSetDurationInHours(),
            purseSeineGear.getAverageSetDurationInHours(),
            purseSeineGear.getStdDevOfSetDurationInHours(),
            0,
            purseSeineGear.getUnassociatedSetSamples()
        )));
        assertFalse(purseSeineGear.isSame(new PurseSeineGear(
            purseSeineGear.getFadManager(),
            purseSeineGear.getMinimumSetDurationInHours(),
            purseSeineGear.getAverageSetDurationInHours(),
            purseSeineGear.getStdDevOfSetDurationInHours(),
            purseSeineGear.getSuccessfulSetProbability(),
            new double[][]{{1.0}}
        )));
    }
}