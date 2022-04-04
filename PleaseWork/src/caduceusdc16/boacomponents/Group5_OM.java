package caduceusdc16.boacomponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.Bid;
import genius.core.BidHistory;
import genius.core.Domain;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.issue.ISSUETYPE;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.issue.ValueInteger;
import genius.core.issue.ValueReal;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import negotiator.boaframework.opponentmodel.tools.UtilitySpaceAdapter;
import org.apache.commons.math3.stat.inference.ChiSquareTest;


public class Group5_OM extends OpponentModel {


    // Global frequency model
    protected Hashtable<String, Object> freqModel;
    protected Hashtable<Number, String> issueIdToName;
    protected Hashtable<String, Double> issueWeight;
    // Bid windows
    protected List<Bid> win1;
    protected List<Bid> win2;

    protected SortedOutcomeSpace sortedOutcomeSpace;
    // Global parameters
    protected int WINDOW_SIZE = 5;
    protected double THRESHOLD = 0.7;
    protected double ALPHA = 10.0;
    protected double BETA = 5.0;


    @Override
    public String getName() {
        return "Distribution-based Model";
    }


    @Override
    // Opponent Modeling initialization
    public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {

        super.init(negotiationSession, parameters);

        // Initialize window
        win1 = new ArrayList<Bid>();
        win2 = new ArrayList<Bid>();
        if (parameters.containsKey("window_size")) {
            WINDOW_SIZE = (int) parameters.get("window_size").intValue();
        }
        if (parameters.containsKey("threshold")) {
            THRESHOLD = (double) parameters.get("threshold");
        }
        if (parameters.containsKey("alpha")) {
            ALPHA = (double) parameters.get("alpha");
        }
        if (parameters.containsKey("beta")) {
            BETA = (double) parameters.get("beta");
        }


        // Initialize tables
        freqModel = new Hashtable<String, Object>();
        issueIdToName = new Hashtable<Number, String>();
        issueWeight = new Hashtable<String, Double>();
        // Get list of domain issues
        List<Issue> issues = negotiationSession.getIssues();
        double initialWeight = 1.0 / issues.size();

        for (Issue i : issues) {

            // Map issue number to its name
            issueIdToName.put(i.getNumber(), i.getName());

            // Map issue name to its frequency model weight
            issueWeight.put(i.getName(), initialWeight);


            if (i.getType() == ISSUETYPE.DISCRETE) {

                IssueDiscrete d = (IssueDiscrete) i;

                // Initialize appearances of every possible value of the issue to 1
                Hashtable<String, Integer> countValues = getAllValues(d, 1);
                freqModel.put(i.getName(), countValues);
            } else if (i.getType() == ISSUETYPE.REAL) {
                freqModel.put(i.getName(), new ArrayList<Double>());
            } else if (i.getType() == ISSUETYPE.INTEGER) {
                freqModel.put(i.getName(), new ArrayList<Integer>());
            }

        }

    }

    public Hashtable<String, Integer> getAllValues(IssueDiscrete i, int alpha) {

        // Dictionary to map every value with the amount of times it has appeared
        // in an opponent's bid
        Hashtable<String, Integer> countValues = new Hashtable<String, Integer>();

        // List of possible values for the issue
        List<ValueDiscrete> possibleValues = i.getValues();

        for (ValueDiscrete v : possibleValues) {
            countValues.put(v.getValue(), alpha);
        }

        return countValues;
    }


    @SuppressWarnings("unchecked")
    protected void updateFrequencyModel(Bid bid) {
        HashMap<Integer, Value> values = bid.getValues();

        Set<Number> keys = issueIdToName.keySet();
        // for every issue
        for (Number key : keys) {
            // get issue name and its value
            String name = issueIdToName.get(key);
            Value value = values.get(key);

            // Get object that the frequency model has stored for the corresponding issue
            // Depending on issue type it can be a Hashtable or an ArrayList of numbers
            Object o = freqModel.get(name);

            // If the issue is a Discrete issue
            if (o instanceof Hashtable) {
                // Update number of appearances of its value
                Hashtable<String, Integer> countValues = (Hashtable<String, Integer>) o;
                if (countValues.containsKey(value.toString())) {
                    int count = countValues.get(value.toString());
                    count++;
                    // update hashtable
                    countValues.replace(value.toString(), count);
                } else {
                    System.out.printf("ERROR :: Frequency model found value {%s} for issue {%s} that wasn't initialized\n", name, value.toString());
                }
                // update hashtable
                freqModel.replace(name, countValues);
            }

        } // end of keys For

    }


