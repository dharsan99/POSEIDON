package uk.ac.ox.oxfish.fisher.equipment.gear;

import ec.util.MersenneTwisterFast;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ox.oxfish.biology.GlobalBiology;
import uk.ac.ox.oxfish.biology.Specie;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.equipment.Catch;
import uk.ac.ox.oxfish.geography.SeaTile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class HabitatAwareRandomCatchabilityTest
{


    @Test
    public void correct() throws Exception {


        HabitatAwareRandomCatchability gear = new HabitatAwareRandomCatchability(
                new double[]{.1},
                new double[]{0},
                new double[]{.2},
                new double[]{0},
                1
        );


        SeaTile tile = mock(SeaTile.class);
        Specie specie = new Specie("0");
        GlobalBiology biology = new GlobalBiology(specie);
        when(tile.getBiomass(specie)).thenReturn(100d);
        when(tile.getRockyPercentage()).thenReturn(1d);


        Fisher fisher = mock(Fisher.class);
        when(fisher.grabRandomizer()).thenReturn(new MersenneTwisterFast());
        Catch fishCaught = gear.fish(fisher, tile, 1, biology);

        Assert.assertEquals(20,fishCaught.getPoundsCaught(specie),.01);
        when(tile.getRockyPercentage()).thenReturn(0d);
        Assert.assertEquals(10,gear.fish(fisher, tile, 1, biology).getPoundsCaught(specie),.01);







    }
}