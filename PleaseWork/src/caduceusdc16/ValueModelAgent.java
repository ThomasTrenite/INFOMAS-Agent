package caduceusdc16;

import caduceusdc16.boacomponents.AC;
import caduceusdc16.boacomponents.Group5_OM;
import caduceusdc16.boacomponents.TimeDependent_Offering;
import caduceusdc16.boacomponents.oms;
import genius.core.boaframework.*;
import genius.core.parties.NegotiationInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This example shows how BOA components can be made into an independent
 * negotiation party and which can handle preference uncertainty.
 *
 * Note that this is equivalent to adding a BOA party via the GUI by selecting
 * the components and parameters. However, this method gives more control over
 * the implementation, as the agent designer can choose to override behavior
 * (such as handling preference uncertainty).
 * <p>
 * For more information, see: Baarslag T., Hindriks K.V., Hendrikx M.,
 * Dirkzwager A., Jonker C.M. Decoupling Negotiating Agents to Explore the Space
 * of Negotiation Strategies. Proceedings of The Fifth International Workshop on
 * Agent-based Complex Automated Negotiations (ACAN 2012), 2012.
 * https://homepages.cwi.nl/~baarslag/pub/Decoupling_Negotiating_Agents_to_Explore_the_Space_of_Negotiation_Strategies_ACAN_2012.pdf
 *
 * @author Tim Baarslag
 */
@SuppressWarnings("serial")
public class ValueModelAgent extends BoaParty
{
    @Override
    public void init(NegotiationInfo info)
    {
        // The choice for each component is made here
        AcceptanceStrategy ac  = new AC();
        OfferingStrategy os  = new TimeDependent_Offering();
        OpponentModel om  = new Group5_OM();
        OMStrategy oms = new oms();

        System.out.println("ValueModel being run!");

        // All component parameters can be set below.
        Map<String, Double> noparams = Collections.emptyMap();
        Map<String, Double> osParams = new HashMap<String, Double>();
        // Set the concession parameter "e" for the offering strategy to yield Boulware-like behavior
        osParams.put("e", 0.2);

        // Initialize all the components of this party to the choices defined above
        configure(ac, noparams,
                os,	osParams,
                om, noparams,
                oms, noparams);
        super.init(info);
    }

    @Override
    public String getDescription()
    {
        return "Value Model with enhanced transformer opponent model and acceptance strategy";
    }

    // All the rest of the agent functionality is defined by the components selected above, using the BOA framework
}
