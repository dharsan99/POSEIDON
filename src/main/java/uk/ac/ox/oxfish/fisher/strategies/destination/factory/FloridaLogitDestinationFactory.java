package uk.ac.ox.oxfish.fisher.strategies.destination.factory;

import uk.ac.ox.oxfish.fisher.heatmap.regression.numerical.ObservationExtractor;
import uk.ac.ox.oxfish.fisher.strategies.destination.FavoriteDestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.destination.LogitDestinationStrategy;
import uk.ac.ox.oxfish.geography.discretization.MapDiscretization;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.scenario.OsmoseWFSScenario;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.CsvColumnsToLists;
import uk.ac.ox.oxfish.utility.FishStateUtilities;
import uk.ac.ox.oxfish.utility.Locker;

import java.nio.file.Paths;
import java.util.LinkedList;

/**
 * Steve saul's stuff, initialized here.
 * Created by carrknight on 12/6/16.
 */
public class FloridaLogitDestinationFactory implements AlgorithmFactory<LogitDestinationStrategy> {


    /**
     * file containing all the betas
     */
    private String coefficientsFile =
            FishStateUtilities.getAbsolutePath(
                    Paths.get("temp_wfs", "longline.csv").toString());


    private String coefficientsStandardDeviationFile =
            FishStateUtilities.getAbsolutePath(
                    Paths.get("temp_wfs", "longline_std.csv").toString());

    /**
     * file containing all the centroids
     */
    private String centroidFile =
            FishStateUtilities.getAbsolutePath(
                    Paths.get("temp_wfs", "areas.txt").toString());


    //



    /**
     * everybody shares the parent same destination logit strategy
     */
    private Locker<FishState,MapDiscretization> discretizationLocker = new Locker<>();
    /**
     * Applies this function to the given argument.
     *
     * @param state the function argument
     * @return the function result
     */
    @Override
    public LogitDestinationStrategy apply(FishState state) {

        MapDiscretization discretization = discretizationLocker.
                presentKey(state, () -> OsmoseWFSScenario.createDiscretization(state, centroidFile)
                );

        CsvColumnsToLists reader = new CsvColumnsToLists(
                coefficientsFile,
                ',',
                new String[]{"area", "intercept", "distance", "habit", "fuel_price", "wind_speed"}
        );
        LinkedList<Double>[] lists = reader.readColumns();
        reader = new CsvColumnsToLists(
                coefficientsStandardDeviationFile,
                ',',
                new String[]{"area", "intercept", "distance", "habit", "fuel_price", "wind_speed"}
        );
        LinkedList<Double>[] std =  reader.readColumns();
        LinkedList<Integer> rowNames = new LinkedList<Integer>();
        double[][] betas = new double[lists[0].size()][];
        for (int i = 0; i < lists[0].size(); i++) {
            //record which site this belongs to
            rowNames.add(lists[0].get(i).intValue());
            //record its coordinates
            betas[i] = new double[lists.length - 1];
            for (int j = 1; j < lists.length; j++)
                betas[i][j - 1] = lists[j].get(i) + state.getRandom().nextGaussian() * std[j].get(i) ;

        }
        ObservationExtractor[][] extractors = new ObservationExtractor[betas.length][];
        ObservationExtractor[] commonExtractor =
                    longlineFloridaCommonExtractor(discretization);
        for (int i = 0; i < extractors.length; i++)
            extractors[i] = commonExtractor;

        return new LogitDestinationStrategy(betas, extractors, rowNames, discretization,
                                            new FavoriteDestinationStrategy(state.getMap(), state.getRandom()),
                                            state.getRandom());

    }

    public static ObservationExtractor[] longlineFloridaCommonExtractor(
            MapDiscretization discretization) {
        return new ObservationExtractor[]{
                //intercept
                (tile, timeOfObservation, agent, model) -> 1d,
                //distance
                (tile, timeOfObservation, agent, model) -> {
                    return model.getMap().distance(
                            agent.getHomePort().getLocation(), tile);
                },
                //habit
                (tile, timeOfObservation, agent, model) -> {
                    //it it has been less than 90 days since you went there, you get the habit bonus!
                    return  model.getDay() -
                            agent.getDiscretizedLocationMemory()
                                    .getLastDayVisited()[discretization.getGroup(tile)] < 90 ?
                            1 : 0;
                },
                //fuel_price TODO: adjust from gallon
                (tile, timeOfObservation, agent, model) ->
                        agent.getHomePort().getGasPricePerLiter(),
                //wind_speed TODO: adjust from mph
                (tile, timeOfObservation, agent, model) -> tile.getWindSpeedInKph()
        };
    }

    public String getCoefficientsFile() {
        return coefficientsFile;
    }

    public void setCoefficientsFile(String coefficientsFile) {
        this.coefficientsFile = coefficientsFile;
    }

    public String getCoefficientsStandardDeviationFile() {
        return coefficientsStandardDeviationFile;
    }

    public void setCoefficientsStandardDeviationFile(String coefficientsStandardDeviationFile) {
        this.coefficientsStandardDeviationFile = coefficientsStandardDeviationFile;
    }

    public String getCentroidFile() {
        return centroidFile;
    }

    public void setCentroidFile(String centroidFile) {
        this.centroidFile = centroidFile;
    }
}