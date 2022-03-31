package caduceusdc16.boacomponents;

import genius.core.Bid;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.uncertainty.UserModel;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;


public class AC extends AcceptanceStrategy {
    private Bid receivedBid;
    private Bid lastOwnBid;
    private double a;
    private double b;
    private static double MINIMUM_TARGET = 0.8;

    public AC() {
    }
    @Override
    public Actions determineAcceptability() {
        receivedBid = negotiationSession.getOpponentBidHistory().getLastBid();
        lastOwnBid = negotiationSession.getOwnBidHistory().getLastBid();
        UserModel user_model = this.negotiationSession.getUserModel();
        double time = negotiationSession.getTime();


        if (receivedBid == null || lastOwnBid == null) {
            return Actions.Reject;

        } if (time < 0.15D) {
            double alpha = 1.0D;
            double beta = 0.005D;
            double receivedUtil2 = negotiationSession.getUtilitySpace().getUtility(receivedBid);
            double UtilToSend2 = negotiationSession.getUtilitySpace().getUtility(lastOwnBid);

            if (alpha * receivedUtil2 + beta >= UtilToSend2) {
                return Actions.Accept;
            } else return Actions.Reject;

        } else {
            double receivedUtil = negotiationSession.getUtilitySpace().getUtility(receivedBid);
            double targetUtil = negotiationSession.getUtilitySpace().getUtility((lastOwnBid));
            double new_targetUtil = 0.7D * (1D - (time - 0.15) * (time - 0.15) * (time - 0.15)) + 0.3D;

            if (receivedUtil >= new_targetUtil) {
                return Actions.Accept;
            } else {
                return Actions.Reject;
            }

        }

    }

    @Override
    public String getName() {
        return "TransformerAcceptanceStrategy";
    }



}





