package uk.ac.ox.oxfish.model;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ox.oxfish.fisher.Port;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.NauticalMapFactory;

import static org.junit.Assert.*;

/**
 * Testing the map itself
 * Created by carrknight on 4/3/15.
 */
public class NauticalMapTest {


    @Test
    public void readTilesDepthCorrectly() {


        NauticalMap map = NauticalMapFactory.fromBathymetryAndShapeFiles("test.asc", "fakempa.shp");
        //open the test grid and the test mpas

        //now, the grid ought to be simple: the altitude increases for each element from the row
        assertEquals(map.getRasterBathymetry().getGridWidth(),72);
        assertEquals(map.getRasterBathymetry().getGridHeight(),36);
        assertEquals(map.getSeaTile(0,0).getAltitude(),1,.0001);
        assertEquals(map.getSeaTile(1,0).getAltitude(),2,.0001);
        assertEquals(map.getSeaTile(71,35).getAltitude(), 2592,.0001);



    }

    @Test
    public void readDistancesCorrectly() {


        //test2.asc is like test.asc but it should be so that lower-left corner center grid is exactly lat0,long0 and
        //grid size is 1
        NauticalMap map = NauticalMapFactory.fromBathymetryAndShapeFiles("test2.asc", "fakempa.shp");
        //open the test grid and the test mpas

        //now, the grid ought to be simple: the altitude increases for each element from the row
        //here I assumed the distance is computed by the EquirectangularDistance object. Could change
        assertEquals(map.distance(0, 0, 3, 3), 471.8, .1);



    }

    @Test
    public void readMPAsCorrectly() {


        NauticalMap map = NauticalMapFactory.fromBathymetryAndShapeFiles("test.asc", "fakempa.shp");

        //there is only one big square MPA in the middle. So at the borders we ought not to be protected
        assertFalse(map.getSeaTile(0,0).isProtected());
        assertFalse(map.getSeaTile(1,0).isProtected());
        assertFalse(map.getSeaTile(71, 35).isProtected());
        //but right in the middle we are
        assertTrue(map.getSeaTile(35, 17).isProtected());




    }


    @Test
    public void addPortsCorrectly()
    {


        //read the 5by5 asc
        NauticalMap map = NauticalMapFactory.fromBathymetryAndShapeFiles("5by5.asc");
        //it has 3 sea columns and then 2 columns of land
        //I can put a port for each coastal land port
        for(int row=0; row<5; row++)
        {
            Port port = new Port(map.getSeaTile(3,row));
            map.addPort(port);
            map.getPorts().contains(port);
            assertEquals(map.getPortMap().getObjectLocation(port).x,3);
            assertEquals(map.getPortMap().getObjectLocation(port).y,row);
            assertEquals(map.getPortMap().getObjectsAtLocation(3,row).size(),1);
            assertEquals(map.getPortMap().getObjectsAtLocation(3,row).get(0),port);
        }
        //no exceptions thrown
        assertEquals(5, map.getPorts().size());

    }

    @Test(expected=IllegalArgumentException.class)
    public void addPortsOnSeaIsWrong()
    {
        NauticalMap map = NauticalMapFactory.fromBathymetryAndShapeFiles("5by5.asc");
        map.addPort(new Port(map.getSeaTile(2, 0))); //throws exception since the seatile is underwater
    }

    @Test(expected=IllegalArgumentException.class)
    public void addPortsAwayFromSeaIsWrong()
    {
        NauticalMap map = NauticalMapFactory.fromBathymetryAndShapeFiles("5by5.asc");
        map.addPort(new Port(map.getSeaTile(4,0))); //it's on land but there is no sea around.
    }



}