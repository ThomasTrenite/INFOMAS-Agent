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

    /**
     * This is the Offering strategy for the transformer agent. It was inspired and based upon the CaduceusDC16 agent.
     * We adapted their offering strategy by
     * 1. Using different initial weights and adding dynamic weight updates.
     * 2. Using different expert agents (only agents that could be decoupled).
     *    The offering strategies of these expert agents were combined with our own opponent model, opponent model strategy
     *    and acceptance strategy.
     *
     * The original agent can be found in agents.anac.y2017.caduceusdc16.CaduceusDC16.
     * The original authors: Taha D G ̈unes ̧, Emir Arditi, and Reyhan Aydo ̆gan.
     * The title of the paper of their agent: Collective voice of experts in multilateral negotiation.
     * Can be found in: In International Conference on Principles and Practice of Multi-Agent Systems, pages 450–458. Springer,
     * Version: 2017
     */
public class Group5_BS extends OfferingStrategy {
    private boolean debug = false;
    private double percentageOfOfferingBestBid = 0.88;
    private AbstractUtilitySpace uspace = null;
    public OfferingStrategy[] agents = new OfferingStrategy[5];
    public double[] weights = new double[]{0.205, 0.2, 0.198, 0.204, 0.193};

    public Group5_BS() {
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

        /**
         * Determines the next bid. If sufficient time has passed (time > totaltime * percentageOfOfferingBestBid) the
         * expert agents are asked to each propose a bid, after which a bid is created by means of majority voting.
         *
         * if (time < totaltime * percentageOfOfferingBestBid) then best bid for our agent is returned.
         *
         * @return the bid to be offered
         */
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

        Bid bid = this.getMostProposedBidWithWeight(agentsThatBid, agentBids);
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

        /**
         * Has enough time passed for the agent to start offering a bid with less utility than the best bid.
         * A high value for percentageOfOfferingBestBid means the agent will only start conceding at the end of the negotiation.
         *
         * @return true or false
         */
    private boolean isBestOfferTime() {
        return this.negotiationSession.getTimeline().getCurrentTime() < this.negotiationSession.getTimeline().getTotalTime() * this.percentageOfOfferingBestBid;
    }

        /**
         * @param agentBids the bids proposed by the expert agents.
         *
         * This method updates the weights that the expert agents use to vote.
         * The updates are based on the utility of the bid that expert agent offers,
         * and the utility that it offers the opponent.
         *
         * The updates are time dependent. At the beginning of the negotiation, the agents utility has a larger
         * impact on the weight, and opponents utility a smaller impact.
         * As time continues, the impact of the opponents utility on the weight increases and
         * the utility of the agent decreases.
         *
         * As the end of the negotiation approaches, the weight updates become smaller.
         * The idea behind this is that the weights should have reached an equilibrium and
         * the dominant expert agent should have been established at this point.
         *
         * beta controls the impact of the agents utility to the update. (decreasing with time)
         * alpha controls the impact of the opponents utility to the update (increasing with time)
         * sigma controls the impact of the total update (decreasing with time)
         *
         */
    private void updateWeights(ArrayList<Bid> agentBids) {

        double[] weightUpdatesUtility = new double[this.weights.length];
        double[] weightUpdatesOpponentUtility = new double[this.weights.length];

        double totalTime = this.negotiationSession.getTimeline().getTotalTime();
        double time = this.negotiationSession.getTimeline().getCurrentTime() - totalTime*this.percentageOfOfferingBestBid ;
        double beta = 1 - time * 1 / ((totalTime - totalTime*this.percentageOfOfferingBestBid ) * 2);
        double sigma = time * 1 / (totalTime - totalTime*this.percentageOfOfferingBestBid );
        double alpha = 1 - time * 1 / ((totalTime - totalTime*this.percentageOfOfferingBestBid) * (3/2)) ;


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
            double updateUtil = weightUpdatesUtility[d] * beta;
            double updateOpponentUtil = weightUpdatesOpponentUtility[d] * sigma;
            double updateTotal = (updateUtil + updateOpponentUtil) * alpha;

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

        /**
         *
         * Here the resulting bid is determined by means of majority voting per value per issue.
         *
         * First the weights of the agents are updated based on the bids they have proposed.
         * Each agent votes using their weight on the values that are in their proposed bid.
         *
         * The values with the highest vote for each vote are selected and placed in a bid.
         *
         * @param agentBids the proposed bids of the expert agents
         * @return the final bid to be offered to the opponent.
         */
    private Bid getMostProposedBidWithWeight(ArrayList<Integer> agentNumbers, ArrayList<Bid> agentBids) {

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