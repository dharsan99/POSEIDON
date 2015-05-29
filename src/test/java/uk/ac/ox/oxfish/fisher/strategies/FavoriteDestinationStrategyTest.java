package uk.ac.ox.oxfish.fisher.strategies;

import ec.util.MersenneTwisterFast;
import org.junit.Test;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.Port;
import uk.ac.ox.oxfish.fisher.actions.Arriving;
import uk.ac.ox.oxfish.fisher.actions.AtPort;
import uk.ac.ox.oxfish.fisher.actions.Moving;
import uk.ac.ox.oxfish.fisher.strategies.destination.FavoriteDestinationStrategy;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FavoriteDestinationStrategyTest {


    //if i am at port, I should be told to move favorite tile
    @Test
    public void depart() throws Exception {
        Fisher fisher = mock(Fisher.class);
        SeaTile portTile = new SeaTile(0,0,1);
        SeaTile favoriteTile = new SeaTile(1,1,-1);
        Port port = mock(Port.class); when(port.getLocation()).thenReturn(portTile);
        when(fisher.getHomePort()).thenReturn(port);

        when(fisher.getDestination()).thenReturn(portTile);
        when(fisher.getLocation()).thenReturn(portTile);
        when(fisher.isAtDestination()).thenReturn(true);

        FavoriteDestinationStrategy strategy = new FavoriteDestinationStrategy(favoriteTile);
        SeaTile destination = strategy.chooseDestination(fisher, new MersenneTwisterFast(), mock(FishState.class),
                                                  mock(AtPort.class));

        assertEquals(destination,favoriteTile);


    }


    //if i am on the move but not there yet, I should be told to keep going
    @Test
    public void keepGoing() throws Exception {
        Fisher fisher = mock(Fisher.class);
        SeaTile portTile = new SeaTile(0,0,1);
        SeaTile favoriteTile = new SeaTile(1,1,-1);
        SeaTile transitTile = new SeaTile(1,0,-1);
        Port port = mock(Port.class); when(port.getLocation()).thenReturn(portTile);
        when(fisher.getHomePort()).thenReturn(port);

        when(fisher.getDestination()).thenReturn(favoriteTile);
        when(fisher.getLocation()).thenReturn(transitTile);
        when(fisher.isAtDestination()).thenReturn(false);

        FavoriteDestinationStrategy strategy = new FavoriteDestinationStrategy(favoriteTile);
        SeaTile destination = strategy.chooseDestination(fisher, new MersenneTwisterFast(), mock(FishState.class),
                                                         mock(Moving.class));

        assertEquals(destination,favoriteTile);


    }

    //if I arrived I should be told to go back to port
    @Test
    public void goBack() throws Exception {
        Fisher fisher = mock(Fisher.class);
        SeaTile portTile = new SeaTile(0,0,1);
        SeaTile favoriteTile = new SeaTile(1,1,-1);
        Port port = mock(Port.class); when(port.getLocation()).thenReturn(portTile);
        when(fisher.getHomePort()).thenReturn(port);

        when(fisher.getDestination()).thenReturn(favoriteTile);
        when(fisher.getLocation()).thenReturn(favoriteTile);
        when(fisher.isAtDestination()).thenReturn(true);

        FavoriteDestinationStrategy strategy = new FavoriteDestinationStrategy(favoriteTile);
        SeaTile destination = strategy.chooseDestination(fisher, new MersenneTwisterFast(),
                                                         mock(FishState.class),
                                                         mock(Arriving.class));

        assertEquals(destination,portTile);


    }

    //if I am going home, I keep going home
    @Test
    public void keepGoingBack() throws Exception {
        Fisher fisher = mock(Fisher.class);
        SeaTile portTile = new SeaTile(0,0,1);
        SeaTile favoriteTile = new SeaTile(1,1,-1);
        SeaTile transitTile = new SeaTile(1,0,-1);

        Port port = mock(Port.class); when(port.getLocation()).thenReturn(portTile);
        when(fisher.getHomePort()).thenReturn(port);

        when(fisher.getDestination()).thenReturn(portTile);
        when(fisher.getLocation()).thenReturn(transitTile);
        when(fisher.isAtDestination()).thenReturn(false);

        FavoriteDestinationStrategy strategy = new FavoriteDestinationStrategy(favoriteTile);
        SeaTile destination = strategy.chooseDestination(fisher, new MersenneTwisterFast(),
                                                         mock(FishState.class),
                                                         mock(Moving.class));

        assertEquals(destination,portTile);


    }

}