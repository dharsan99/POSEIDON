package uk.ac.ox.oxfish.fisher.equipment;

import uk.ac.ox.oxfish.biology.GlobalBiology;
import uk.ac.ox.oxfish.biology.Specie;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.geography.SeaTile;

import java.util.HashMap;

/**
 * Fish the fixed proportion
 * Created by carrknight on 4/20/15.
 */
public class FixedProportionGear implements Gear
{

    final public double proportionFished;

    public FixedProportionGear(double proportionFished) {
        this.proportionFished = proportionFished;
    }

    @Override
    public Catch fish(
            Fisher fisher, SeaTile where, GlobalBiology modelBiology) {

        double[] caught = new double[modelBiology.getSize()];
        for (Specie specie : modelBiology.getSpecies())
        {
            double poundsCaught = proportionFished * where.getBiomass(specie);
            if(poundsCaught>0) {
                where.reactToThisAmountOfBiomassBeingFished(specie, poundsCaught);
                caught[specie.getIndex()] = poundsCaught;
            }

        }
        return new Catch(caught);
    }
}