package uk.ac.ox.oxfish.geography.fads;

import org.apache.sis.measure.Quantities;
import org.junit.Test;
import uk.ac.ox.oxfish.biology.GlobalBiology;
import uk.ac.ox.oxfish.biology.Species;
import uk.ac.ox.oxfish.fisher.equipment.fads.Fad;
import uk.ac.ox.oxfish.fisher.equipment.fads.FadManager;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.currents.CurrentMaps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static uk.ac.ox.oxfish.utility.Measures.TONNE;

public class FadInitializerTest {

    @Test
    public void fadBiomassInitializedToZero() {
        final GlobalBiology globalBiology =
            new GlobalBiology(new Species("A"), new Species("B"));
        final FadInitializer fadInitializer =
            new FadInitializer(Quantities.create(1d, TONNE), 0d);
        final FadMap fadMap =
            new FadMap(mock(NauticalMap.class), mock(CurrentMaps.class), globalBiology, fadInitializer);
        final FadManager fadManager = new FadManager(fadMap, 0);

        final Fad fad = fadInitializer.apply(fadManager);
        for (Species species : globalBiology.getSpecies())
            assertEquals(fad.getAggregatedBiology().getBiomass(species), 0, 0);
    }
}