package uk.ac.ox.oxfish.model.regs;

/**
 * A simple set of additional methods for a regulation object that involves per specie quotas
 * Created by carrknight on 8/26/15.
 */
public interface QuotaPerSpecieRegulation extends Regulation
{


    public double getQuotaRemaining(int specieIndex);

    public void setQuotaRemaining(int specieIndex, double newQuotaValue);

}