package uk.ac.ox.oxfish.model.plugins;

import uk.ac.ox.oxfish.biology.BiomassResetterFactory;
import uk.ac.ox.oxfish.biology.BiomassTotalResetterFactory;
import uk.ac.ox.oxfish.biology.boxcars.AbundanceGathererBuilder;
import uk.ac.ox.oxfish.biology.boxcars.FishingMortalityAgentFactory;
import uk.ac.ox.oxfish.biology.boxcars.SPRAgentBuilder;
import uk.ac.ox.oxfish.biology.boxcars.SprOracleBuilder;
import uk.ac.ox.oxfish.biology.complicated.factory.SnapshotAbundanceResetterFactory;
import uk.ac.ox.oxfish.biology.complicated.factory.SnapshotBiomassResetterFactory;
import uk.ac.ox.oxfish.model.AdditionalStartable;
import uk.ac.ox.oxfish.model.data.collectors.TowLongLoggerFactory;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class AdditionalStartables {

    private AdditionalStartables(){}

    public static final LinkedHashMap<String,Supplier<AlgorithmFactory<? extends AdditionalStartable>>> CONSTRUCTORS =
            new LinkedHashMap<>();

    public static final LinkedHashMap<Class<? extends AlgorithmFactory>,String> NAMES = new LinkedHashMap<>();


    static {
        CONSTRUCTORS.put("Tow Heatmapper",
                TowAndAltitudePluginFactory::new
        );
        NAMES.put(TowAndAltitudePluginFactory.class,
                "Tow Heatmapper");


        CONSTRUCTORS.put("Biomass Location Resetter",
                         BiomassResetterFactory::new
        );
        NAMES.put(BiomassResetterFactory.class,
                  "Biomass Location Resetter");


        CONSTRUCTORS.put("Biomass Total Resetter",
                         BiomassTotalResetterFactory::new
        );
        NAMES.put(BiomassTotalResetterFactory.class,
                  "Biomass Total Resetter");


        CONSTRUCTORS.put("Abundance Snapshot Resetter",
                SnapshotAbundanceResetterFactory::new
        );
        NAMES.put(SnapshotAbundanceResetterFactory.class,
                "Abundance Snapshot Resetter");


        CONSTRUCTORS.put("Biomass Snapshot Resetter",
                SnapshotBiomassResetterFactory::new
        );
        NAMES.put(SnapshotBiomassResetterFactory.class,
                "Biomass Snapshot Resetter");


        CONSTRUCTORS.put("Abundance Gatherers",
                AbundanceGathererBuilder::new
        );
        NAMES.put(AbundanceGathererBuilder.class,
                "Abundance Gatherers");

        CONSTRUCTORS.put("SPR Agent",
                SPRAgentBuilder::new
        );
        NAMES.put(SPRAgentBuilder.class,
                "SPR Agent");

        CONSTRUCTORS.put("SPR Oracle",
                         SprOracleBuilder::new
        );
        NAMES.put(SprOracleBuilder.class,
                  "SPR Oracle");


        CONSTRUCTORS.put("Fishing Mortality Agent",
                         FishingMortalityAgentFactory::new
        );
        NAMES.put(FishingMortalityAgentFactory.class,
                  "Fishing Mortality Agent");


        CONSTRUCTORS.put("Fish Entry By Profit",
                         FisherEntryByProfitFactory::new
        );
        NAMES.put(FisherEntryByProfitFactory.class,
                  "Fish Entry By Profit");


        CONSTRUCTORS.put("Fish Entry Constant Rate",
                         FisherEntryConstantRateFactory::new
        );
        NAMES.put(FisherEntryConstantRateFactory.class,
                  "Fish Entry Constant Rate");


        CONSTRUCTORS.put("Full-time Seasonal Retired Data Collectors",
                         FullSeasonalRetiredDataCollectorsFactory::new
        );
        NAMES.put(FullSeasonalRetiredDataCollectorsFactory.class,
                  "Full-time Seasonal Retired Data Collectors");

        CONSTRUCTORS.put("Biomass Depletion Data Collectors",
                         BiomassDepletionGathererFactory::new
        );
        NAMES.put(BiomassDepletionGathererFactory.class,
                  "Biomass Depletion Data Collectors");

        CONSTRUCTORS.put("Tow Long Logger",
                         TowLongLoggerFactory::new
        );
        NAMES.put(TowLongLoggerFactory.class,
                  "Tow Long Logger");



    }

}
