package ua.agentlab;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

/**
   This example shows how to implement the initiator role in 
   a FIPA-contract-net interaction protocol. In this case in particular 
   we use a <code>ContractNetInitiator</code>  
   to assign a dummy task to the agent that provides the best offer
   among a set of agents (whose local
   names must be specified as arguments).
   @author Giovanni Caire - TILAB
 */
public class ContractNetInitiatorAgent extends Agent {
	private int nResponders;

	protected void setup() { 
  	// Read names of responders as arguments
  	Object[] args = getArguments();
  	if (args != null && args.length > 1) {
  		final int area = Integer.parseInt(args[0].toString());
  		nResponders = args.length - 1;
  		
  		System.out.println("Need repair: appartment area " + area + " in " + nResponders + " firms.");
  		System.out.println("Repair firms:");
  		
  		ACLMessage initMessage = new ACLMessage(ACLMessage.CFP);
  		for (int i = 1; i < args.length; ++i) {
  			System.out.println("'" + args[i] + "'");
  			initMessage.addReceiver(new AID((String) args[i], AID.ISLOCALNAME));
  		}
  		System.out.println("--------------------------------------------");
		initMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
		// We want to receive a reply in 10 secs
		initMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
		initMessage.setContent("do-repair: " + area);

		addBehaviour(new ContractNetInitiator(this, initMessage) {

			protected void handlePropose(ACLMessage propose, Vector v) {
				System.out.println("Repair by "+propose.getSender().getLocalName()+" will cost " +propose.getContent() + "$ for " + area + "m^2");
			}

			protected void handleRefuse(ACLMessage refuse) {
				System.out.println("Repair firm "+refuse.getSender().getLocalName()+" refuse request for " + area + " m^2");
			}

			protected void handleFailure(ACLMessage failure) {
				if (failure.getSender().equals(myAgent.getAMS())) {
					System.out.println("Repair firm does not exist anymore");
				}
				else {
					System.out.println("Repair firm  "+failure.getSender().getLocalName()+" failed");
				}
				nResponders--;
			}

			protected void handleAllResponses(Vector responses, Vector acceptances) {
				if (responses.size() < nResponders) {
					System.out.println("Timeout expired: missing "+(nResponders - responses.size())+" repair firms");
				}
				// Evaluate proposals.
				int bestPrice = Integer.MAX_VALUE;
				AID bestProposer = null;
				ACLMessage accept = null;
				Enumeration e = responses.elements();
				while (e.hasMoreElements()) {
					ACLMessage msg = (ACLMessage) e.nextElement();
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						reply.setContent("Price is too high!");
						acceptances.addElement(reply);
						int proposal = Integer.parseInt(msg.getContent());
						if (proposal < bestPrice) {
							bestPrice = proposal;
							bestProposer = msg.getSender();
							accept = reply;
						}
					}
				}
				// Accept the proposal of the best proposer
				if (accept != null) {
					System.out.println("Accepting proposal "+bestPrice+"$ from repair firm '"+bestProposer.getLocalName() + "'");
					accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					accept.setContent("You propose the best price!");
				} else {
					System.out.println("Can't do repair for " + area + " m^2 because all firms refused");
				}
			}

			protected void handleInform(ACLMessage inform) {
				System.out.println("Repair firm '"+inform.getSender().getLocalName()+"' successfully did repair for " + area + " m^2");
			}
		});
  	}
  	else {
  		System.out.println("No repair firms specified.");
  	}
  } 
}