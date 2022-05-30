package Resource;

import Utilities.Constants;
import Utilities.DFInteraction;
import jade.core.Agent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import Libraries.IResource;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.proto.ContractNetResponder;

import javax.swing.text.Utilities;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class ResourceAgent extends Agent {

    String id;
    IResource myLib;
    String description;
    String[] associatedSkills;
    String location;
    boolean occupied = false;

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        // ID: OP, GS1/2, QCS1/2
        this.id = (String) args[0];
        this.description = (String) args[1];

        //Load hw lib
        try {
            String className = "Libraries." + (String) args[2];
            Class cls = Class.forName(className);
            Object instance;
            instance = cls.newInstance();
            myLib = (IResource) instance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(ResourceAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Locations: GlueStation1, GlueStation2, Operator, QualityControlStation1, QualityControlStation2
        this.location = (String) args[3];

        myLib.init(this);
        this.associatedSkills = myLib.getSkills();
        System.out.println("Resource Deployed: " + this.id + " Executes: " + Arrays.toString(associatedSkills));

        // Register in DF with the corresponding skills as services
        try {
            DFInteraction.RegisterInDF(this, this.associatedSkills, Constants.DFSERVICE_RESOURCE);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Add responder behaviour/s - always live
        this.addBehaviour(new CFPResponder(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
        this.addBehaviour(new ReqResourceResp(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
    }

    private class ReqResourceResp extends AchieveREResponder {

        public ReqResourceResp(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            ACLMessage msg = request.createReply();
            msg.setPerformative(ACLMessage.AGREE);
            return msg;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            // Execute skill
            boolean isDone = myLib.executeSkill(request.getContent());
            ACLMessage msg = request.createReply();
            if (isDone) {
                msg.setPerformative(ACLMessage.INFORM);
                //msg.setContent(Arrays.toString(associatedSkills));
                if (Objects.equals(request.getContent(), Constants.SK_QUALITY_CHECK)) {
                    String quality = verifyQuality() == 1 ? "OK" : "NOK";
                    msg.setContent(quality);
                }
            } else {
                msg.setPerformative(ACLMessage.FAILURE);
            }
            occupied = false;
            return msg;
        }
    }

    private int verifyQuality() {
        //int quality = 0;
        return Math.random() > 0.5 ? 1 : 0;
    }

    private class CFPResponder extends ContractNetResponder {

        public CFPResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
            ACLMessage msg = cfp.createReply();
            if (!occupied) {
                msg.setPerformative(ACLMessage.PROPOSE);
                StringBuilder content = new StringBuilder();
                for (String skill: associatedSkills) {
                    content.append(skill);
                    content.append(":");
                }
                msg.setContent(String.valueOf(content));
            } else {
                msg.setPerformative(ACLMessage.REFUSE);
            }
            return msg;
        }

        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            block(1000);
            ACLMessage msg = cfp.createReply();
            msg.setPerformative(ACLMessage.INFORM);
            msg.setContent(location);
            occupied = true;
            return msg;
        }
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }
}
