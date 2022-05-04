package Product;

import Utilities.Constants;
import Utilities.DFInteraction;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Vector;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class ProductAgent extends Agent {

    String id;
    ArrayList<String> executionPlan = new ArrayList<>();
    private boolean CFPflag = false;
    private int currState = 0;
    private AID resouceId = null;
    private String location = null;
    private String nextLoc = null;

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        this.id = (String) args[0];
        this.executionPlan = this.getExecutionList((String) args[1]);
        System.out.println("Product launched: " + this.id + " Requires: " + executionPlan);
        this.nextLoc = this.executionPlan.get(0);

        // Necessary behaviour/s to control the flow of its own production
        this.addBehaviour(new HandlePlan());
    }

    // Start and Exit of the product plan
    private class HandlePlan extends SimpleBehaviour {
        private boolean finished = false;

        @Override
        public void action() {
            // Entry/Exit Operator: move the AGV to operation (resource) - get or drop product
            if (Objects.equals(executionPlan.get(currState), Constants.SK_PICK_UP)) {
                // entry point: source
                addBehaviour(new AGVControl());
            } else if (Objects.equals(executionPlan.get(currState), Constants.SK_DROP)){
                // exit point: sink
                finished = true;
            } else {
                // Not entry neither exit - normal states
                if (nextLoc == null) {
                    //end of get next state
                    addBehaviour(new SendCFP());
                }
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }

    // Get the next state - if there is no next task - put planEnded to true
    private class GetNextTask extends OneShotBehaviour {
        @Override
        public void action() {
            if (currState < executionPlan.size()) {
                currState++;
                System.out.println("Next task get: " + executionPlan.get(currState));
            } else {
                System.out.println("Ended all tasks");
            }
            nextLoc = null;
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
            if (CFPflag){
                System.out.println("##");
            }
        }
    }

    private class AGVControl extends OneShotBehaviour {
        // 1-entry, 2-exit
        private int isOp;

        private AGVControl() {
            this.isOp = -1;
        }

        private AGVControl(int isOp) {
            this.isOp = isOp;
        }

        @Override
        public void action() {
            // From entry point
            if (currState == 0 && location == null && resouceId == null) {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Operation");
                try {
                    AID target = DFInteraction.SearchInDFByName(Constants.SK_PICK_UP, myAgent)[0].getName();
                    msg.addReceiver(target);
                    addBehaviour(new ReqAGVInit(myAgent, msg));
                    resouceId = target;
                    System.out.println(target.toString());
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }

            // Get positions: now and next station
            if (!Objects.equals(location, nextLoc)) {
                // Request AGV move
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                try {
                    AID target = DFInteraction.SearchInDFByType(Constants.DFSERVICE_TRANSPORT, myAgent)[0].getName();
                    msg.addReceiver(target);
                    addBehaviour(new ReqAGVInit(myAgent, msg));
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }

            if (Objects.equals(location, nextLoc) && resouceId != null) {
                addBehaviour(new ExecuteSkill());
            }
        }
    }
    private class ExecuteSkill extends OneShotBehaviour {

        @Override
        public void action() {
            // FLAG: to indicate the move is finished
            if (location.equals(nextLoc)) {
                // Request SKILL execution
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(resouceId);
                addBehaviour(new ReqResourceInit(myAgent, msg));
            }
        }
    }

    private class ReqAGVInit extends AchieveREInitiator{

        public ReqAGVInit(Agent a, ACLMessage msg) {
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
            location = nextLoc;
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
            System.out.println(myAgent.getLocalName() + ": INFORM - " + inform.getContent());
            addBehaviour(new GetNextTask());
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
            resouceId = inform.getSender();
            nextLoc = inform.getContent();
            CFPflag = true;
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            //super.handleAllResponses(responses, acceptances);
            System.out.println(myAgent.getLocalName() + ": ALL PROPOSAL received.");
            boolean acceptFlag = false;

            for (int i = 0; i < responses.size(); i++){
                ACLMessage msg = (ACLMessage) responses.get(i);
                System.out.println("PROPOSAL: " + msg.toString());
                ACLMessage reply = msg.createReply();
                // FOR TESTING ONLY
                if (executionPlan.get(currState).equals(msg.toString()) && !acceptFlag){
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    acceptFlag = true;
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
