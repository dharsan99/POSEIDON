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

package uk.ac.ox.oxfish.model.data.webviz.scenarios;

import java.util.Collection;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class HeatmapDefinition {

    private final String title;
    private final String path;
    private final String legend;
    private final Collection<ColourMapEntry> colourMap;

    public HeatmapDefinition(
        final String title,
        final String path,
        final String legend,
        final Collection<ColourMapEntry> colourMap
    ) {
        this.title = title;
        this.path = path;
        this.legend = legend;
        this.colourMap = colourMap;
    }

}