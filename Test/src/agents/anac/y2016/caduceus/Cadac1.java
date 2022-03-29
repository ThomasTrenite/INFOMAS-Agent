//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package agents.anac.y2016.caduceus;

import agents.anac.y2016.caduceus.agents.Atlas3.Atlas3;
import agents.anac.y2016.caduceus.agents.Caduceus.UtilFunctions;
import agents.anac.y2016.caduceus.agents.RandomDance.RandomDance;
import agents.anac.y2016.caduceus.agents.kawaii.kawaii;
import agents.anac.y2016.grandma.GrandmaAgent;
import bilateralexamples.BoaPartyExample;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.ActionWithBid;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.parties.NegotiationParty;
import genius.core.persistent.DefaultPersistentDataContainer;
import genius.core.persistent.PersistentDataType;
import genius.core.uncertainty.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Cadac1 extends AbstractNegotiationParty {
	public double discountFactor = 0.0D;
	private double selfReservationValue = 0.75D;
	private double percentageOfOfferingBestBid = 0.83D;
	private Random random;
	public NegotiationParty[] agents = new NegotiationParty[5];
	public double[] scores = UtilFunctions.normalize(new double[]{500.0D, 10.0D, 5.0D, 3.0D, 1.0D});

	public Cadac1() {
	}

	public double getScore(int var1) {
		return this.scores[var1];
	}

	public void init(NegotiationInfo var1) {
		super.init(var1);
		this.random = new Random(var1.getRandomSeed());
		//this.agents[0] = new ParsAgent();
		this.agents[0] = new BoaPartyExample();
		this.agents[1] = new RandomDance();
		this.agents[2] = new kawaii();
		this.agents[3] = new Atlas3();
		this.agents[4] = new GrandmaAgent();
		//this.agents[4] = new agents.anac.y2016.caduceus.agents.Caduceus.Caduceus();
		this.discountFactor = this.getUtilitySpace().getDiscountFactor();
		double var2 = this.getUtilitySpace().getReservationValueUndiscounted();
		System.out.println("Discount Factor is " + this.discountFactor);
		System.out.println("Reservation Value is " + var2);
		this.selfReservationValue = Math.max(this.selfReservationValue, var2);
		this.percentageOfOfferingBestBid *= this.discountFactor;
		NegotiationParty[] var4 = this.agents;
		int var5 = var4.length;

		for(int var6 = 0; var6 < var5; ++var6) {
			NegotiationParty var7 = var4[var6];
			var7.init(new NegotiationInfo(this.getUtilitySpace(), var1.getUserModel(), (User)null, var1.getDeadline(), var1.getTimeline(), var1.getRandomSeed(), var1.getAgentID(), new DefaultPersistentDataContainer((Serializable)null, PersistentDataType.DISABLED)));
		}

	}

	public Action chooseAction(List<Class<? extends Action>> var1) {
		if (this.isBestOfferTime()) {
			Bid var2 = this.getBestBid();
			if (var2 != null) {
			//	System.out.println("bestbidPartyID "+this.getPartyId());
				return new Offer(this.getPartyId(), var2);
			}

			System.err.println("Best Bid is null?");
		}

		ArrayList var12 = new ArrayList();
		ArrayList var3 = new ArrayList();
		NegotiationParty[] var4 = this.agents;
		int var5 = var4.length;

		for(int var6 = 0; var6 < var5; ++var6) {
			NegotiationParty var7 = var4[var6];
			Action var8 = var7.chooseAction(var1);
			var3.add(var8);
		}

		double var13 = 0.0D;
		double var14 = 0.0D;
		ArrayList var15 = new ArrayList();
		int var9 = 0;

		for(Iterator var10 = var3.iterator(); var10.hasNext(); ++var9) {
			Action var11 = (Action)var10.next();
			if (var11 instanceof Accept) {
				var13 += this.getScore(var9);
			} else if (var11 instanceof Offer) {
				var14 += this.getScore(var9);
				var12.add(((Offer)var11).getBid());
				var15.add(var9);
			}
		}

		if (var13 > var14) {
			return new Accept(this.getPartyId(), ((ActionWithBid)this.getLastReceivedAction()).getBid());
		} else if (var14 > var13) {
			return new Offer(this.getPartyId(), this.getRandomizedAction(var15, var12));
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
				//if ()
				return (Bid)var2.get(var4);
			}

			++var4;
		}

		return null;
	}

	public void receiveMessage(AgentID var1, Action var2) {
		super.receiveMessage(var1, var2);
		NegotiationParty[] var3 = this.agents;
		int var4 = var3.length;

		for(int var5 = 0; var5 < var4; ++var5) {
			NegotiationParty var6 = var3[var5];
			var6.receiveMessage(var1, var2);
		}

	}

	public String getDescription() {
		return "ANAC2016";
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
}
