package caduceusdc16.boacomponents;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.bidding.BidDetailsSorterUtility;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.BOAparameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

public class oms extends OMStrategy {
	private long possibleBids;
	private Random random;
	private BidDetailsSorterUtility comp = new BidDetailsSorterUtility();

	public oms() {
	}

	@Override
	public void init(NegotiationSession var1, OpponentModel var2, Map<String, Double> var3) {
		this.initializeAgent(var1, var2, var3);
		if (var3.get("t") != null) {
			updateThreshold = var3.get("t").doubleValue();
		} else {
			System.out.println("OMStrategy assumed t = 1.1");
		}
	}

	private void initializeAgent(NegotiationSession var1, OpponentModel var2, Map<String, Double> var3) {
		this.negotiationSession = var1;

		try {
			super.init(this.negotiationSession, var2, new HashMap());
		} catch (Exception var4) {
			var4.printStackTrace();
		}

		this.possibleBids = this.negotiationSession.getUtilitySpace().getDomain().getNumberOfPossibleBids();
		this.random = new Random();
	}

	@Override
	public BidDetails getBid(List<BidDetails> var1) {
		ArrayList var2 = new ArrayList();
		Iterator var3 = var1.iterator();
		if (negotiationSession.getTime()<0.3) {
			while (var3.hasNext()) {
				BidDetails var4 = (BidDetails) var3.next();

				try {
					double var5 = this.model.getBidEvaluation(var4.getBid());
					BidDetails var7 = new BidDetails(var4.getBid(), var5);
					var2.add(var7);
				} catch (Exception var9) {
					var9.printStackTrace();
				}
			}

			int var10 = (int) Math.round((double) var2.size() / 10.0D);
			if (var10 < 3) {
				var10 = 3;
			}

			if (var10 > 20) {
				var10 = 20;
			}

			Collections.sort(var2, this.comp);
			int var11 = this.random.nextInt(Math.min(var2.size(), var10));
			Bid var12 = ((BidDetails) var2.get(var11)).getBid();
			BidDetails var6 = null;

			try {
				var6 = new BidDetails(var12, this.negotiationSession.getUtilitySpace().getUtility(var12), this.negotiationSession.getTime());
			} catch (Exception var8) {
				var8.printStackTrace();
			}

			return var6;
		}else {
			if (var1.size() == 1) {
				return var1.get(0);
			}
			double bestUtil = -1;
			BidDetails bestBid = var1.get(0);

			// 2. Check that not all bids are assigned at utility of 0
			// to ensure that the opponent model works. If the opponent model
			// does not work, offer a random bid.
			boolean allWereZero = true;
			// 3. Determine the best bid
			for (BidDetails bid : var1) {
				double evaluation = model.getBidEvaluation(bid.getBid());
				if (evaluation > 0.0001) {
					allWereZero = false;
				}
				if (evaluation > bestUtil) {
					bestBid = bid;
					bestUtil = evaluation;
				}
			}
			// 4. The opponent model did not work, therefore, offer a random bid.
			if (allWereZero) {
				Random r = new Random();
				return var1.get(r.nextInt(var1.size()));
			}
			return bestBid;
		}
	}

/*
	public BidDetails getBid(List<BidDetails> allBids) {

		// 1. If there is only a single bid, return this bid
		if (allBids.size() == 1) {
			return allBids.get(0);
		}
		double bestUtil = -1;
		BidDetails bestBid = allBids.get(0);

		// 2. Check that not all bids are assigned at utility of 0
		// to ensure that the opponent model works. If the opponent model
		// does not work, offer a random bid.
		boolean allWereZero = true;
		// 3. Determine the best bid
		for (BidDetails bid : allBids) {
			double evaluation = model.getBidEvaluation(bid.getBid());
			if (evaluation > 0.0001) {
				allWereZero = false;
			}
			if (evaluation > bestUtil) {
				bestBid = bid;
				bestUtil = evaluation;
			}
		}
		// 4. The opponent model did not work, therefore, offer a random bid.
		if (allWereZero) {
			Random r = new Random();
			return allBids.get(r.nextInt(allBids.size()));
		}
		return bestBid;
	}
*/

	/**
	 * when to stop updating the opponentmodel. Note that this value is not
	 * exactly one as a match sometimes lasts slightly longer.
	 */
	double updateThreshold = 1.1;

	/**
	 * Initializes the opponent model strategy. If a value for the parameter t
	 * is given, then it is set to this value. Otherwise, the default value is
	 * used.
	 *
	 * @param negotiationSession
	 *            state of the negotiation.
	 * @param model
	 *            opponent model used in conjunction with this opponent modeling
	 *            strategy.
	 * @param parameters
	 *            set of parameters for this opponent model strategy.
	 */

	/**
	 * The opponent model may be updated, unless the time is higher than a given
	 * constant.
	 * 
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() {
		return negotiationSession.getTime() < updateThreshold;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("t", 1.1, "Time after which the OM should not be updated"));
		return set;
	}

	@Override
	public String getName() {
		return "oms";
	}
}