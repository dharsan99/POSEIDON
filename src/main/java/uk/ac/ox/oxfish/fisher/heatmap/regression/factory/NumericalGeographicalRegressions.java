package uk.ac.ox.oxfish.fisher.heatmap.regression.factory;

import uk.ac.ox.oxfish.fisher.heatmap.regression.numerical.NumericalGeographicalRegression;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by carrknight on 7/5/16.
 */
public class NumericalGeographicalRegressions {


    private NumericalGeographicalRegressions() {
    }

    /**
     * the list of all registered CONSTRUCTORS
     */
    public static final Map<String,Supplier<AlgorithmFactory<? extends NumericalGeographicalRegression>>> CONSTRUCTORS =
            new LinkedHashMap<>();
    /**
     * a link to go from class back to the name of the constructor
     */
    public static final Map<Class<? extends AlgorithmFactory>,String> NAMES =
            new LinkedHashMap<>();
    static{

        CONSTRUCTORS.put("Space Only Kernel",
                         SpaceOnlyKernelRegressionFactory::new);
        NAMES.put(SpaceOnlyKernelRegressionFactory.class,"Space Only Kernel");

        CONSTRUCTORS.put("Space and Time Kernel",
                         TimeAndSpaceKernelRegressionFactory::new);
        NAMES.put(TimeAndSpaceKernelRegressionFactory.class,"Space and Time Kernel");


        CONSTRUCTORS.put("Nearest Neighbor",
                         NearestNeighborRegressionFactory::new);
        NAMES.put(NearestNeighborRegressionFactory.class,"Nearest Neighbor");


        CONSTRUCTORS.put("Nearest Neighbor Transduction",
                         NearestNeighborTransductionFactory::new);
        NAMES.put(NearestNeighborTransductionFactory.class,"Nearest Neighbor Transduction");


        CONSTRUCTORS.put("Kernel Transduction",
                         KernelTransductionFactory::new);
        NAMES.put(KernelTransductionFactory.class,
                  "Kernel Transduction");


        CONSTRUCTORS.put("RBF Kernel Transduction",
                         DefaultRBFKernelTransductionFactory::new);
        NAMES.put(DefaultRBFKernelTransductionFactory.class,
                  "RBF Kernel Transduction");


        CONSTRUCTORS.put("Particle Filter Regression",
                         ParticleFilterRegressionFactory::new);
        NAMES.put(ParticleFilterRegressionFactory.class,
                  "Particle Filter Regression");


        CONSTRUCTORS.put("Simple Kalman",
                         SimpleKalmanRegressionFactory::new);
        NAMES.put(SimpleKalmanRegressionFactory.class,
                  "Simple Kalman");



    }
}
