package ua.agentlab;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;

/**
   This example shows how to implement the responder role in 
   a FIPA-contract-net interaction protocol. In this case in particular 
   we use a <code>ContractNetResponder</code>  
   to participate into a negotiation where an initiator needs to assign
   a task to an agent among a set of candidates.
   @author Giovanni Caire - TILAB
 */
public class ContractNetResponderAgent extends Agent {
	private static final String[] allCars = new String[]{"VW", "BMW", "Honda", "Ford", "Nissan", "Audi"};

	private float coef = 1;
	private final Random rand = new Random();
	private int areaBarrier = 50 + rand.nextInt(51); 

	protected void setup() {
		System.out.println("Repair firm "+getLocalName()+" waiting for CFP request...");
		coef += rand.nextInt(100) / 100.0f;
		System.out.println("Coef = " + coef + ", barrier = " + areaBarrier);
		System.out.println("--------------------------------------------");
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );

		addBehaviour(new ContractNetResponder(this, template) {
			@Override
			protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
				System.out.println("Repair firm "+getLocalName()+": CFP received from "+cfp.getSender().getLocalName()+". Action is "+cfp.getContent());
				int area = Integer.parseInt(cfp.getContent().substring("do-repair: ".length()));
				int price = (int) (area * coef * 100);
				if (area >= areaBarrier) {
					// We provide a proposal
					System.out.println("Repair firm "+getLocalName()+": proposing repair for " + area + " m^2 for " + price + "$");
					ACLMessage propose = cfp.createReply();
					propose.setPerformative(ACLMessage.PROPOSE);
					propose.setContent(Integer.toString(price));
					return propose;
				}
				else {
					// We refuse to provide a proposal
					System.out.println("Repair firm "+getLocalName()+": refuse because area " + area + " m^2 is too small...");
					throw new RefuseException("evaluation-failed");
				}
			}

			@Override
			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
				System.out.println("Repair firm "+getLocalName()+": Proposal accepted");
				if (performAction()) {
					System.out.println("Repair firm "+getLocalName()+": doing repair successfully performed: " + accept.getContent());
					ACLMessage inform = accept.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					return inform;
				}
				else {
					System.out.println("Repair firm "+getLocalName()+": repairing failed because of default");
					throw new FailureException("default");
				}	
			}

			protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				System.out.println("Repair firm "+getLocalName()+": proposal for doing repair rejected. Reason: " + reject.getContent());
			}
		} );
	}

	private boolean performAction() {
		// Simulate action execution by generating a random number
		return rand.nextInt(100) < 10;
	}
}