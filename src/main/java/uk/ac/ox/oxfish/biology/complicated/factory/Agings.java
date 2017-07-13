package uk.ac.ox.oxfish.biology.complicated.factory;

import uk.ac.ox.oxfish.biology.complicated.AgingProcess;
import uk.ac.ox.oxfish.utility.AlgorithmFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by carrknight on 7/8/17.
 */
public class Agings {


    /**
     * the list of all registered CONSTRUCTORS
     */
    public static final Map<String,Supplier<AlgorithmFactory<? extends AgingProcess>>> CONSTRUCTORS =
            new LinkedHashMap<>();
    /**
     * a link to go from class back to the name of the constructor
     */
    public static final Map<Class<? extends AlgorithmFactory>,String> NAMES =
            new LinkedHashMap<>();


    static{
        CONSTRUCTORS.put("Yearly Aging",
                         StandardAgingFactory::new);
        NAMES.put(StandardAgingFactory.class,"Yearly Aging");

        CONSTRUCTORS.put("Proportional Aging",
                         ProportionalAgingFactory::new);
        NAMES.put(ProportionalAgingFactory.class,"Proportional Aging");

    }

}