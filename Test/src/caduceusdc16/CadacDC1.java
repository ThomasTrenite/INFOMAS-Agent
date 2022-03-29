//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

//package agents.anac.y2017.caduceusdc16;
package caduceusdc16;

import agents.anac.y2016.atlas3.Atlas32016;
import agents.anac.y2016.caduceus.agents.Atlas3.Atlas3;
import agents.anac.y2016.caduceus.agents.Caduceus.UtilFunctions;
import agents.anac.y2016.caduceus.agents.RandomDance.RandomDance;
import agents.anac.y2016.caduceus.agents.kawaii.kawaii;
import agents.anac.y2016.farma.Farma;
import agents.anac.y2016.grandma.GrandmaAgent;
import agents.anac.y2016.myagent.MyAgent;
import agents.anac.y2016.parscat.ParsCat;
import agents.anac.y2016.yxagent.YXAgent;
import bilateralexamples.BoaPartyExample;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.list.Tuple;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.parties.NegotiationParty;
import genius.core.persistent.StandardInfo;
import genius.core.persistent.StandardInfoList;
import genius.core.utility.AbstractUtilitySpace;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import static java.util.Objects.isNull;

public class CadacDC1 extends AbstractNegotiationParty {
	public double discountFactor = 0.0D;
	private double selfReservationValue = 0.75D;
	private double percentageOfOfferingBestBid = 0.5D;
	private Random random;
	private Bid lastReceivedBid = null;
	private AbstractUtilitySpace uspace = null;
	public NegotiationParty[] agents = new NegotiationParty[5];
	/*
	* scores: the weights of each of the agents
	* */
	public double[] scores = UtilFunctions.normalize(new double[]{5.0D, 4.0D, 3.0D, 2.0D, 1.0D});

	public CadacDC1() {
	}

	public double getScore(int var1) {
		return this.scores[var1];
	}

	public void init(NegotiationInfo var1) {
		super.init(var1);
		this.random = new Random(var1.getRandomSeed());
		this.agents[0] = new YXAgent();
		//this.agents[0] = new BoaPartyExample();
		this.agents[1] = new RandomDance();
		this.agents[2] = new kawaii();
		this.agents[3] = new Atlas3();
		this.agents[4] = new GrandmaAgent();
		this.uspace = this.getUtilitySpace();
		this.discountFactor = this.getUtilitySpace().getDiscountFactor();
		double var2 = this.getUtilitySpace().getReservationValueUndiscounted();
		System.out.println("Discount Factor is " + this.discountFactor);
		System.out.println("Reservation Value is " + var2);
		this.percentageOfOfferingBestBid *= this.discountFactor;
		StandardInfoList var4 = (StandardInfoList)this.getData().get();
		System.out.println("(StandardInfoList1) " + var4.isEmpty());



		if (!var4.isEmpty()) {
			double var5 = 0.0D;
			Iterator var7 = var4.iterator();

			while(var7.hasNext()) {
				StandardInfo var8 = (StandardInfo)var7.next();
				int var9 = var8.getAgentProfiles().size();
				List var10 = var8.getUtilities();
				int var11 = var10.size();
				List var12 = var10.subList(var11 - var9, var11);
				Iterator var13 = var12.iterator();

				while(var13.hasNext()) {
					Tuple var14 = (Tuple)var13.next();
					if (((String)var14.get1()).toLowerCase().contains("CaduceusDC16".toLowerCase())) {
						var5 += (Double)var14.get2();
					}
				}
			}

			double origSelfReservationValue = this.selfReservationValue;
			this.selfReservationValue = var5 / (double)var4.size();

			System.out.println(this.selfReservationValue);
			System.out.println("origSelfReservationValue: "+origSelfReservationValue);
		}

		for(int i = 0; i < this.agents.length; ++i) {
			NegotiationParty agent = this.agents[i];
			agent.init(var1);
		}


//		NegotiationParty[] var15 = this.agents;
//		int var6 = var15.length;
//
//		for(int var16 = 0; var16 < var6; ++var16) {
//			NegotiationParty var17 = var15[var16];
//			var17.init(var1);
//		}

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


		//Get a reaction (Accept or make new offer) from each agent.

		/**
		 * agentActions - List of the reactions (accept or offer) of our expert agents to the opponents bid
		 *
		 */

		ArrayList agentActions = new ArrayList();

		NegotiationParty[] agentArray = this.agents;

		for(int i = 0; i < agentArray.length; ++i) {
			NegotiationParty agent = agentArray[i];
			Action action = agent.chooseAction(var1);
			agentActions.add(action);
		}



		// here the agents vote whether to accept the bid or propose a new one.


		/**
		 *  agentBids - List of bids of expert agents that did not accept opponents bid
		 *  agentsThatBid - Lis of the corresponding number of the agent that placed a bid.
		 */
		double acceptCounter = 0.0D; // acceptCounter
		double makeNewOfferCounter = 0.0D;	// makeNewOfferCounter
		ArrayList agentBids = new ArrayList();
		ArrayList agentsThatBid = new ArrayList();
		int agentNumber = 0;

		for(Iterator agentActionIter = agentActions.iterator(); agentActionIter.hasNext(); ++agentNumber) {
			Action action = (Action)agentActionIter.next();
			if (action instanceof Accept) {
				acceptCounter += this.getScore(agentNumber);
			} else if (action instanceof Offer) {
				makeNewOfferCounter += this.getScore(agentNumber);
				agentBids.add(((Offer)action).getBid());
				agentsThatBid.add(agentNumber);
			}
		}

		if (acceptCounter > makeNewOfferCounter && this.uspace.getUtility(this.lastReceivedBid) >= this.selfReservationValue) {
			return new Accept(this.getPartyId(), this.lastReceivedBid);
		} else if (makeNewOfferCounter > acceptCounter) {
			return new Offer(this.getPartyId(), this.getMostProposedBidWithWeight(agentsThatBid, agentBids));
		} else {
			return new Offer(this.getPartyId(), this.getBestBid());
		}
	}

