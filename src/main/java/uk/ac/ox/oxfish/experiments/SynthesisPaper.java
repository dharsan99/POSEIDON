package uk.ac.ox.oxfish.experiments;


import com.esotericsoftware.minlog.Log;
import uk.ac.ox.oxfish.fisher.selfanalysis.factory.CutoffPerTripObjectiveFactory;
import uk.ac.ox.oxfish.fisher.strategies.destination.factory.PerTripImitativeDestinationFactory;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.data.Gatherer;
import uk.ac.ox.oxfish.model.data.collectors.DataColumn;
import uk.ac.ox.oxfish.model.data.collectors.TowHeatmapGatherer;
import uk.ac.ox.oxfish.model.regs.Regulation;
import uk.ac.ox.oxfish.model.regs.factory.AnarchyFactory;
import uk.ac.ox.oxfish.model.regs.factory.MultiITQStringFactory;
import uk.ac.ox.oxfish.model.regs.factory.ProtectedAreasOnlyFactory;
import uk.ac.ox.oxfish.model.scenario.PrototypeScenario;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.FishStateUtilities;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;
import uk.ac.ox.oxfish.utility.yaml.FishYAML;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class SynthesisPaper {


    public static void main(String[] args) throws IOException {
        Log.set(com.esotericsoftware.minlog.Log.LEVEL_INFO);
        /*
        avoidTheLine(100,
                     Paths.get("inputs","paper_synthesis"),
                     Paths.get("runs","paper_synthesis"));
                     */
        thresholdSweeps(25,
                        Paths.get("inputs","paper_synthesis"),
                        Paths.get("runs","paper_synthesis")
                        );
    }

    /**
     * run multiple times a scenario with 2 species where one is only
     * loosely protected by an MPA in the first scenario and by both an MPA and an ITQ
     * in the second instance
     * @param runsPerScenario number of runs per regulation
     * @param inputFolder
     * @param outputFolder
     */
    public static void  avoidTheLine(
            int runsPerScenario, Path inputFolder,
            Path outputFolder) throws IOException {


        Log.info("DEMO-1");
        String scenarioYaml = String.join("\n", Files.readAllLines(inputFolder.resolve("avoid_the_line.yaml")));

        Path container = outputFolder.resolve("demo1");
        container.toFile().mkdirs();


        Supplier<AlgorithmFactory<? extends Regulation>> regulation = new Supplier<AlgorithmFactory<? extends Regulation>>() {
            @Override
            public AlgorithmFactory<? extends Regulation> get() {
                return new ProtectedAreasOnlyFactory();
            }
        };

        demo1Sweep(runsPerScenario, scenarioYaml, container, regulation,"mpa");


        regulation = new Supplier<AlgorithmFactory<? extends Regulation>>() {
            @Override
            public AlgorithmFactory<? extends Regulation> get() {
                return new AnarchyFactory();
            }
        };

        demo1Sweep(runsPerScenario, scenarioYaml, container, regulation,"anarchy");

        regulation = new Supplier<AlgorithmFactory<? extends Regulation>>() {
            @Override
            public AlgorithmFactory<? extends Regulation> get() {
                MultiITQStringFactory factory = new MultiITQStringFactory();
                factory.setYearlyQuotaMaps("1:500");
                return factory;
            }
        };

        demo1Sweep(runsPerScenario, scenarioYaml, container, regulation,"itq");


    }

    public static void demo1Sweep(
            int numberOfRuns, String readScenario, Path outputFolder,
            Supplier<AlgorithmFactory<? extends Regulation>> regulation, final String name) throws IOException {
        for(int run = 0; run< numberOfRuns; run++) {
            FishYAML reader = new FishYAML();
            PrototypeScenario scenario = reader.loadAs(readScenario, PrototypeScenario.class);
            Log.info("\tMPA CASE " + run);
            scenario.setRegulation(regulation.get());

            FishState state = new FishState(run);
            state.setScenario(scenario);

            //add tows on the line counter (the neighborhood is size 2 because that's the size of the border where
            //blue fish live and is not protected)
            DataColumn borders = state.getDailyDataSet().registerGatherer("Tows on the Line",
                                                                          (Gatherer<FishState>) state1 -> {

                                                                              double lineSum = 0;
                                                                              NauticalMap map = state1.getMap();
                                                                              for (SeaTile tile : map.getAllSeaTilesExcludingLandAsList()) {
                                                                                  int trawlsHere = map.getDailyTrawlsMap().get(
                                                                                          tile.getGridX(),
                                                                                          tile.getGridY());
                                                                                  if (!tile.isProtected() &&
                                                                                          map.getMooreNeighbors(tile,
                                                                                                                2).stream().anyMatch(
                                                                                                  o -> ((SeaTile) o).isProtected())) {
                                                                                      lineSum += trawlsHere;
                                                                                  }
                                                                              }

                                                                              return lineSum;

                                                                          }
                    , Double.NaN);
            //now just count all tows
            DataColumn totals = state.getDailyDataSet().registerGatherer("Tows",
                                                                       (Gatherer<FishState>) state1 -> {

                                                                           double lineSum = 0;
                                                                           NauticalMap map = state1.getMap();
                                                                           for (SeaTile tile : map.getAllSeaTilesExcludingLandAsList()) {
                                                                               int trawlsHere = map.getDailyTrawlsMap().get(
                                                                                       tile.getGridX(),
                                                                                       tile.getGridY());
                                                                               lineSum += trawlsHere;
                                                                           }
                                                                           return lineSum;
                                                                       }
                    , Double.NaN);


            //collect a picture of heatmap for the first run
            TowHeatmapGatherer mapper = null;
            if(run==0) {
                mapper = new TowHeatmapGatherer(0);
                state.registerStartable(mapper);
            }




            state.start();
            while(state.getYear()<20)
                state.schedule.step(state);


            if(run==0) {
                String grid = FishStateUtilities.gridToCSV(mapper.getTowHeatmap());
                Files.write(outputFolder.resolve("grid" + name + ".csv"), grid.getBytes());
            }

            FishStateUtilities.printCSVColumnsToFile(outputFolder.resolve(name+"_"+run+".csv").toFile(),
                                                     totals,
                                                     borders);


        }
    }


    public static void thresholdSweeps(int runsPerScenario, Path inputFolder, Path outputFolder) throws IOException {

        String readScenario = String.join("\n", Files.readAllLines(inputFolder.resolve("fronts.yaml")));

        FileWriter writer = new FileWriter(outputFolder.resolve("demo2.csv").toFile());
        writer.write("low_threshold,upper_threshold,run,final_biomass\n");
        writer.flush();

        //baseline
        for(int run=0; run<runsPerScenario; run++) {
            FishYAML reader = new FishYAML();
            PrototypeScenario scenario = reader.loadAs(readScenario, PrototypeScenario.class);
            Log.info("\tStandard Fronts " + run);

            FishState state = new FishState(run);
            state.setScenario(scenario);
            state.start();
            while(state.getYear()<10)
                state.schedule.step(state);
            Double lastBiomass = state.getDailyDataSet().getLatestObservation("Biomass Species 0");
            writer.write("none" + ","  + "none" + ","+ run + "," + lastBiomass +"\n");
            writer.flush();

        }

        //go by upper thresholds
        for(double upperThreshold = 1; upperThreshold<20; upperThreshold++)
        {
            for(int run=0; run<runsPerScenario; run++)
            {
                FishYAML reader = new FishYAML();
                PrototypeScenario scenario = reader.loadAs(readScenario, PrototypeScenario.class);
                Log.info("\tHigh threshold " + upperThreshold + " --- "+ run);

                FishState state = new FishState(run);
                CutoffPerTripObjectiveFactory objectiveFunction = new CutoffPerTripObjectiveFactory();
                ((PerTripImitativeDestinationFactory) scenario.getDestinationStrategy()).setObjectiveFunction(
                        objectiveFunction);
                objectiveFunction.getHighThreshold().setActive(true);
                objectiveFunction.getHighThreshold().setValue(new FixedDoubleParameter(upperThreshold));
                state.setScenario(scenario);
                state.start();
                while(state.getYear()<10)
                    state.schedule.step(state);
                Double lastBiomass = state.getDailyDataSet().getLatestObservation("Biomass Species 0");
                writer.write("none" + ","  + upperThreshold + ","+ run + "," + lastBiomass+"\n");
                writer.flush();

            }
        }

        //go by lower thresholds
        for(double lowerThreshold = 1; lowerThreshold<20; lowerThreshold++)
        {
            for(int run=0; run<runsPerScenario; run++)
            {
                FishYAML reader = new FishYAML();
                PrototypeScenario scenario = reader.loadAs(readScenario, PrototypeScenario.class);
                Log.info("\tLow threshold " + lowerThreshold + " --- "+ run);

                FishState state = new FishState(run);
                CutoffPerTripObjectiveFactory objectiveFunction = new CutoffPerTripObjectiveFactory();
                ((PerTripImitativeDestinationFactory) scenario.getDestinationStrategy()).setObjectiveFunction(
                        objectiveFunction);
                objectiveFunction.getLowThreshold().setActive(true);
                objectiveFunction.getLowThreshold().setValue(new FixedDoubleParameter(lowerThreshold));
                state.setScenario(scenario);
                state.start();
                while(state.getYear()<10)
                    state.schedule.step(state);
                Double lastBiomass = state.getDailyDataSet().getLatestObservation("Biomass Species 0");
                writer.write(lowerThreshold + ","  + "none" + ","+ run + "," + lastBiomass+"\n");
                writer.flush();

            }
        }
        writer.close();

    }
}