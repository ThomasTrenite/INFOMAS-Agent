
package caduceusdc16;

import agents.anac.y2016.caduceus.agents.Caduceus.UtilFunctions;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.parties.NegotiationParty;
import genius.core.utility.AbstractUtilitySpace;
import  caduceusdc16.boacomponents.Group5_OM;

import java.util.*;
import java.util.Map.Entry;

public class CadacDC1 extends AbstractNegotiationParty {
	public double discountFactor = 0.0D;
	private double selfReservationValue = 0.75D;
	private double percentageOfOfferingBestBid = 0.1D;
	private double sigma = 0.95;
	private boolean debug = false;
	private Bid lastReceivedBid = null;
	private AbstractUtilitySpace uspace = null;
	public NegotiationParty[] agents = new NegotiationParty[5];
	/*
	* weights: the weights of each agent. They are initialized according to the experiments we conducted
	* */
	public double[] weights = new double[]{0.205, 0.2, 0.198, 0.204, 0.193};

	public CadacDC1() {
	}

	public double getWeight(int agentNumber) {
		return this.weights[agentNumber];
	}

	public void init(NegotiationInfo negotiationInfo) {
		super.init(negotiationInfo);
		//Random random = new Random(negotiationInfo.getRandomSeed());
		this.agents[0] = new AgentK();
		this.agents[1] = new NegotiatiorReloaded();
		this.agents[2] = new OMACagent();
		this.agents[3] = new Yushu();
		this.agents[4] = new BRAM();

		this.uspace = this.getUtilitySpace();
		this.discountFactor = this.getUtilitySpace().getDiscountFactor();
		this.percentageOfOfferingBestBid *= this.discountFactor;


		for(int i = 0; i < this.agents.length; ++i) {
			NegotiationParty agent = this.agents[i];
			agent.init(negotiationInfo);
		}

	}

	//TODO
	public Action chooseActionSameAcceptance(List<Class<? extends Action>> var1) {
		Action action = this.agents[0].chooseAction(var1);
		if (action instanceof Accept) { //&& this.uspace.getUtility(this.lastReceivedBid) >= this.selfReservationValue) {
			return new Accept(this.getPartyId(), this.lastReceivedBid);

		} else if (action instanceof Offer) {

			ArrayList agentBids = new ArrayList();
			for (NegotiationParty agent: this.agents) {
				agent.chooseAction(var1);
				agentBids.add(((Offer)action).getBid());
			}

			Bid bid = this.getMostProposedBidWithWeight(null, agentBids);
			Offer offer = new Offer(this.getPartyId(), bid);
		}
		return null;
	}

	public Action chooseAction(List<Class<? extends Action>> var1) {

		//First check whether enough time has elapsed before we concede.
		if (this.isBestOfferTime()) {
			Bid bestBid = this.getBestBid();
			if (bestBid != null) {
				return new Offer(this.getPartyId(), bestBid);
			}
			System.err.println("Best Bid is null?");
		}

		//chooseActionSameAcceptance()

		ArrayList agentActions = new ArrayList();
		NegotiationParty[] agentArray = this.agents;

		for(int i = 0; i < agentArray.length; ++i) {
			NegotiationParty agent = agentArray[i];
			Action action = agent.chooseAction(var1);
			agentActions.add(action);
		}

		// here the agents vote whether to accept the bid or propose a new one.

		double acceptCounter = 0.0D;
		double makeNewOfferCounter = 0.0D;
		ArrayList agentBids = new ArrayList();
		ArrayList agentsThatBid = new ArrayList();
		int agentNumber = 0;


		for(Iterator agentActionIter = agentActions.iterator(); agentActionIter.hasNext(); ++agentNumber) {
			Action action = (Action)agentActionIter.next();
			if (action instanceof Accept) {
				acceptCounter += this.getWeight(agentNumber);
			} else if (action instanceof Offer) {
				makeNewOfferCounter += this.getWeight(agentNumber);
				agentBids.add(((Offer)action).getBid());
				agentsThatBid.add(agentNumber);
			}
		}

		if (acceptCounter > makeNewOfferCounter && this.uspace.getUtility(this.lastReceivedBid) >= this.selfReservationValue) {
			return new Accept(this.getPartyId(), this.lastReceivedBid);
		} else if (makeNewOfferCounter > acceptCounter) {

			Bid bid = this.getMostProposedBidWithWeight(agentsThatBid, agentBids);
			Offer offer = new Offer(this.getPartyId(), bid);
			return offer;
		} else {
			return new Offer(this.getPartyId(), this.getBestBid());
		}
	}

	public void receiveMessage(AgentID var1, Action var2) {
		super.receiveMessage(var1, var2);
		if (var2 instanceof Offer) {
			this.lastReceivedBid = ((Offer)var2).getBid();
		}

		NegotiationParty[] var3 = this.agents;
		int var4 = var3.length;

		for(int var5 = 0; var5 < var4; ++var5) {
			NegotiationParty var6 = var3[var5];
			var6.receiveMessage(var1, var2);
		}
	}

	public String getDescription() {
		return "Transformer";
	}

	private Bid getBestBid() {
		try {
			return this.utilitySpace.getMaxUtilityBid();
		} catch (Exception var2) {
			var2.printStackTrace();
			return null;
		}
	}

	private boolean isBestOfferTime() {
		return this.getTimeLine().getCurrentTime() < this.getTimeLine().getTotalTime() * this.percentageOfOfferingBestBid;
	}


	private void updateWeights(ArrayList<Bid> agentBids) {

		if (debug) {
			for (double d : this.weights) {
				System.out.println("weights before update = " + d);
			}
		}

		double[] weightUpdates = new double[this.weights.length];
		double time = this.getTimeLine().getCurrentTime();

		if (debug) System.out.println("time = " + time);

		for (int bidnumber = 0; bidnumber < agentBids.size(); bidnumber++) {
			//opponentUtility = Group5_OM.getOpponentUtility(agentBids.get(bidnumber));
			weightUpdates[bidnumber] = this.uspace.getUtility(agentBids.get(bidnumber));

			if (debug) System.out.println("weightUpdates = " + weightUpdates[bidnumber]);
		}

		for (int d = 0; d < this.weights.length; d++) {
			this.weights[d] = this.weights[d] * (1 - Math.pow(this.sigma, time)) + weightUpdates[d] * Math.pow(this.sigma, time);
		}

		this.weights = UtilFunctions.normalize(this.weights);

		if (debug) {
			for (double d: this.weights) {
				System.out.println("Updated weights after normalizing = " + d);
			}
		}

	}

	private Bid getMostProposedBidWithWeight(ArrayList<Integer> agentNumbers, ArrayList<Bid> agentBids) {

		try {
			this.updateWeights(agentBids);

			List<Issue> issues = agentBids.get(0).getIssues();
			//System.out.println("issues = " + issues);
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

			Bid bid = new Bid(this.utilitySpace.getDomain(), bidP);
			return bid;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
