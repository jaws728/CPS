package Product;

import Utilities.Constants;
import Utilities.DFInteraction;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class ProductAgent extends Agent {

    String id;
    ArrayList<String> executionPlan = new ArrayList<>();
    private boolean FIPAflag = false;
    private boolean CFPflag = false;
    // String nextPos;
    private int currPos = 0;
    private AID resouceCFPId = null;
    private boolean planEnded = false;
    // TO DO: Add remaining attributes required for your implementation

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        this.id = (String) args[0];
        this.executionPlan = this.getExecutionList((String) args[1]);
        System.out.println("Product launched: " + this.id + " Requires: " + executionPlan);

        // TODO: Add necessary behaviour/s for the product to control the flow
        // of its own production
        // 1. Behaviours
        // SequentialBehaviour sb = new SequentialBehaviour();
        // for (String state: this.executionPlan) {
        //     sb.addSubBehaviour(new SimpleState(this, state));
        // }
        // this.addBehaviour(sb);

        //2. FIPA
        //ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        //msg.addReceiver(new AID("responder", false));
        //this.addBehaviour(new ReqTransportInit(this, msg));
        //this.addBehaviour(new ReqResourceInit(this, msg));
        //ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
        //msgCFP.addReceiver(new AID("responder", false));
        //this.addBehaviour(new CFPInit(this, msgCFP));

        //3. Register Agent
        //DFInteraction.RegisterInDF(this, this.id);
    }

    // Get the next state - if there is no next task - put planEnded to true
    private class GetNextTask extends OneShotBehaviour {
        @Override
        public void action() {
            if (currPos < executionPlan.size()) {
                currPos++;
                System.out.println("Next task get: " + executionPlan.get(currPos));
            } else {
                planEnded = true;
                System.out.println("Ended all tasks: " + planEnded);
            }
        }
    }

    // Sending CFP requests to all resources
    private class SendCFP extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
            DFAgentDescription[] target = null;
            try {
                target = DFInteraction.SearchInDFByType(Constants.DFSERVICE_RESOURCE, myAgent);
                for (int i = 0; i < target.length; i++){
                    msgCFP.addReceiver(target[i].getName());
                    myAgent.addBehaviour(new CFPInit(myAgent, msgCFP));
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    // All CFP responses received - handled it and decide the next state
    private  class HandleCFP extends SimpleBehaviour {
        private boolean finished = false;

        @Override
        public void action() {
            if (CFPflag){
                System.out.println("All negotiation done");
                // Transport? get the positions -> now and station
                // if not: pass to execute skill
                // ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                // msg.addReceiver(resouceCFPId);
                // myAgent.addBehaviour(new ReqResourceInit(myAgent, msg));
                CFPflag = false;
                finished = true;
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }

    private class AGVcontrol extends SimpleBehaviour {

        @Override
        public void action() {

        }

        @Override
        public boolean done() {
            return false;
        }
    }
    private class ExecuteSkill extends SimpleBehaviour {

        @Override
        public void action() {

        }

        @Override
        public boolean done() {
            return false;
        }
    }

    /*
    private class SimpleState extends SimpleBehaviour {

        private boolean finished = false;
        String currState;

        public SimpleState(Agent a, String currState){
            super(a);
            this.currState = currState;
        }

        @Override
        public void action() {
            switch(currState){
                case Constants.SK_PICK_UP -> {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    AID target = null;
                    try {
                        target = DFInteraction.SearchInDFByType(Constants.DFSERVICE_TRANSPORT, myAgent)[0].getName();
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                    msg.addReceiver(target);
                    myAgent.addBehaviour(new ReqTransportInit(myAgent, msg));
                }
                case Constants.SK_GLUE_TYPE_A -> {
                    ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
                    DFAgentDescription[] target = null;
                    try {
                        target = DFInteraction.SearchInDFByType(Constants.DFSERVICE_RESOURCE, myAgent);
                        for (int i = 0; i < target.length; i++){
                            msgCFP.addReceiver(target[i].getName());
                            myAgent.addBehaviour(new CFPInit(myAgent, msgCFP));
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                    if (CFPflag) {
                        System.out.println("All negotiation done");
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.addReceiver(resouceCFPId);
                        myAgent.addBehaviour(new ReqResourceInit(myAgent, msg));
                        resouceCFPId = null;
                        CFPflag = false;
                        FIPAflag = true;
                    }
                }
            }
            System.out.println("FLAG: " + FIPAflag);
            // End of the FIPA request
            if (FIPAflag) {
                System.out.println("SB: " + currState + " - step: " + currPos);
                finished = true;
            }
        }

        @Override
        public boolean done() {
            FIPAflag = false;
            currPos++;
            return finished;
        }
    }
    */

    private class ReqTransportInit extends AchieveREInitiator{

        public ReqTransportInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            //super.handleAgree(agree);
            System.out.println(myAgent.getLocalName() + ": AGREE transport msg received.");
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            //super.handleInform(inform);
            System.out.println(myAgent.getLocalName() + ": INFORM transport REQ msg received.");
            FIPAflag = true;
        }
    }

    private class ReqResourceInit extends AchieveREInitiator{

        public ReqResourceInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            super.handleAgree(agree);
            System.out.println(myAgent.getLocalName() + ": AGREE msg received.");
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            super.handleInform(inform);
            System.out.println(myAgent.getLocalName() + ": INFORM msg received.");
        }
    }

    private class CFPInit extends ContractNetInitiator {

        public CFPInit(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            //super.handleInform(inform);
            System.out.println(myAgent.getLocalName() + ": INFORM CFP.");
            CFPflag = true;
            resouceCFPId = inform.getSender();
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            //super.handleAllResponses(responses, acceptances);
            System.out.println(myAgent.getLocalName() + ": ALL PROPOSAL received.");
            boolean flag = false;

            for (int i = 0; i < responses.size(); i++){
                ACLMessage msg = (ACLMessage) responses.get(i);
                ACLMessage reply = msg.createReply();
                // FOR TESTING ONLY
                if (executionPlan.get(currPos).equals(msg.toString()) && !flag){
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    flag = true;
                } else {
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                }
                acceptances.add(i, reply);
            }
        }
    }

    @Override
    protected void takeDown() {
        super.takeDown(); //To change body of generated methods, choose Tools | Templates.
    }

    private ArrayList<String> getExecutionList(String productType){
        return switch (productType) {
            case "A" -> Utilities.Constants.PROD_A;
            case "B" -> Utilities.Constants.PROD_B;
            case "C" -> Utilities.Constants.PROD_C;
            default -> null;
        };
    }

}
