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

package uk.ac.ox.oxfish.fisher.strategies.destination.fad;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.geography.ports.Port;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.ox.oxfish.geography.TestUtilities.makeCornerPortMap;

public class RouteTest {

    @Test
    public void test() {
        NauticalMap map = makeCornerPortMap(3, 3);
        final Port port = map.getPorts().getFirst();

        final Fisher fisher = mock(Fisher.class);
        when(fisher.getHomePort()).thenReturn(port);
        when(fisher.getLocation()).thenReturn(map.getSeaTile(2, 2));

        final Route currentRoute =
            new RouteToPortSelector(map)
                .selectRoute(fisher, 0, null)
                .orElseThrow(() -> new IllegalStateException("No route to port!"));

        // we're at 2, 2 and want to fish there, so we should stay there
        when(fisher.canAndWantToFishHere()).thenReturn(true);
        final SeaTile dest22 = currentRoute.next();
        when(fisher.getLocation()).thenReturn(dest22);
        assertEquals(map.getSeaTile(2, 2), fisher.getLocation());

        // we're at 2, 2 and we don't want to fish there anymore, so we should head for 1, 1
        when(fisher.canAndWantToFishHere()).thenReturn(false);
        final SeaTile dest11a = currentRoute.next();
        when(fisher.getLocation()).thenReturn(dest11a);
        assertEquals(map.getSeaTile(1, 1), fisher.getLocation());

        // we're at 1, 1 and want to fish there, so we should stay there
        when(fisher.canAndWantToFishHere()).thenReturn(true);
        final SeaTile dest11b = currentRoute.next();
        when(fisher.getLocation()).thenReturn(dest11b);
        assertEquals(map.getSeaTile(1, 1), fisher.getLocation());

        // we're at 1, 1 and we don't want to fish there anymore, so we should head for 0, 0
        when(fisher.canAndWantToFishHere()).thenReturn(false);
        final SeaTile dest00 = currentRoute.next();
        when(fisher.getLocation()).thenReturn(dest00);
        assertEquals(map.getSeaTile(0, 0), fisher.getLocation());

        // we're now at port, so we should have exhausted our current route
        assertFalse(currentRoute.hasNext());
    }

}