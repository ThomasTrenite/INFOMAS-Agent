import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.bidding.BidDetailsSorterUtility;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
This OMS decides its strategy based on time. Before the time threshold, this strategy choose
a random bid from N best bids, where N is determined by size of outcome space and user can set
a minimum number for N. After the time threshold, this strategy choose the one best bid.
Users can set 3 parameters, minimum number for N, opponent model update threshold time, and
change to one best bid time threshold.
 */
public class Group5_OMS extends OMStrategy {
    private Random rand;
    private int bestN;
    private BidDetailsSorterUtility comp = new BidDetailsSorterUtility();
    double updateThreshold = 1.1D;
    double oneBidThreshold = 0.95D;

    public Group5_OMS() {
    }

    // initialize the model and check the parameters
    public void init(NegotiationSession var1, OpponentModel var2, Map<String, Double> var3) {
        super.init(var1, var2, var3);
        this.negotiationSession = var1;
        this.model = var2;

        // initialize the minimum number to an integer
        if (var3.get("n") != null) {
            int var4 = ((Double)var3.get("n")).intValue();
            this.initializeAgent(var1, var2, var4);
        } else {
            throw new IllegalArgumentException("Constant \"n\" for amount of best bids was not set.");
        }
        if (var3.get("t") != null) {
            this.updateThreshold = (Double)var3.get("t");
        } else {
            System.out.println("OMStrategy assumed t = 1.1");
        }
        if (var3.get("x") !=null) {
            this.oneBidThreshold = (Double)var3.get("x");
        }
    }

    private void initializeAgent(NegotiationSession var1, OpponentModel var2, int var3) {
        try {
            super.init(var1, var2, new HashMap());
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        this.rand = new Random();
        this.bestN = var3;
    }

    // two phase in this function, before oneBidThreshold, use random choose
    // after oneBidThreshold, use the one best bid
    public BidDetails getBid(List<BidDetails> var1) {
        ArrayList var2 = new ArrayList(var1.size());
        Iterator var3 = var1.iterator();

        // get the bids' details
        while(var3.hasNext()) {
            BidDetails var4 = (BidDetails)var3.next();
            Bid var5 = var4.getBid();
            BidDetails var6 = new BidDetails(var5, this.model.getBidEvaluation(var5), this.negotiationSession.getTime());
            var2.add(var6);
        }

        Bid var10 = null;

        // sort the set
        Collections.sort(var2, this.comp);

        // check the one best bid threshold, first phase of strategy
        if (this.negotiationSession.getTime() < this.oneBidThreshold) {
            int var11 = (int) Math.round((double) var2.size()/3.0D);
            if (var11 < this.bestN) {
                var11 = this.bestN;
            }
            if (var11 > 20) {
                var11 = 20;
            }

            int var9 = this.rand.nextInt(Math.min(var2.size(), var11));
            var10 = ((BidDetails) var2.get(var9)).getBid();         // randomly choose one bid
        }else {
            var10 = ((BidDetails) var2.get(0)).getBid();            // choose the best bid, second phase of strategy
        }

        // return the chosen bid
        BidDetails var8 = null;
        try {
            var8 = new BidDetails(var10, this.negotiationSession.getUtilitySpace().getUtility(var10), this.negotiationSession.getTime());
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        return var8;
    }

    // determine when to stop updating opponent model
    public boolean canUpdateOM() {
        return this.negotiationSession.getTime() < this.updateThreshold;
    }

    // get the parameters
    public Set<BOAparameter> getParameterSpec() {
        HashSet var1 = new HashSet();
        var1.add(new BOAparameter("n", 3.0D, "A random bid is selected from the best n bids"));
        var1.add(new BOAparameter("t", 1.1D, "Time after which the OM should not be updated"));
        var1.add(new BOAparameter("x", 0.95D, "Time after which the strategy changes to one best bid"));
        return var1;
    }

    public String getName() {
        return "Group5_OMS";
    }
}