	private Bid getRandomizedAction(ArrayList<Integer> var1, ArrayList<Bid> var2) {
		double[] var3 = new double[var1.size()];
		int var4 = 0;

		for(Iterator var5 = var1.iterator(); var5.hasNext(); ++var4) {
			Integer var6 = (Integer)var5.next();
			var3[var4] = this.getScore(var6);
		}

		var3 = UtilFunctions.normalize(var3);
		UtilFunctions.print(var3);
		double var14 = this.random.nextDouble();
		double var7 = 0.0D;
		var4 = 0;
		double[] var9 = var3;
		int var10 = var3.length;

		for(int var11 = 0; var11 < var10; ++var11) {
			double var12 = var9[var11];
			var7 += var12;
			if (var14 < var7) {
				return (Bid)var2.get(var4);
			}

			++var4;
		}

		return null;
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
		return "TransformerCadac";
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


	/**
	 *
	 * @param agentNumbers
	 * @param agentBids
	 * @return
	 */
	private Bid getMostProposedBidWithWeight(ArrayList<Integer> agentNumbers, ArrayList<Bid> agentBids) {

		try {
			List issues = agentBids.get(0).getIssues();
			System.out.println("issues = " + issues);
			HashMap bidP = new HashMap();

			label46:
//			for(int issue = 1; issue <= issues.size(); ++issue) {
			for(int issue = 0; issue < issues.size(); issue++) {

				HashMap valuesForIssue = new HashMap();

				for(int agent = 0; agent < agentNumbers.size(); agent++) {

					//System.out.println("agentbid = " + agentBids.get(agent));
					//System.out.println("agent = " + this.agents[agent].getDescription());


					System.out.println(" issues = " + issues.get(issue));
					Issue i = (Issue) issues.get(issue);
					Value valueOfIssueOfAgent = agentBids.get(agent).getValue(i);
					System.out.println("valueOfIssueOfAgent = " + valueOfIssueOfAgent);


					//int var9 = agentNumbers.get(agent);

					Double var10 = (Double)valuesForIssue.get(valueOfIssueOfAgent);

					System.out.println("var10 = " + var10);

					valuesForIssue.put(valueOfIssueOfAgent, var10 == null ? 1.0D : var10 + this.scores[agentNumbers.get(agent)]);



				}
				System.out.println();
				System.out.println("valuesForIssue = " + valuesForIssue);


				Entry var12 = null;
				Iterator var13 = valuesForIssue.entrySet().iterator();

				while(true) {
					Entry var14;
					do {
						if (!var13.hasNext()) {
							bidP.put(issue, var12.getKey());
							continue label46;
						}

						var14 = (Entry)var13.next();
					} while(var12 != null && !((Double)var14.getValue() > (Double)var12.getValue()));

					var12 = var14;
				}
			}

			return new Bid(this.utilitySpace.getDomain(), bidP);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
