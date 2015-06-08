package uk.ac.ox.oxfish.model.scenario;

import ec.util.MersenneTwisterFast;
import uk.ac.ox.oxfish.biology.GlobalBiology;
import uk.ac.ox.oxfish.biology.LocalBiology;
import uk.ac.ox.oxfish.biology.Specie;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.fisher.Port;
import uk.ac.ox.oxfish.fisher.equipment.Boat;
import uk.ac.ox.oxfish.fisher.equipment.FixedProportionGear;
import uk.ac.ox.oxfish.fisher.equipment.Hold;
import uk.ac.ox.oxfish.fisher.strategies.departing.DepartingStrategy;
import uk.ac.ox.oxfish.fisher.strategies.departing.FixedProbabilityDepartingFactory;
import uk.ac.ox.oxfish.fisher.strategies.departing.FixedProbabilityDepartingStrategy;
import uk.ac.ox.oxfish.fisher.strategies.destination.FavoriteDestinationStrategy;
import uk.ac.ox.oxfish.fisher.strategies.fishing.FishUntilFullStrategy;
import uk.ac.ox.oxfish.fisher.strategies.fishing.FishingStrategy;
import uk.ac.ox.oxfish.fisher.strategies.fishing.factory.FishUntilFullFactory;
import uk.ac.ox.oxfish.geography.CartesianDistance;
import uk.ac.ox.oxfish.geography.NauticalMap;
import uk.ac.ox.oxfish.geography.NauticalMapFactory;
import uk.ac.ox.oxfish.geography.SeaTile;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.market.FixedPriceMarket;
import uk.ac.ox.oxfish.model.market.Markets;
import uk.ac.ox.oxfish.model.regs.FishingSeason;
import uk.ac.ox.oxfish.model.regs.Regulations;
import uk.ac.ox.oxfish.utility.StrategyFactory;
import uk.ac.ox.oxfish.utility.parameters.DoubleParameter;
import uk.ac.ox.oxfish.utility.parameters.FixedDoubleParameter;

import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is the scenario that recreates the NETLOGO prototype model. This means a fake generated sea and coast
 * Created by carrknight on 4/20/15.
 */
public class PrototypeScenario implements Scenario {

    /**
     * number of species
     */
    private int numberOfSpecies = 1;

    /**
     * market prices for each species
     */
    private double[] marketPrices = new double[]{10.0};
    /**
     * higher the more the coast gets jagged
     */
    private int coastalRoughness = 4;
    /**
     * how many rounds of depth smoothing to do
     */
    private int depthSmoothing = 1000000;

    /**
     * how many rounds of biology smoothing to do
     */
    private int biologySmoothing = 1000000;
    /**
     * random minimum biomass pre-smoothing
     */
    private int minBiomass = 10;
    /**
     * random maximum biomass pre-smoothing
     */
    private int maxBiomass = 5000;
    /**
     * number of ports
     */
    private int ports = 1;
    /**
     * map width
     */
    private int width = 50;


    private final Function<SeaTile, LocalBiology> biologyInitializer =
            NauticalMapFactory.fromLeftToRightBiology(
            maxBiomass, width);
    /**
     * map height
     */
    private int height =50;

    /**
     * the number of fishers
     */
    private int fishers = 50;

    /**
     * Uses Caartesian distance
     */
    private double gridSizeInKm = 10;

    /**
     * boat speed
     */
    private DoubleParameter speedInKmh = new FixedDoubleParameter(5);

    /**
     * hold size
     */
    private DoubleParameter holdSize = new FixedDoubleParameter(100);

    /**
     * efficiency
     */
    private DoubleParameter fishingEfficiency = new FixedDoubleParameter(.01);


    /**
     * factory to produce departing strategy
     */
    private StrategyFactory<? extends DepartingStrategy> departingStrategy = new FixedProbabilityDepartingFactory();

    /**
     * factory to produce fishing strategy
     */
    private StrategyFactory<? extends FishingStrategy> fishingStrategy = new FishUntilFullFactory();


    private Regulations regulation =  new FishingSeason(true,300);

    private Function<MersenneTwisterFast, Consumer<NauticalMap>> biologySmootherMaker =
            NauticalMapFactory.smoothConstantBiology(
                    biologySmoothing,
                    width, height);;


    public PrototypeScenario() {
    }



