package uk.ac.ox.oxfish.fisher.strategies.destination;

import ec.util.MersenneTwisterFast;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.Port;
import uk.ac.ox.oxfish.fisher.actions.Moving;
import uk.ac.ox.oxfish.fisher.log.TripRecord;
import uk.ac.ox.oxfish.fisher.strategies.RandomThenBackToPortDestinationStrategyTest;
import uk.ac.ox.oxfish.fisher.strategies.destination.factory.PerTripIterativeDestinationFactory;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class PerTripIterativeDestinationStrategyTest {


    @Test
    public void hillclimbsCorrectly() throws Exception {

        //best fitness is 100,100 worse is 0,0; the agent starts at 50,50, can it make it towards the top?

        final FishState fishState = RandomThenBackToPortDestinationStrategyTest.generateSimpleSquareMap(100);
        NauticalMap map = fishState.getMap();
        MersenneTwisterFast random = new MersenneTwisterFast();
        when(fishState.getRandom()).thenReturn(random);
        final FavoriteDestinationStrategy delegate = new FavoriteDestinationStrategy(
                map.getSeaTile(50, 50));
        final PerTripIterativeDestinationStrategy hill = new PerTripIterativeDestinationFactory().apply(fishState);

        //mock fisher enough to fool delegate
        Fisher fisher = mock(Fisher.class);
        when(fisher.grabRandomizer()).thenReturn(random);
        when(fisher.getLocation()).thenReturn(delegate.getFavoriteSpot());
        when(fisher.isGoingToPort()).thenReturn(false);
        final Port port = mock(Port.class);
        when(port.getLocation()).thenReturn(mock(SeaTile.class));
        when(fisher.getHomePort()).thenReturn(port);

        hill.start(fishState);
        SeaTile favoriteSpot=null;
        for(int i=0; i<1000; i++)
        {
            TripRecord record = mock(TripRecord.class);
            when(record.isCompleted()).thenReturn(true);
            when(record.isCutShort()).thenReturn(true);
            favoriteSpot = hill.chooseDestination(fisher,random,fishState,new Moving());
            when(record.getProfitPerHour()).thenReturn((double) (favoriteSpot.getGridX() + favoriteSpot.getGridY()));
            when(fisher.getLastFinishedTrip()).thenReturn(record);
            hill.reactToFinishedTrip(record);

        }

        System.out.println(favoriteSpot.getGridX() + " --- " + favoriteSpot.getGridY());
        Assert.assertTrue(favoriteSpot.getGridX() > 90);
        Assert.assertTrue(favoriteSpot.getGridY() > 90);

    }
}