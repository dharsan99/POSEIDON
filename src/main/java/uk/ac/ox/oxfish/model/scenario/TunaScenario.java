/*
 *  POSEIDON, an agent-based model of fisheries
 *  Copyright (C) 2020  CoHESyS Lab cohesys.lab@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package uk.ac.ox.oxfish.model.scenario;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.vividsolutions.jts.geom.Coordinate;
import ec.util.MersenneTwisterFast;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import sim.engine.Steppable;
import tech.units.indriya.ComparableQuantity;
import uk.ac.ox.oxfish.biology.GlobalBiology;
import uk.ac.ox.oxfish.biology.complicated.factory.SnapshotBiomassResetterFactory;
import uk.ac.ox.oxfish.biology.growers.FadAwareCommonLogisticGrowerInitializerFactory;
import uk.ac.ox.oxfish.biology.initializer.BiologyInitializer;
import uk.ac.ox.oxfish.biology.initializer.MultipleIndependentSpeciesBiomassInitializer;
import uk.ac.ox.oxfish.biology.initializer.allocator.ConstantAllocatorFactory;
import uk.ac.ox.oxfish.biology.initializer.allocator.FileBiomassAllocatorFactory;
import uk.ac.ox.oxfish.biology.initializer.allocator.PolygonAllocatorFactory;
import uk.ac.ox.oxfish.biology.initializer.allocator.SmootherFileAllocatorFactory;
import uk.ac.ox.oxfish.biology.initializer.factory.MultipleIndependentSpeciesBiomassFactory;
import uk.ac.ox.oxfish.biology.initializer.factory.SingleSpeciesBiomassNormalizedFactory;
import uk.ac.ox.oxfish.biology.weather.initializer.WeatherInitializer;
import uk.ac.ox.oxfish.biology.weather.initializer.factory.ConstantWeatherFactory;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.actions.fads.DeployFad;
import uk.ac.ox.oxfish.fisher.actions.fads.FadAction;
import uk.ac.ox.oxfish.fisher.actions.fads.MakeFadSet;
import uk.ac.ox.oxfish.fisher.actions.fads.MakeUnassociatedSet;
import uk.ac.ox.oxfish.fisher.actions.fads.Regions;
import uk.ac.ox.oxfish.fisher.actions.fads.SetAction;
import uk.ac.ox.oxfish.fisher.equipment.Boat;
import uk.ac.ox.oxfish.fisher.equipment.Engine;
import uk.ac.ox.oxfish.fisher.equipment.FuelTank;
import uk.ac.ox.oxfish.fisher.equipment.Hold;
import uk.ac.ox.oxfish.fisher.equipment.fads.Fad;
import uk.ac.ox.oxfish.fisher.equipment.gear.factory.PurseSeineGearFactory;
import uk.ac.ox.oxfish.fisher.equipment.gear.fads.PurseSeineGear;
import uk.ac.ox.oxfish.fisher.selfanalysis.profit.HourlyCost;
import uk.ac.ox.oxfish.fisher.strategies.departing.PurseSeineDepartingStrategyFactory;
import uk.ac.ox.oxfish.fisher.strategies.destination.factory.FadDestinationStrategyFactory;
import uk.ac.ox.oxfish.fisher.strategies.destination.fad.FadDestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.destination.fad.FadGravityDestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.fishing.factory.FadFishingStrategyFactory;
import uk.ac.ox.oxfish.geography.CumulativeTravelTimeCachingDecorator;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.NauticalMapFactory;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.geography.currents.CurrentPattern;
import uk.ac.ox.oxfish.geography.fads.FadMap;
import uk.ac.ox.oxfish.geography.fads.FadMapFactory;
import uk.ac.ox.oxfish.geography.mapmakers.FromFileMapInitializerFactory;
import uk.ac.ox.oxfish.geography.pathfinding.AStarFallbackPathfinder;
import uk.ac.ox.oxfish.geography.ports.FromSimpleFilePortInitializer;
import uk.ac.ox.oxfish.geography.ports.Port;
import uk.ac.ox.oxfish.model.AdditionalStartable;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.StepOrder;
import uk.ac.ox.oxfish.model.data.Gatherer;
import uk.ac.ox.oxfish.model.event.BiomassDrivenTimeSeriesExogenousCatchesFactory;
import uk.ac.ox.oxfish.model.event.ExogenousCatches;
import uk.ac.ox.oxfish.model.market.FixedPriceMarket;
import uk.ac.ox.oxfish.model.market.MarketMap;
import uk.ac.ox.oxfish.model.market.gas.FixedGasPrice;
import uk.ac.ox.oxfish.model.market.gas.GasPriceMaker;
import uk.ac.ox.oxfish.model.network.EmptyNetworkBuilder;
import uk.ac.ox.oxfish.model.network.SocialNetwork;
import uk.ac.ox.oxfish.model.regs.MultipleRegulations;
import uk.ac.ox.oxfish.model.regs.Regulation;
import uk.ac.ox.oxfish.model.regs.factory.MultipleRegulationsFactory;
import uk.ac.ox.oxfish.model.regs.factory.NoFishingFactory;
import uk.ac.ox.oxfish.model.regs.factory.SpecificProtectedAreaFromCoordinatesFactory;
import uk.ac.ox.oxfish.model.regs.factory.SpecificProtectedAreaFromShapeFileFactory;
import uk.ac.ox.oxfish.model.regs.factory.TemporaryRegulationFactory;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;
import uk.ac.ox.oxfish.utility.parameters.DoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;

import javax.measure.Quantity;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Volume;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableRangeMap.toImmutableRangeMap;
import static java.time.Month.JANUARY;
import static java.time.Month.JULY;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static si.uom.NonSI.KNOT;
import static si.uom.NonSI.TONNE;
import static tech.units.indriya.quantity.Quantities.getQuantity;
import static tech.units.indriya.unit.Units.CUBIC_METRE;
import static tech.units.indriya.unit.Units.KILOGRAM;
import static tech.units.indriya.unit.Units.KILOMETRE_PER_HOUR;
import static uk.ac.ox.oxfish.fisher.actions.fads.FadAction.proportionGathererName;
import static uk.ac.ox.oxfish.fisher.actions.fads.FadAction.totalCounterName;
import static uk.ac.ox.oxfish.geography.currents.CurrentPattern.Y2017;
import static uk.ac.ox.oxfish.utility.MasonUtils.oneOf;
import static uk.ac.ox.oxfish.utility.Measures.asDouble;
import static uk.ac.ox.oxfish.utility.Measures.convert;
import static uk.ac.ox.oxfish.utility.csv.CsvParserUtil.parseAllRecords;

@SuppressWarnings("UnstableApiUsage")
public class TunaScenario implements Scenario {

    private static final Path INPUT_DIRECTORY = Paths.get("inputs", "tuna");
    public static final ImmutableMap<CurrentPattern, Path> currentFiles =
        new ImmutableMap.Builder<CurrentPattern, Path>()
            //.put(Y2015, input("currents_2015.csv"))
            //.put(Y2016, input("currents_2016.csv"))
            .put(Y2017, input("currents_2017.csv"))
            //.put(Y2018, input("currents_2018.csv"))
            //.put(NEUTRAL, input("currents_neutral.csv"))
            //.put(EL_NINO, input("currents_el_nino.csv"))
            //.put(LA_NINA, input("currents_la_nina.csv"))
            .build();
    private static final ImmutableMap<String, Path> biomassFiles = ImmutableMap.of(
        "BET", input("2017_BET_DIST.csv"),
        "SKJ", input("2017_SKJ_DIST.csv"),
        "YFT", input("2017_YFT_DIST.csv")
    );
    public static final BiMap<String, String> speciesNames =
        parseAllRecords(input("species_names.csv")).stream().collect(toImmutableBiMap(
            r -> r.getString("species_code"),
            r -> r.getString("species_name")
        ));
    private static final Path schaeferParamsFile = input("schaefer_params.csv");
    private final ImmutableList<String> actionNames = ImmutableList.of(
        DeployFad.ACTION_NAME,
        MakeFadSet.ACTION_NAME,
        MakeUnassociatedSet.ACTION_NAME
    );
    private final Set<Integer> regionNumbers = Regions.REGION_NAMES.keySet();
    private final ImmutableList<String> yearlyFisherCounters = Stream.of(
        actionNames.stream().map(FadAction::totalCounterName),
        regionNumbers.stream().flatMap(regionNumber ->
            actionNames.stream().map(actionName -> FadAction.regionCounterName(actionName, regionNumber))
        ),
        speciesNames.values().stream().flatMap(speciesName ->
            Stream.of(MakeFadSet.ACTION_NAME, MakeUnassociatedSet.ACTION_NAME).map(actionName ->
                SetAction.catchesCounterName(speciesName, actionName)
            )
        ),
        speciesNames.values().stream().map(Fad::biomassLostCounterName)
    ).flatMap(identity()).collect(toImmutableList());
    private final FromSimpleFilePortInitializer portInitializer = new FromSimpleFilePortInitializer(input("ports.csv"));
    private Path mapFile = input("depth.csv");
    private Path deploymentValuesFile = input("deployment_values.csv");
    private Path iattcShapeFile = input("iattc_area").resolve("RFB_IATTC.shp");
    private Path galapagosEezShapeFile = input("galapagos_eez").resolve("eez.shp");
    private Path pricesFile = input("prices.csv");
    private Path boatsFile = input("boats.csv");
    private Path fadCarryingCapacitiesFile = input("fad_carrying_capacities.csv");
    private Path unassociatedCatchSampleFile = input("unassociated_catch_sample.csv");
    private Path costsFile = input("costs.csv");
    private int targetYear = 2017;
    private boolean fadMortalityIncludedInExogenousCatches = true;
    private final BiomassDrivenTimeSeriesExogenousCatchesFactory exogenousCatchesFactory =
        new BiomassDrivenTimeSeriesExogenousCatchesFactory(
            input("exogenous_catches.csv"),
            targetYear,
            (fishState, speciesCode) -> fishState.getBiology().getSpecie(speciesNames.get(speciesCode)),
            fadMortalityIncludedInExogenousCatches
        );
    private FromFileMapInitializerFactory mapInitializer = new FromFileMapInitializerFactory(mapFile, 101, 0.5);
    private AlgorithmFactory<? extends WeatherInitializer> weatherInitializer = new ConstantWeatherFactory();
    private DoubleParameter gasPricePerLiter = new FixedDoubleParameter(0.01);
    private FisherDefinition fisherDefinition = new FisherDefinition();
    private AlgorithmFactory<? extends MultipleIndependentSpeciesBiomassInitializer> biologyInitializers =
        new MultipleIndependentSpeciesBiomassFactory(
            parseAllRecords(schaeferParamsFile).stream().map(r -> makeBiomassInitializerFactory(
                r.getString("species_code"),
                r.getDouble("logistic_growth_rate"), // logistic growth rate (r)
                getQuantity(r.getDouble("carrying_capacity_in_tonnes"), TONNE), // total carrying capacity (K)
                getQuantity(r.getDouble("total_biomass_in_tonnes"), TONNE) // total biomass
            )).collect(toList()),
            false,
            false
        );
    private List<AlgorithmFactory<? extends AdditionalStartable>> plugins;

    public TunaScenario() {

        final SnapshotBiomassResetterFactory snapshotBiomassResetterFactory = new SnapshotBiomassResetterFactory();
        snapshotBiomassResetterFactory.setRestoreOriginalLocations(true);
        plugins = Lists.newArrayList(snapshotBiomassResetterFactory);

        AlgorithmFactory<? extends Regulation> regulations = new MultipleRegulationsFactory(ImmutableMap.of(
            new SpecificProtectedAreaFromShapeFileFactory(galapagosEezShapeFile), "all",
            new TemporaryRegulationFactory( // El Corralito
                dayOfYear(OCTOBER, 9), dayOfYear(NOVEMBER, 8),
                new SpecificProtectedAreaFromCoordinatesFactory(4, -110, -3, -96)
            ), "all",
            new TemporaryRegulationFactory(
                dayOfYear(JULY, 29), dayOfYear(OCTOBER, 8),
                new NoFishingFactory()
            ), "closure A",
            new TemporaryRegulationFactory(
                dayOfYear(NOVEMBER, 9), dayOfYear(JANUARY, 19),
                new NoFishingFactory()
            ), "closure B"
        ));

        final PurseSeineGearFactory purseSeineGearFactory = new PurseSeineGearFactory();
        purseSeineGearFactory.getFadInitializerFactory().setCarryingCapacities(
            parseAllRecords(fadCarryingCapacitiesFile).stream()
                .filter(r -> r.getInt("year") == targetYear)
                .collect(toMap(
                    r -> speciesNames.get(r.getString("species_code")),
                    r -> convert(r.getDouble("k"), TONNE, KILOGRAM)
                ))
        );
        purseSeineGearFactory.getFadInitializerFactory().setAttractionRates(ImmutableMap.of(
            "Bigeye tuna", new FixedDoubleParameter(0.05),
            "Yellowfin tuna", new FixedDoubleParameter(0.0321960615),
            "Skipjack tuna", new FixedDoubleParameter(0.007183564999999999)
        ));
        purseSeineGearFactory.setUnassociatedCatchSampleFile(unassociatedCatchSampleFile);

        fisherDefinition.setRegulation(regulations);
        fisherDefinition.setGear(purseSeineGearFactory);
        fisherDefinition.setFishingStrategy(new FadFishingStrategyFactory());
        fisherDefinition.setDestinationStrategy(new FadDestinationStrategyFactory());
        fisherDefinition.setDepartingStrategy(new PurseSeineDepartingStrategyFactory());
    }

    public static Path input(String filename) { return INPUT_DIRECTORY.resolve(filename); }

    private int dayOfYear(Month month, int dayOfMonth) {
        return LocalDate.of(targetYear, month, dayOfMonth)
            .getDayOfYear();
    }

    @SuppressWarnings("unused") public Path getMapFile() { return mapFile; }

    @SuppressWarnings("unused") public void setMapFile(Path mapFile) { this.mapFile = mapFile; }

    @SuppressWarnings("unused") public Path getDeploymentValuesFile() { return deploymentValuesFile; }

    public void setDeploymentValuesFile(Path deploymentValuesFile) { this.deploymentValuesFile = deploymentValuesFile; }

    @SuppressWarnings("unused") public Path getIattcShapeFile() { return iattcShapeFile; }

    @SuppressWarnings("unused") public void setIattcShapeFile(Path iattcShapeFile) {
        this.iattcShapeFile = iattcShapeFile;
    }

    @SuppressWarnings("unused") public Path getGalapagosEezShapeFile() { return galapagosEezShapeFile; }

    @SuppressWarnings("unused") public void setGalapagosEezShapeFile(Path galapagosEezShapeFile) {
        this.galapagosEezShapeFile = galapagosEezShapeFile;
    }

    @SuppressWarnings("unused") public Path getPricesFile() { return pricesFile; }

    @SuppressWarnings("unused") public void setPricesFile(Path pricesFile) { this.pricesFile = pricesFile; }

    @SuppressWarnings("unused") public Path getBoatsFile() { return boatsFile; }

    public void setBoatsFile(Path boatsFile) { this.boatsFile = boatsFile; }

    @SuppressWarnings("unused") public Path getFadCarryingCapacitiesFile() { return fadCarryingCapacitiesFile; }

    @SuppressWarnings("unused") public void setFadCarryingCapacitiesFile(Path fadCarryingCapacitiesFile) {
        this.fadCarryingCapacitiesFile = fadCarryingCapacitiesFile;
    }

    @SuppressWarnings("unused") public Path getUnassociatedCatchSampleFile() { return unassociatedCatchSampleFile; }

    @SuppressWarnings("unused") public void setUnassociatedCatchSampleFile(Path unassociatedCatchSampleFile) {
        this.unassociatedCatchSampleFile = unassociatedCatchSampleFile;
    }

    public BiomassDrivenTimeSeriesExogenousCatchesFactory getExogenousCatchesFactory() {
        return exogenousCatchesFactory;
    }

    @SuppressWarnings("unused")
    public AlgorithmFactory<? extends MultipleIndependentSpeciesBiomassInitializer> getBiologyInitializers() {
        return biologyInitializers;
    }

    @SuppressWarnings("unused")
    public void setBiologyInitializers(AlgorithmFactory<? extends MultipleIndependentSpeciesBiomassInitializer> biologyInitializers) {
        this.biologyInitializers = biologyInitializers;
    }

    @SuppressWarnings("unused")
    public Path getPortFilePath() {
        return portInitializer.getFilePath();
    }

    @SuppressWarnings("unused")
    public void setPortFilePath(Path filePath) {
        portInitializer.setFilePath(filePath);
    }

    public FromFileMapInitializerFactory getMapInitializer() {
        return mapInitializer;
    }

    public void setMapInitializer(
        FromFileMapInitializerFactory mapInitializer
    ) {
        this.mapInitializer = mapInitializer;
    }

    @SuppressWarnings("unused")
    public AlgorithmFactory<? extends WeatherInitializer> getWeatherInitializer() {
        return weatherInitializer;
    }

    @SuppressWarnings("unused")
    public void setWeatherInitializer(
        AlgorithmFactory<? extends WeatherInitializer> weatherInitializer
    ) {
        this.weatherInitializer = weatherInitializer;
    }

    public DoubleParameter getGasPricePerLiter() {
        return gasPricePerLiter;
    }

    public void setGasPricePerLiter(DoubleParameter gasPricePerLiter) {
        this.gasPricePerLiter = gasPricePerLiter;
    }

    @SuppressWarnings("unused")
    public FisherDefinition getFisherDefinition() {
        return fisherDefinition;
    }

    @SuppressWarnings("unused")
    public void setFisherDefinition(FisherDefinition fisherDefinition) {
        this.fisherDefinition = fisherDefinition;
    }

    @Override
    public ScenarioEssentials start(FishState model) {
        final BiologyInitializer biologyInitializer = biologyInitializers.apply(model);
        final GlobalBiology globalBiology = biologyInitializer.generateGlobal(model.random, model);
        final NauticalMap nauticalMap = mapInitializer.apply(model).makeMap(model.random, globalBiology, model);
        nauticalMap.setDistance(new CumulativeTravelTimeCachingDecorator(nauticalMap.getDistance()));
        nauticalMap.setPathfinder(new AStarFallbackPathfinder(nauticalMap.getDistance()));

        //this next static method calls biology.initialize, weather.initialize and the like
        NauticalMapFactory.initializeMap(
            nauticalMap, model.random, biologyInitializer, this.weatherInitializer.apply(model), globalBiology, model
        );

        final Double gasPrice = gasPricePerLiter.apply(model.random);
        final GasPriceMaker gasPriceMaker = new FixedGasPrice(gasPrice);

        final MarketMap marketMap = makeMarketMap(globalBiology);
        portInitializer
            .buildPorts(nauticalMap, model.random, seaTile -> marketMap, model, gasPriceMaker)
            .forEach(port -> port.setGasPricePerLiter(gasPrice));

        return new ScenarioEssentials(globalBiology, nauticalMap);
    }

    private MarketMap makeMarketMap(GlobalBiology globalBiology) {
        Map<String, Double> prices = parseAllRecords(pricesFile).stream()
            .filter(
                r -> r.getInt("year") == targetYear
            )
            .collect(toMap(
                r -> r.getString("species_code"),
                r -> r.getDouble("price_per_tonne") / 1000.0 // convert to price / kg
            ));
        final MarketMap marketMap = new MarketMap(globalBiology);
        globalBiology.getSpecies().forEach(species -> {
            final String speciesCode = speciesNames.inverse().get(species.getName());
            marketMap.addMarket(species, new FixedPriceMarket(prices.get(speciesCode)));
        });
        return marketMap;
    }

    @Override
    public ScenarioPopulation populateModel(FishState model) {

        final FadMap fadMap = (new FadMapFactory()).apply(model);
        model.setFadMap(fadMap);
        model.registerStartable(fadMap);

        final LinkedList<Port> ports = model.getMap().getPorts();
        Preconditions.checkState(!ports.isEmpty());

        final RangeMap<ComparableQuantity<Mass>, HourlyCost> hourlyCostsPerCarryingCapacity =
            parseAllRecords(costsFile).stream().collect(toImmutableRangeMap(
                r -> Range.openClosed(
                    getQuantity(r.getInt("lower_capacity"), TONNE),
                    getQuantity(r.getInt("upper_capacity"), TONNE)
                ),
                r -> new HourlyCost(r.getDouble("daily_cost") / 24.0)
            ));

        FisherFactory fisherFactory = fisherDefinition.getFisherFactory(model, ports, 0);
        fisherFactory.getAdditionalSetups().add(fisher -> {
            // Setup hourly costs as a function of capacity
            final ComparableQuantity<Mass> capacity = getQuantity(fisher.getHold().getMaximumLoad(), KILOGRAM);
            final HourlyCost hourlyCost = hourlyCostsPerCarryingCapacity.get(capacity);
            fisher.getAdditionalTripCosts().add(hourlyCost);

            // Store a reference to the fisher in the FAD manager
            ((PurseSeineGear) fisher.getGear()).getFadManager().setFisher(fisher);

            // Add purse-seine-specific yearly counters to the fisher's memory
            yearlyFisherCounters.forEach(column -> fisher.getYearlyCounter().addColumn(column));

            // Every year, on July 15th, purse seine vessels must choose which temporal closure period they will observe.
            final int daysFromNow = 1 + dayOfYear(JULY, 15);
            Steppable assignClosurePeriod = simState -> {
                if (fisher.getRegulation() instanceof MultipleRegulations) {
                    chooseClosurePeriod(fisher, model.getRandom());
                    ((MultipleRegulations) fisher.getRegulation()).reassignRegulations(model, fisher);
                }
            };
            model.scheduleOnceInXDays(
                assignClosurePeriod,
                StepOrder.DAWN, daysFromNow
            );
            model.scheduleOnceInXDays(
                simState -> model.scheduleEveryXDay(assignClosurePeriod, StepOrder.DAWN, 365),
                StepOrder.DAWN, daysFromNow
            );
        });

        final Map<String, Port> portsByName = ports.stream().collect(toMap(Port::getName, identity()));

        final Supplier<FuelTank> fuelTankSupplier = () -> new FuelTank(Double.MAX_VALUE);

        final Map<String, Fisher> fishersByBoatId =
            parseAllRecords(boatsFile).stream()
                .filter(record -> record.getInt("year") == targetYear)
                //.limit(10)
                .collect(toMap(
                    record -> record.getString("boat_id"),
                    record -> {
                        final String portName = record.getString("port_name");
                        final Double length = record.getDouble("length_in_m");
                        final Quantity<Mass> carryingCapacity =
                            getQuantity(record.getDouble("carrying_capacity_in_t"), TONNE);
                        final double carryingCapacityInKg = asDouble(carryingCapacity, KILOGRAM);
                        final Quantity<Volume> holdVolume =
                            getQuantity(record.getDouble("hold_volume_in_m3"), CUBIC_METRE);
                        final Quantity<Speed> speed = getQuantity(record.getDouble("speed_in_knots"), KNOT);
                        final Engine engine = new Engine(
                            Double.NaN, // Unused
                            1.0, // This is not realistic, but fuel costs are wrapped into daily costs
                            asDouble(speed, KILOMETRE_PER_HOUR)
                        );
                        fisherFactory.setPortSupplier(() -> portsByName.get(portName));
                        // we don't have beam width in the data file, but it isn't used anyway
                        final double beam = 1.0;
                        fisherFactory.setBoatSupplier(() -> new Boat(length, beam, engine, fuelTankSupplier.get()));
                        fisherFactory.setHoldSupplier(() -> new Hold(
                            carryingCapacityInKg,
                            holdVolume,
                            model.getBiology()
                        ));
                        final Fisher fisher = fisherFactory.buildFisher(model);
                        fisher.getTags().add(record.getString("boat_id"));
                        chooseClosurePeriod(fisher, model.getRandom());
                        return fisher;
                    }
                ));

        assignDeploymentLocationValues(model.getMap(), fishersByBoatId);

        // Mutate the fisher factory back into a random boat generator
        // TODO: we don't have boat entry in the tuna model for now, but when we do, this shouldn't be entirely random
        fisherFactory.setBoatSupplier(fisherDefinition.makeBoatSupplier(model.random));
        fisherFactory.setHoldSupplier(fisherDefinition.makeHoldSupplier(model.random, model.getBiology()));
        fisherFactory.setPortSupplier(() -> oneOf(ports, model.random));

        final Map<String, FisherFactory> fisherFactories =
            ImmutableMap.of(FishState.DEFAULT_POPULATION_NAME, fisherFactory);

        final SocialNetwork network = new SocialNetwork(new EmptyNetworkBuilder());

        final ExogenousCatches exogenousCatches = exogenousCatchesFactory.apply(model);
        model.registerStartable(exogenousCatches);

        plugins.forEach(plugin -> model.registerStartable(plugin.apply(model)));
        registerGatherers(model);

        return new ScenarioPopulation(new ArrayList<>(fishersByBoatId.values()), network, fisherFactories);

    }

    private void chooseClosurePeriod(Fisher fisher, MersenneTwisterFast rng) {
        final ImmutableList<String> periods = ImmutableList.of("closure A", "closure B");
        fisher.getTags().removeIf(periods::contains);
        fisher.getTags().add(oneOf(periods, rng));
    }

    private void assignDeploymentLocationValues(
        NauticalMap nauticalMap,
        Map<String, ? extends Fisher> fishersByBoatId
    ) {
        final Map<String, Map<SeaTile, Double>> deploymentValuesPerBoatId =
            parseAllRecords(deploymentValuesFile).stream()
                .filter(record -> record.getInt("year") == targetYear)
                .map(record -> Triple.of( // oh, how I long for case classes...
                    record.getString("boat_id"),
                    nauticalMap.getSeaTile(new Coordinate(record.getDouble("lon"), record.getDouble("lat"))),
                    record.getDouble("value")
                ))
                .filter(triple -> {
                    final SeaTile seaTile = triple.getMiddle();
                    return seaTile != null && seaTile.isWater();
                })
                .collect(groupingBy(Triple::getLeft, toMap(Triple::getMiddle, Triple::getRight)));

        final Map<SeaTile, Double> defaultDeploymentValues =
            deploymentValuesPerBoatId.values().stream()
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .collect(toMap(identity(), x -> 0.0));

        //todo remove these soon

        fishersByBoatId.forEach((boatId, fisher) -> {
            if (fisher.getDestinationStrategy() instanceof FadDestinationStrategy) {
                final Map<SeaTile, Double> deploymentValues =
                    deploymentValuesPerBoatId.getOrDefault(boatId, defaultDeploymentValues);
                ((FadDestinationStrategy) fisher.getDestinationStrategy())
                    .getFadDeploymentRouteSelector()
                    .setDeploymentLocationValues(deploymentValues);
            }
        });

        fishersByBoatId.forEach((boatId, fisher) -> {
            if (fisher.getDestinationStrategy() instanceof FadGravityDestinationStrategy) {
                final Map<SeaTile, Double> deploymentValues =
                    deploymentValuesPerBoatId.getOrDefault(boatId, defaultDeploymentValues);
                ((FadGravityDestinationStrategy) fisher.getDestinationStrategy())
                    .getFadDeploymentRouteSelector()
                    .setDeploymentLocationValues(deploymentValues);
            }
        });

    }

    private void registerGatherers(FishState fishState) {
        yearlyFisherCounters.forEach(column ->
            fishState.getYearlyDataSet().registerGatherer(column, yearlyCounterAdder(column), 0.0)
        );
        regionNumbers.forEach(regionNumber ->
            actionNames.forEach(actionName ->
                fishState.getYearlyDataSet().registerGatherer(
                    proportionGathererName(actionName, regionNumber),
                    model -> yearlyCounterAdder(FadAction.regionCounterName(actionName, regionNumber)).apply(model) /
                        yearlyCounterAdder(totalCounterName(actionName)).apply(model),
                    0.0
                )
            )
        );
        fishState.getBiology().getSpecies().forEach(species ->
            fishState.getYearlyDataSet().registerGatherer(
                "Total " + species.getName() + " biomass under FADs",
                model -> model.getFadMap().getTotalBiomass(species),
                0.0
            )
        );
    }

    @NotNull private Gatherer<FishState> yearlyCounterAdder(String column) {
        return fishState ->
            fishState.getFishers().stream()
                .mapToDouble(fisher -> fisher.getYearlyCounter().getColumn(column))
                .sum();
    }

    @SuppressWarnings("unused") public int getTargetYear() { return targetYear; }

    @SuppressWarnings("unused") public void setTargetYear(int targetYear) {
        this.targetYear = targetYear;
    }

    private SingleSpeciesBiomassNormalizedFactory makeBiomassInitializerFactory(
        String speciesCode,
        double logisticGrowthRate,
        Quantity<Mass> totalCarryingCapacity,
        Quantity<Mass> totalBiomass
    ) {
        final SingleSpeciesBiomassNormalizedFactory factory = new SingleSpeciesBiomassNormalizedFactory();
        factory.setSpeciesName(speciesNames.get(speciesCode));
        factory.setGrower(new FadAwareCommonLogisticGrowerInitializerFactory(logisticGrowthRate));
        factory.setCarryingCapacity(new FixedDoubleParameter(asDouble(totalCarryingCapacity, KILOGRAM)));
        factory.setBiomassSuppliedPerCell(false);

        final double biomassRatio = totalBiomass.divide(totalCarryingCapacity).getValue().doubleValue();
        factory.setInitialBiomassAllocator(new ConstantAllocatorFactory(biomassRatio));

        final FileBiomassAllocatorFactory initialCapacityAllocator = new SmootherFileAllocatorFactory();
        initialCapacityAllocator.setBiomassPath(biomassFiles.get(speciesCode));
        initialCapacityAllocator.setInputFileHasHeader(true);
        final PolygonAllocatorFactory polygonAllocatorFactory = new PolygonAllocatorFactory();
        polygonAllocatorFactory.setShapeFile(iattcShapeFile);
        polygonAllocatorFactory.setDelegate(initialCapacityAllocator);
        factory.setInitialCapacityAllocator(polygonAllocatorFactory);

        return factory;
    }

    @SuppressWarnings("unused")
    public List<AlgorithmFactory<? extends AdditionalStartable>> getPlugins() { return plugins; }

    @SuppressWarnings("unused")
    public void setPlugins(List<AlgorithmFactory<? extends AdditionalStartable>> plugins) { this.plugins = plugins; }

    @SuppressWarnings("unused") public Path getCostsFile() {
        return costsFile;
    }

    public void setCostsFile(Path costsFile) {
        this.costsFile = costsFile;
    }

    @SuppressWarnings("unused") public boolean isFadMortalityIncludedInExogenousCatches() {
        return fadMortalityIncludedInExogenousCatches;
    }

    @SuppressWarnings("unused")
    public void setFadMortalityIncludedInExogenousCatches(boolean fadMortalityIncludedInExogenousCatches) {
        this.fadMortalityIncludedInExogenousCatches = fadMortalityIncludedInExogenousCatches;
    }

}
