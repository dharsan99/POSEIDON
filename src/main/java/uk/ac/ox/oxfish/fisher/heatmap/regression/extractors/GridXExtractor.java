package uk.ac.ox.oxfish.fisher.heatmap.regression.extractors;

import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.heatmap.regression.extractors.ObservationExtractor;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;

/**
 * extract the grid x coordinate from the observation
 * Created by carrknight on 7/7/16.
 */
public class GridXExtractor  implements ObservationExtractor {





    @Override
    public double extract(SeaTile tile, double timeOfObservation, Fisher agent, FishState model) {
        return tile.getGridX();
    }
}