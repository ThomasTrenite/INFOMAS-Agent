package caduceusdc16.boacomponents;

import agents.anac.y2016.caduceus.agents.Caduceus.UtilFunctions;
import negotiator.boaframework.offeringstrategy.anac2011.AgentK2_Offering;
import negotiator.boaframework.offeringstrategy.anac2012.BRAMAgent2_Offering;
import negotiator.boaframework.offeringstrategy.anac2012.TheNegotiatorReloaded_Offering;
import negotiator.boaframework.offeringstrategy.anac2012.OMACagent_Offering;
import negotiator.boaframework.offeringstrategy.anac2010.Yushu_Offering;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.utility.AbstractUtilitySpace;
import java.util.*;
import java.util.Map.Entry;

public class Group5_BS extends OfferingStrategy {
    private boolean debug = false;
    private double percentageOfOfferingBestBid = 0.85;
    private AbstractUtilitySpace uspace = null;
    public OfferingStrategy[] agents = new OfferingStrategy[5];
    public double[] weights = new double[]{0.205, 0.2, 0.198, 0.204, 0.193};

    public Group5_BS() {
    }

    public double getScore(int agentNumber) {
        return this.weights[agentNumber];
    }

    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel model,
                     OMStrategy oms, Map<String,Double> parameters) throws Exception {
        super.init(negotiationSession, parameters);
        this.agents[0] = new AgentK2_Offering();
        this.agents[1] = new BRAMAgent2_Offering();
        this.agents[2] = new TheNegotiatorReloaded_Offering();
        this.agents[3] = new Yushu_Offering();
        this.agents[4] = new OMACagent_Offering();
        for (int i=0; i< agents.length; i++) {
            agents[i].init(negotiationSession, model, oms, parameters);
        }
        this.uspace = this.negotiationSession.getUtilitySpace();
        this.opponentModel = model;
    }

    @Override
    public BidDetails determineOpeningBid() {
        return this.determineNextBid();
    }

    @Override
    public BidDetails determineNextBid() {

        if (isBestOfferTime()) {
            Bid bestBid = getBestBid();
            return  new BidDetails(bestBid, this.negotiationSession.getUtilitySpace().getUtility(bestBid), this.negotiationSession.getTime());

        }

        ArrayList agentBids = new ArrayList();
        ArrayList agentsThatBid = new ArrayList();
        for (int i = 0; i<agents.length; i++){
            BidDetails var1 = agents[i].determineNextBid();
            agentBids.add(var1.getBid());
            agentsThatBid.add(i);
        }

        Bid bid = this.getMostProposedBidWithWeight(agentsThatBid, agentBids, this.opponentModel);
        nextBid = new BidDetails(bid, this.negotiationSession.getUtilitySpace().getUtility(bid), this.negotiationSession.getTime());
        return nextBid;
    }

    private Bid getBestBid() {
        try {
            return this.negotiationSession.getUtilitySpace().getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isBestOfferTime() {
        return this.negotiationSession.getTimeline().getCurrentTime() < this.negotiationSession.getTimeline().getTotalTime() * this.percentageOfOfferingBestBid;
    }


    private void updateWeights(ArrayList<Bid> agentBids) {

        double[] weightUpdatesUtility = new double[this.weights.length];
        double[] weightUpdatesOpponentUtility = new double[this.weights.length];

        double totalTime = this.negotiationSession.getTimeline().getTotalTime();
        double time = this.negotiationSession.getTimeline().getCurrentTime() - totalTime*this.percentageOfOfferingBestBid ;
        double beta = 1 / (totalTime - totalTime*this.percentageOfOfferingBestBid );
        double alpha = 1 / ((totalTime - totalTime*this.percentageOfOfferingBestBid) * (3/2)) ;

        if (debug) {
            System.out.println("beta = " + beta);
            System.out.println("time = " + time);
            System.out.println("alpha = " + alpha);
        }

        if (debug) {
            for (double d : this.weights) {
                System.out.println("weights before update = " + d);
            }
        }

        for (int bidnumber = 0; bidnumber < agentBids.size(); bidnumber++) {
            weightUpdatesUtility[bidnumber] = this.uspace.getUtility(agentBids.get(bidnumber));
            weightUpdatesOpponentUtility[bidnumber] = opponentModel.getBidEvaluation(agentBids.get(bidnumber));

            if (debug) System.out.println("weightUpdates = " + weightUpdatesUtility[bidnumber]);
            if (debug) System.out.println("weightUpdatesOpponentUtility = " + weightUpdatesOpponentUtility[bidnumber]);

        }

        weightUpdatesUtility = UtilFunctions.normalize(weightUpdatesUtility);
        weightUpdatesOpponentUtility = UtilFunctions.normalize(weightUpdatesOpponentUtility);

        for (int d = 0; d < this.weights.length; d++) {
            double updateUtil;
            double updateOpponentUtil;
            double updateTotal;
            updateUtil = weightUpdatesUtility[d] * (1 - time * beta);
            updateOpponentUtil = weightUpdatesOpponentUtility[d] * (time * beta);
            updateTotal = (updateUtil + updateOpponentUtil) * (1 - time * alpha);

            if (debug) {
                System.out.println("update_1 = " + updateUtil);
                System.out.println("update_2 = " + updateOpponentUtil);
                System.out.println("update_3 = " + updateTotal);
            }
            this.weights[d] = this.weights[d] +  updateTotal;

        }

        if (debug) {
            for (double d : this.weights) {
                System.out.println("Updated weights before  normalizing = " + d);
            }

            this.weights = UtilFunctions.normalize(this.weights);

            if (debug) {
                for (double d : this.weights) {
                    System.out.println("Updated weights after normalizing = " + d);
                }
            }
        }
    }


    private Bid getMostProposedBidWithWeight(ArrayList<Integer> agentNumbers, ArrayList<Bid> agentBids, OpponentModel opponentModel) {

        try {
            this.updateWeights(agentBids);

            List<Issue> issues = agentBids.get(0).getIssues();
            HashMap bidP = new HashMap();

            label46:
            for(int issue = 0; issue < issues.size(); issue++) {

                HashMap valuesForIssue = new HashMap();

                for(int agent = 0; agent < agentNumbers.size(); agent++) {

                    Issue i = (Issue) issues.get(issue);
                    Value valueOfIssueOfAgent = agentBids.get(agent).getValue(i);
                    Double accumulatedWeightOfValue = (Double)valuesForIssue.get(valueOfIssueOfAgent); //accumulatedWeightOfValueForIssue

                    if (accumulatedWeightOfValue == null) {
                        accumulatedWeightOfValue = 1.0;
                    }
                    accumulatedWeightOfValue = accumulatedWeightOfValue + this.weights[agentNumbers.get(agent)];
                    valuesForIssue.put(valueOfIssueOfAgent, accumulatedWeightOfValue);

                }

                Entry currentBestValue = null;
                Iterator valueIterator = valuesForIssue.entrySet().iterator();

// we need to change this method to instead of taking the max it creates a distribution and samples from the distribution

                while(true) {
                    Entry currentValue;
                    do {
                        if (!valueIterator.hasNext()) {
                            bidP.put(issue+1, currentBestValue.getKey());
                            continue label46;
                        }

                        currentValue = (Entry)valueIterator.next();
                    } while(currentBestValue != null && !((Double)currentValue.getValue() > (Double)currentBestValue.getValue()));

                    currentBestValue = currentValue;
                }
            }

            Bid bid = new Bid(this.negotiationSession.getDomain(), bidP);
            return bid;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getDescription() {
        return "Transformer";
    }

    @Override
    public String getName() {
        return "Group5_BS";
    }
}