    /**
     * Compares old and new windows and checks, for every issue, if the opponent has concealed.
     * This is done by comparing the change in the frequencies for every possible value of an issue
     * between both windows.
     * If the value with maximum frequency for one issue does not match for both windows, we
     * consider that the opponent concealed.
     * The weigths for the non-concealed issues are increased at the end of the comparison
     */
    protected void updateWeights() {
        int totalOffers = negotiationSession.getOpponentBidHistory().getHistory().size();
        Hashtable<String, Hashtable<String, Double>> win1Dic = getWindowFreq(win1);
        Hashtable<String, Hashtable<String, Double>> win2Dic = getWindowFreq(win2);
        Hashtable<String, Boolean> concealed = new Hashtable<String, Boolean>();
        int con = 0;

        //double list for chi square test

        Set<String> issues = win1Dic.keySet();

        for (String issue : issues) {
            Hashtable<String, Double> win1Values = win1Dic.get(issue);
            Hashtable<String, Double> win2Values = win2Dic.get(issue);

            Set<String> values_win1 = win1Values.keySet();

            double[] ob = new double[values_win1.size()];
            double[] ex = new double[values_win1.size()];
            int i = 0;

            for (String value : values_win1) {
                double win1Freql = win1Values.get(value);
                double win2Freq = win2Values.get(value);
                ob[i] = win1Freql;
                ex[i] = win2Freq;
                i++;

            }
            float merge = 0;
            double distance = 0;
            float common = 0;
            for (i = 0; i < ob.length; i++) {
                if (ob[i] == ex[i])
                    common++;
                else distance = distance + Math.abs(ob[i] - ex[i]);
            }
            merge = ob.length + ex.length - common;
            float jaccard_distance = (1 - common / merge);

            System.out.println(jaccard_distance);
            if (jaccard_distance > THRESHOLD) {
                concealed.put(issue, true);
            } else {
                concealed.put(issue, false);
                con++;
            }

        }
        if (con != issues.size()) {

            for (String issue : issues) {

                if (!concealed.get(issue)) {
                    double oldW = issueWeight.get(issue);
                    double newW = oldW + getDelta(10, 5);
                    issueWeight.replace(issue, newW);
                }
            }
        }
        scaleWeights();

    }


    @Override
    protected void updateModel(Bid bid, double time) {
        updateFrequencyModel(bid);

        if (win2.size() == WINDOW_SIZE) {

            // Update weights
            updateWeights();

            // Copy win2 into win1
            win1.clear();
            for (Bid b : win2) {
                Bid copy = new Bid(b);
                win1.add(copy);
            }

            // Clear win2
            win2.clear();
        }

        // Add bids to window 1 (only at the beginning of the negotiation
        if (win1.size() < WINDOW_SIZE) {
            win1.add(bid);
        }
        // If window 1 already full, add bids to window 2
        else if (win2.size() < WINDOW_SIZE) {
            win2.add(bid);
        }


    }


    public double getOpponentUtility(Bid bid) {
        HashMap<Integer, Value> values = bid.getValues();

        double utility = 0;

        Set<Number> keys = issueIdToName.keySet();

        for (Number key : keys) {
            // get issue name and its value
            String name = issueIdToName.get(key);
            Value value = values.get(key);
            double count = -1;
            double max = -1;
            double temp;

            Hashtable<String, Integer> countValues = (Hashtable<String, Integer>) freqModel.get(name);

            // Variable "count" stores the number of times value
            // "value" has appeared in a bid since the beginning
            count = (double) countValues.get(value.toString());

            // Variable "max" stores the number of times that the maximum value
            // for that issue appeared in a bid since the beginning
            Set<String> seenValues = countValues.keySet();
            for (String key1 : seenValues) {
                temp = countValues.get(key1);
                if (temp > max) max = temp;
            }

            double valueFreq = count * 1.0 / max;
            double issueScore = issueWeight.get(name) * valueFreq;
            utility += issueScore;


        }
        return utility;
    }

    @Override
    // Estimation of the opponent's utility of a given bid
    public double getBidEvaluation(Bid bid) {

        double utility = 0;

        if (negotiationSession.getTimeline().getCurrentTime() > 5) {

            utility = getOpponentUtility(bid);

            return utility;
        } else {

            return negotiationSession.getUtilitySpace().getUtility(bid);
        }

    }

    @Override
    public AdditiveUtilitySpace getOpponentUtilitySpace() {
        AdditiveUtilitySpace utilitySpace = new UtilitySpaceAdapter(this, this.negotiationSession.getDomain());
        return utilitySpace;
    }