    /**
     * this is the very first method called by the model when it is started. The scenario needs to instantiate all the
     * essential objects for the model to take place
     *
     * @param model the model
     * @return a scenario-result object containing the map, the list of agents and the biology object
     */
    @Override
    public ScenarioResult start(FishState model) {

        MersenneTwisterFast random = model.random;

        GlobalBiology biology = GlobalBiology.genericListOfSpecies(numberOfSpecies);

        NauticalMap map = NauticalMapFactory.prototypeMapWithRandomSmoothedBiology(coastalRoughness,
                                                                                   random,
                                                                                   depthSmoothing,
                                                                                   biologyInitializer,
                                                                                   biologySmootherMaker.apply(random));
        map.setDistance(new CartesianDistance(gridSizeInKm));


        //general biology
        //create fixed price market
        Markets markets = new Markets(biology);
        for(Specie specie : biology.getSpecies())
            markets.addMarket(specie,new FixedPriceMarket(specie,marketPrices[specie.getIndex()]));

        //create random ports, all sharing the same market
        NauticalMapFactory.addRandomPortsToMap(map, ports, seaTile -> markets, random);



        LinkedList<Fisher> fisherList = new LinkedList<>();
        Port[] ports =map.getPorts().toArray(new Port[map.getPorts().size()]);
        for(int i=0;i<fishers;i++)
        {
            Port port = ports[random.nextInt(ports.length)];
            DepartingStrategy departing = departingStrategy.apply(model);
            double speed = speedInKmh.apply(random);
            double capacity = holdSize.apply(random);
            double efficiency =fishingEfficiency.apply(random);
            fisherList.add(new Fisher(port, random, regulation, departing,
                                      new FavoriteDestinationStrategy(map,random),
                                      fishingStrategy.apply(model),
                                      new Boat(speed),
                                      new Hold(capacity, biology.getSize()),
                                      new FixedProportionGear(efficiency)
            ));
        }



        return new ScenarioResult(biology,map,fisherList,markets);
    }


    public int getCoastalRoughness() {
        return coastalRoughness;
    }

    public void setCoastalRoughness(int coastalRoughness) {
        this.coastalRoughness = coastalRoughness;
    }

    public int getDepthSmoothing() {
        return depthSmoothing;
    }

    public void setDepthSmoothing(int depthSmoothing) {
        this.depthSmoothing = depthSmoothing;
    }

    public int getBiologySmoothing() {
        return biologySmoothing;
    }

    public void setBiologySmoothing(int biologySmoothing) {
        this.biologySmoothing = biologySmoothing;
    }

    public int getMinBiomass() {
        return minBiomass;
    }

    public void setMinBiomass(int minBiomass) {
        this.minBiomass = minBiomass;
    }

    public int getMaxBiomass() {
        return maxBiomass;
    }

    public void setMaxBiomass(int maxBiomass) {
        this.maxBiomass = maxBiomass;
    }

    public int getPorts() {
        return ports;
    }

    public void setPorts(int ports) {
        this.ports = ports;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFishers() {
        return fishers;
    }

    public void setFishers(int fishers) {
        this.fishers = fishers;
    }

    public double getGridSizeInKm() {
        return gridSizeInKm;
    }

    public void setGridSizeInKm(double gridSizeInKm) {
        this.gridSizeInKm = gridSizeInKm;
    }

    public DoubleParameter getSpeedInKmh() {
        return speedInKmh;
    }

    public void setSpeedInKmh(DoubleParameter speedInKmh) {
        this.speedInKmh = speedInKmh;
    }

    public DoubleParameter getFishingEfficiency() {
        return fishingEfficiency;
    }

    public void setFishingEfficiency(DoubleParameter fishingEfficiency) {
        this.fishingEfficiency = fishingEfficiency;
    }

    public Regulations getRegulation() {
        return regulation;
    }

    public void setRegulation(Regulations regulation) {
        this.regulation = regulation;
    }


    public Function<SeaTile, LocalBiology> getBiologyInitializer() {
        return biologyInitializer;
    }

    public Function<MersenneTwisterFast, Consumer<NauticalMap>> getBiologySmootherMaker() {
        return biologySmootherMaker;
    }

    public void setBiologySmootherMaker(
            Function<MersenneTwisterFast, Consumer<NauticalMap>> biologySmootherMaker) {
        this.biologySmootherMaker = biologySmootherMaker;
    }

    public int getNumberOfSpecies() {
        return numberOfSpecies;
    }

    public void setNumberOfSpecies(int numberOfSpecies) {
        this.numberOfSpecies = numberOfSpecies;
    }


    public StrategyFactory<? extends DepartingStrategy> getDepartingStrategy() {
        return departingStrategy;
    }

    public void setDepartingStrategy(
            StrategyFactory<? extends DepartingStrategy> departingStrategy) {
        this.departingStrategy = departingStrategy;
    }

    public StrategyFactory<? extends FishingStrategy> getFishingStrategy() {
        return fishingStrategy;
    }

    public void setFishingStrategy(
            StrategyFactory<? extends FishingStrategy> fishingStrategy) {
        this.fishingStrategy = fishingStrategy;
    }


    public DoubleParameter getHoldSize() {
        return holdSize;
    }

    public void setHoldSize(DoubleParameter holdSize) {
        this.holdSize = holdSize;
    }
}