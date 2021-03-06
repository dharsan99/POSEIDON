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

package uk.ac.ox.oxfish.model.data.webviz.heatmaps;

import sim.engine.SimState;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.model.FishState;

import static java.util.stream.IntStream.range;
import static uk.ac.ox.oxfish.utility.FishStateUtilities.round;

public final class ExtractorBasedHeatmapBuilder implements HeatmapBuilder {

    private final NumericExtractor numericExtractor;
    private final TimestepsBuilder timestepsBuilder;

    ExtractorBasedHeatmapBuilder(
        final NumericExtractor numericExtractor,
        final TimestepsBuilder timestepsBuilder
    ) {
        this.numericExtractor = numericExtractor;
        this.timestepsBuilder = timestepsBuilder;
    }

    @Override
    public Heatmap buildJsonObject(final FishState fishState) { return new Heatmap(timestepsBuilder.build()); }

    @Override public void step(final SimState simState) {

        final FishState fishState = (FishState) simState;
        final NauticalMap map = fishState.getMap();

        final double[] cellValues =
            range(0, map.getHeight()).boxed().flatMapToDouble(y ->
                range(0, map.getWidth()).mapToDouble(x ->
                    round(numericExtractor.applyAsDouble(map.getSeaTile(x, y)))
                )
            ).toArray();

        timestepsBuilder.add(new Timestep(fishState.getDay(), cellValues));
    }

    @Override public void start(final FishState fishState) {
        HeatmapBuilder.super.start(fishState);
        numericExtractor.start(fishState);
    }

}