    @Override
    // Parameters for the Opponent Model component in Genius
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("window_size", 5.0, "Window capacity in number of opponent's bid"));
        set.add(new BOAparameter("threshold", 0.7, "Min. p value for chi test"));
        set.add(new BOAparameter("alpha", 10.0, "Base weight increase"));
        set.add(new BOAparameter("beta", 5.0, "Controls influence of time in weight update. Higher values = more increment"));
        return set;
    }


    /**
     * Given a hash table with value appearances, returns a similar structure
     * with value frequencies with respect to a specified total
     */
    public Hashtable<String, Hashtable<String, Double>> getFrequency(Hashtable<String, Object> model, int total) {

        // Hashtable to store the frequency of every value of every issue
        Hashtable<String, Hashtable<String, Double>> frequencies = new Hashtable<String, Hashtable<String, Double>>();

        Set<String> keys = model.keySet();
        for (String issue : keys) {

            // Hashtable to store the frequency of every value of a specific issue
            Hashtable<String, Double> valueFreq = new Hashtable<String, Double>();

            Object o = model.get(issue);
            // If the issue is a Discrete issue
            if (o instanceof Hashtable) {
                @SuppressWarnings("unchecked")
                Hashtable<String, Integer> countValues = (Hashtable<String, Integer>) o;

                Set<String> possibleValues = countValues.keySet();
                for (String value : possibleValues) {
                    // Get number of times the value appeared in a bid
                    double count = countValues.get(value);
                    // Frequency is calculated as "count" divided by a given total (number of rounds, elements of a window)

                    double freq = count * 1.0 / (total + possibleValues.size());
                    // Store freq in hashtable
                    valueFreq.put(value, count);
                }
                frequencies.put(issue, valueFreq);
            }
        }


        return frequencies;
    }
    /**
     * Get the issue frequency for each window
     */
    public Hashtable<String, Hashtable<String, Double>> getWindowFreq(List<Bid> window) {

        // Get base empty model with Laplace smoothing
        Hashtable<String, Object> model = getEmptyModel();

        // For every bid in the window
        for (Bid bid : window) {
            HashMap<Integer, Value> bidValues = bid.getValues();

            Set<Number> keys = issueIdToName.keySet();
            // For every issue
            for (Number key : keys) {

                // Get issue name and its value
                String name = issueIdToName.get(key);
                Value value = bidValues.get(key);


                // Add one to its appearances
                Hashtable<String, Integer> countValues = (Hashtable<String, Integer>) model.get(name);
                int count = countValues.get(value.toString());
                count += 1;
                countValues.replace(value.toString(), count);
            }
        }

        // Once the model is complete with the window data, calculate frequencies and return
        return getFrequency(model, window.size());
    }

    /**
     * Generate an empty model
     */
    public Hashtable<String, Object> getEmptyModel() {
        // Initialize tables
        Hashtable<String, Object> emptyModel = new Hashtable<String, Object>();

        // Get list of domain issues
        List<Issue> issues = negotiationSession.getIssues();
        // initial weights of each issues are equal
        double initialWeight = 1.0 / issues.size();

        for (Issue i : issues) {
            String name = i.getName();
            IssueDiscrete d = (IssueDiscrete) i;
            // Initialize appearances of every possible value of the issue to 1
            Hashtable<String, Integer> countValues = getAllValues(d, 1);
            emptyModel.put(i.getName(), countValues);

        }

        return emptyModel;
    }
    /**
     * Calculate the issue weight increase
     */
    public double getDelta(double alpha, double beta) {

        return alpha * (1 - Math.pow(negotiationSession.getTime(), beta));
    }

    /**
     * Scales the weights of the issues so that they add up to 1
     */
    public void scaleWeights() {
        Set<String> issues = issueWeight.keySet();
        double sum = 0;
        for (String issue : issues) {
            sum += issueWeight.get(issue);
        }
        for (String issue : issues) {
            double w = issueWeight.get(issue);
            w /= sum;
            issueWeight.replace(issue, w);
        }
    }

    /**
     * Helper method to add all elements in an array
     */
    public double sum(double... values) {
        double result = 0;
        for (double value : values)
            result += value;
        return result;
    }
}



//
//    protected String printWeights() {
//        String s = "{\n";
//        Set<String> issues = issueWeight.keySet();
//        for (String issue : issues) {
//            String issueName = String.format("%30s", issue);
//            String rounded = String.format("%.8f", issueWeight.get(issue));
//
//            s += "\t"+issueName+" : "+rounded+"\n";
//        }
//        s += "}\n";
//        return s;
//    }
//
//}
