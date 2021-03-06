package uk.ac.ox.oxfish.experiments.indonesia.limited;

import com.google.common.base.Preconditions;
import com.opencsv.CSVReader;
import sim.engine.SimState;
import sim.engine.Steppable;
import uk.ac.ox.oxfish.experiments.indonesia.Slice6Sweeps;
import uk.ac.ox.oxfish.geography.ports.Port;
import uk.ac.ox.oxfish.model.AdditionalStartable;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.StepOrder;
import uk.ac.ox.oxfish.model.market.FixedPriceMarket;
import uk.ac.ox.oxfish.model.market.Market;
import uk.ac.ox.oxfish.model.market.MarketProxy;
import uk.ac.ox.oxfish.model.scenario.FlexibleScenario;
import uk.ac.ox.oxfish.model.scenario.Scenario;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.FishStateUtilities;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class NoData718Slice6Policy {

    public static final String CANDIDATES_CSV_FILE = "total_successes.csv";
    public static final int SEED = 0;
    private static Path OUTPUT_FOLDER =
            NoData718Slice6.MAIN_DIRECTORY.resolve("outputs");



    static public LinkedHashMap<String, Function<Integer, Consumer<Scenario>>> policies = new LinkedHashMap();


    private static Function<Integer,Consumer<Scenario>> decreasePricesForAllSpeciesByAPercentage(double taxRate) {

        return new Function<Integer, Consumer<Scenario>>() {
            public Consumer<Scenario> apply(Integer shockYear) {


                return new Consumer<Scenario>() {

                    @Override
                    public void accept(Scenario scenario) {

                        ((FlexibleScenario) scenario).getPlugins().add(
                                new AlgorithmFactory<AdditionalStartable>() {
                                    @Override
                                    public AdditionalStartable apply(FishState state) {

                                        return new AdditionalStartable() {
                                            @Override
                                            public void start(FishState model) {

                                                model.scheduleOnceAtTheBeginningOfYear(
                                                        new Steppable() {
                                                            @Override
                                                            public void step(SimState simState) {

                                                                //shock the prices
                                                                for (Port port : ((FishState) simState).getPorts()) {
                                                                    for (Market market : port.getDefaultMarketMap().getMarkets()) {

                                                                        if(port.getName().equals("Port 0")) {
                                                                            final FixedPriceMarket delegate = (FixedPriceMarket) ((MarketProxy) market).getDelegate();
                                                                            delegate.setPrice(
                                                                                    delegate.getPrice() * (1 - taxRate)
                                                                            );
                                                                        }
                                                                        else {

                                                                            final FixedPriceMarket delegate = ((FixedPriceMarket) ((MarketProxy) ((MarketProxy) market).getDelegate()).getDelegate());
                                                                            delegate.setPrice(
                                                                                    delegate.getPrice() * (1 - taxRate)
                                                                            );
                                                                        }
                                                                    }
                                                                }

                                                            }
                                                        }, StepOrder.DAWN, shockYear);

                                            }
                                        };


                                    }
                                });


                    }
                };
            }

            ;

        };
    }



    static {


        for(double yearlyReduction = .01; yearlyReduction<=.05; yearlyReduction= FishStateUtilities.round5(yearlyReduction+.005)) {
            double finalYearlyReduction = yearlyReduction;
            policies.put(
                    yearlyReduction+"_yearlyReduction_noentry",
                    shockYear -> Slice6Sweeps.setupFleetReductionConsumer(
                            shockYear,
                            finalYearlyReduction
                    ).andThen(
                            NoDataPolicy.removeEntry(shockYear)
                    )

            );
        }

        policies.put(
                "BAU",
                shockYear -> scenario -> {
                }

        );


        policies.put(
                "noentry",
                shockYear -> NoDataPolicy.removeEntry(shockYear)

        );


        for(int days = 250; days>=100; days-=10) {
            int finalDays = days;
            policies.put(
                    days+"_days_noentry",
                    shockYear -> NoDataPolicy.buildMaxDaysRegulation(shockYear,
                            new String[]{"population0", "population1", "population2"}
                            , finalDays).andThen(
                            NoDataPolicy.removeEntry(shockYear)
                    )

            );
        }



        policies.put(
                "tax_20",
                shockYear -> NoDataPolicy.removeEntry(shockYear).andThen(
                        decreasePricesForAllSpeciesByAPercentage(.2d).apply(shockYear)
                )

        );


    }




    public static void main(String[] args) throws IOException {

        runPolicyDirectory(
                OUTPUT_FOLDER.getParent().resolve(CANDIDATES_CSV_FILE).toFile(),
                OUTPUT_FOLDER,
                policies);


    }

    public static void runPolicyDirectory(File candidateFile,
                                          Path outputFolder,
                                          LinkedHashMap<String, Function<Integer, Consumer<Scenario>>> policies) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(
                candidateFile
        ));

        List<String[]> strings = reader.readAll();
        for (int i = 1; i < strings.size(); i++) {

            String[] row = strings.get(i);
            runOnePolicySimulation(
                    Paths.get(row[0]),
                    Integer.parseInt(row[1]),
                    Integer.parseInt(row[2]), outputFolder, policies
            );
        }
    }


    private static void runOnePolicySimulation(Path scenarioFile,
                                               int yearOfPriceShock,
                                               int yearOfPolicyShock,
                                               Path outputFolder,
                                               LinkedHashMap<String, Function<Integer,
                                                       Consumer<Scenario>>> policies) throws IOException {



        List<String> additionalColumns = new LinkedList<>();
        for (String species : NoData718Slice1.validSpecies) {
            final String agent = NoData718Slice2PriceIncrease.speciesToSprAgent.get(species);
            Preconditions.checkNotNull(agent, "species has no agent!");
            additionalColumns.add("SPR " + species + " " + agent + "_small");
        }
        NoData718Slice4PriceIncrease.priceIncreaseOneRun(
                scenarioFile,
                yearOfPolicyShock,
                outputFolder,
                policies,
                null,
                true,
                NoData718Slice4PriceIncrease.priceShockAndSeedingGenerator(0).
                        apply(yearOfPriceShock)

        );


    }

}
