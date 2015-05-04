package uk.ac.ox.oxfish.model;

import org.junit.Test;
import uk.ac.ox.oxfish.model.data.DataGatherer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;


public class DataGathererTest {


    @Test
    public void gathersCorrectly() throws Exception {

        DataGatherer<String> gatherer = new DataGatherer<String>(true) {
        };

        FishState state = mock(FishState.class);
        gatherer.start(state, "12345");
        gatherer.registerGather("column1", Double::valueOf, -1);

        gatherer.registerGather("column2", s -> Double.valueOf(s.substring(0, 1)), -1);

        gatherer.step(state);
        gatherer.step(state);

        assertEquals(gatherer.getDataView().values().iterator().next().size(), 2);
        assertEquals(gatherer.getDataView().get("column1").get(0), 12345, .0001);
        assertEquals(gatherer.getDataView().get("column1").get(1), 12345, .0001);
        assertEquals(gatherer.getDataView().get("column2").get(0), 1, .0001);
        assertEquals(gatherer.getDataView().get("column2").get(1), 1, .0001);

        gatherer.registerGather("column3", s -> Double.valueOf(s.substring(1, 2)), -1);
        assertEquals(gatherer.getDataView().size(), 3);
        //old stuff hasn't changed, hopefully
        assertEquals(gatherer.getDataView().values().iterator().next().size(), 2);
        assertEquals(gatherer.getDataView().get("column2").get(0), 1, .0001);
        assertEquals(gatherer.getDataView().get("column2").get(1), 1, .0001);
        //new stuff is filled with default
        assertEquals(gatherer.getDataView().get("column3").get(0), -1, .0001);
        assertEquals(gatherer.getDataView().get("column3").get(1),-1 ,.0001);

        //and it collects
        gatherer.step(state);
        assertEquals(gatherer.getDataView().values().iterator().next().size(), 3);
        assertEquals(gatherer.getDataView().get("column3").get(2), 2, .0001);
    }
}