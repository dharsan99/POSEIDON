package uk.ac.ox.oxfish.model.market.itq;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.engine.Steppable;
import uk.ac.ox.oxfish.fisher.Fisher;
import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.Startable;
import uk.ac.ox.oxfish.model.StepOrder;
import uk.ac.ox.oxfish.model.data.collectors.Counter;
import uk.ac.ox.oxfish.model.data.collectors.IntervalPolicy;
import uk.ac.ox.oxfish.model.regs.MonoQuotaRegulation;
import uk.ac.ox.oxfish.model.regs.QuotaPerSpecieRegulation;
import uk.ac.ox.oxfish.utility.FishStateUtilities;

import java.util.*;

/**
 * An order book to trade ITQs. Very experimental. For now allows only one trade per person per step
 * Created by carrknight on 8/20/15.
 */
public class ITQOrderBook implements Steppable,Startable{


    public static final String MATCHES_COLUMN_NAME = "MATCHES";
    public static final String QUOTA_COLUMN_NAME = "QUOTA_VOLUME";
    public static final String MONEY_COLUMN_NAME = "MONEY_VOLUME";
    HashMap<Fisher,MonoQuotaPriceGenerator> pricers  = new HashMap<>();

    private Queue<Quote> asks;

    private Queue<Quote> bids;

    private double markup = 0.05;


    private double lastClosingPrice = Double.NaN;

    private Counter counter = new Counter(IntervalPolicy.EVERY_DAY);


    private int unitsTradedPerMatch = 100;


    private final int specieIndex;


    /**
     * this gets called by the fish-state right after the scenario has started. It's useful to set up steppables
     * or just to percolate a reference to the model
     *
     * @param model the model
     */
    @Override
    public void start(FishState model) {
        counter.addColumn(MATCHES_COLUMN_NAME);
        counter.addColumn(QUOTA_COLUMN_NAME);
        counter.addColumn(MONEY_COLUMN_NAME);
        model.scheduleEveryDay(this, StepOrder.POLICY_UPDATE);
        counter.start(model);
    }

    /**
     * tell the startable to turnoff,
     */
    @Override
    public void turnOff() {

    }

    public ITQOrderBook(int specieIndex)
    {

        //create the queues holding on to the quotes
        asks = new PriorityQueue<>(100, Quote::compareTo);
        bids = new PriorityQueue<>(100, (o1, o2) -> -o1.compareTo(o2));
        this.specieIndex = specieIndex;

    }


    public void registerTrader(Fisher fisher, MonoQuotaPriceGenerator pricer)
    {
        pricers.put(fisher,pricer);
    }


    public void step(SimState state)
    {
        MersenneTwisterFast random = ((FishState) state).getRandom();
        List<Map.Entry<Fisher,MonoQuotaPriceGenerator>> traders = new ArrayList<>(pricers.entrySet());
        Collections.shuffle(traders,new Random(random.nextLong()));

        //fill the quotes
        for(Map.Entry<Fisher,MonoQuotaPriceGenerator> trader : traders)
        {
            double price = trader.getValue().computeLambda();
            if(Double.isFinite(price)) {
                double buyPrice = FishStateUtilities.round(price * (1 - markup));
                //do I want to buy?
                if (price > 0) {
                    bids.add(new Quote(
                            buyPrice,
                            trader.getKey()));
                }
                //can I sell?
                if (((MonoQuotaRegulation) trader.getKey().getRegulation()).getQuotaRemaining(specieIndex) >= unitsTradedPerMatch ) {
                    double salePrice = Math.max(FishStateUtilities.round(Math.max(price * (1 + markup), .5)),
                                                buyPrice + FishStateUtilities.EPSILON) //never let bids and ask cross, even if markup is 0!
                            ;
                    assert buyPrice < salePrice;
                    asks.add(new Quote(
                            salePrice,
                            trader.getKey()));
                }
            }
        }

        //go for it
        clearQuotes(random);


        //clear the quotes
        asks.clear();
        bids.clear();
    }

    private void clearQuotes(MersenneTwisterFast random)
    {
        if(bids.isEmpty() || asks.isEmpty())
            return;

        Quote bestBid = bids.remove();
        Quote bestAsk = asks.remove();
        //does somebody want to trade?
        if (bestAsk.getPrice() <= bestBid.getPrice()) {

            double tradingPrice = bestAsk.getPrice()    ;
            assert tradingPrice >= bestAsk.getPrice();
            assert tradingPrice <=bestBid.getPrice();

            //now trade!
            QuotaPerSpecieRegulation buyerQuota = (QuotaPerSpecieRegulation) bestBid.getTrader().getRegulation();
            QuotaPerSpecieRegulation sellerQuota = (QuotaPerSpecieRegulation) bestAsk.getTrader().getRegulation();

            buyerQuota.setQuotaRemaining(specieIndex, buyerQuota.getQuotaRemaining(specieIndex)+unitsTradedPerMatch);
            sellerQuota.setQuotaRemaining(specieIndex, sellerQuota.getQuotaRemaining(specieIndex)-unitsTradedPerMatch);
            bestBid.getTrader().spendExogenously(unitsTradedPerMatch * tradingPrice);
            bestAsk.getTrader().earn(unitsTradedPerMatch * tradingPrice);
            counter.count(QUOTA_COLUMN_NAME, unitsTradedPerMatch);
            counter.count(MONEY_COLUMN_NAME, unitsTradedPerMatch * tradingPrice);
            counter.count(MATCHES_COLUMN_NAME,1);



            lastClosingPrice = tradingPrice;
            assert sellerQuota.getQuotaRemaining(specieIndex)>=0;

            //again!
            clearQuotes(random);

        }
    }


    public double getMarkup() {
        return markup;
    }

    public void setMarkup(double markup) {
        this.markup = markup;
    }

    public int getUnitsTradedPerMatch() {
        return unitsTradedPerMatch;
    }

    public void setUnitsTradedPerMatch(int unitsTradedPerMatch) {
        this.unitsTradedPerMatch = unitsTradedPerMatch;
    }


    /**
     * How many buyers and sellers fruitfully since the beginning of the day
     */
    public double getDailyMatches()
    {
        return counter.getColumn(MATCHES_COLUMN_NAME);
    }

    /**
     * How many buyers and sellers fruitfully since the beginning of the day
     */
    public double getDailyQuotasExchanged()
    {
        return counter.getColumn(QUOTA_COLUMN_NAME);
    }

    /**
     * How many buyers and sellers traded since the beginning of the day
     */
    public double getDailyAveragePrice()
    {
        double quotas = getDailyQuotasExchanged();
        if(quotas ==0)
            return Double.NaN;
        return counter.getColumn(MONEY_COLUMN_NAME)/ quotas;
    }

    public double getLastClosingPrice() {
        return lastClosingPrice;
    }
}