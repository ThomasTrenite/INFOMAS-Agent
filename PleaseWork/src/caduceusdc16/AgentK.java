package caduceusdc16;

import caduceusdc16.boacomponents.AC;
import caduceusdc16.boacomponents.Group5_OM;
import caduceusdc16.boacomponents.oms;
import genius.core.boaframework.*;
import genius.core.parties.NegotiationInfo;
import negotiator.boaframework.offeringstrategy.anac2010.AgentK_Offering;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class AgentK extends BoaParty{



    @Override
    public void init(NegotiationInfo info)
    {
        // The choice for each component is made here
        AcceptanceStrategy ac  = new AC();
        OfferingStrategy os  = new AgentK_Offering();
        OpponentModel om  = new Group5_OM();
        OMStrategy oms = new oms();

        System.out.println("AgentK being run!");

        // All component parameters can be set below.
        Map<String, Double> noparams = Collections.emptyMap();
        Map<String, Double> osParams = new HashMap<String, Double>();
        // Set the concession parameter "e" for the offering strategy to yield Boulware-like behavior
        //osParams.put("e", 0.2);

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
        return "AgentK with transformer acceptance strategy and opponent model";
    }

    // All the rest of the agent functionality is defined by the components selected above, using the BOA framework
}
