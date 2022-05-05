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
import java.util.Arrays;
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
    private int currState = -1;
    private AID resouceId = null;
    private String location = null;
    private String nextLoc = null;
    private boolean taskDone = false;

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        this.id = (String) args[0];
        this.executionPlan = this.getExecutionList((String) args[1]);
        System.out.println("Product launched: " + this.id + " Requires: " + executionPlan);
        this.nextLoc = this.executionPlan.get(0);

        // Necessary behaviour/s to control the flow of its own production
        // this.addBehaviour(new HandlePlan());
        this.addBehaviour(new GetNextTask());
    }

    private class InitPlan extends OneShotBehaviour {
        @Override
        public void action() {
            if (currState == 0 && Objects.equals(executionPlan.get(currState), Constants.SK_PICK_UP)) {
                addBehaviour(new AGVControl());
            }
        }
    }

    // Get the next state - if there is no next task - put planEnded to true
    private class GetNextTask extends OneShotBehaviour {
        @Override
        public void action() {
            if (currState < executionPlan.size() - 1) {
                currState++;
                System.out.println("Next task get: " + executionPlan.get(currState));
            } else {
                System.out.println("Ended all tasks");
            }

            resouceId = null;

            if (currState == 0 || currState == executionPlan.size() - 1) {
                addBehaviour(new AGVControl());
            } else {
                addBehaviour(new SendCFP());
            }
        }
    }

    // Sending CFP requests to all resources
    private class SendCFP extends SimpleBehaviour {
        private boolean finished = false;
        private boolean firstEnter = true;

        @Override
        public void action() {
            if (firstEnter) {
                ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
                try {
                    // DFAgentDescription[] target = DFInteraction.SearchInDFByType(Constants.DFSERVICE_RESOURCE, myAgent);
                    DFAgentDescription[] target = DFInteraction.SearchInDFByName(executionPlan.get(currState), myAgent);
                    for (DFAgentDescription t : target) {
                        msgCFP.addReceiver(t.getName());
                        addBehaviour(new CFPInit(myAgent, msgCFP));
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
                firstEnter = false;
            } else {
                if (resouceId != null){
                    System.out.println("All CFP HANDLED.");
                    addBehaviour(new AGVControl());
                    finished = true;
                }
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }

    private class AGVControl extends SimpleBehaviour {
        private boolean finished = false;
        private boolean firstEnter = true;

        @Override
        public void action() {
            if (firstEnter) {
                // Outside of station: Request AGV move
                if (!Objects.equals(location, nextLoc)) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    if (resouceId == null)  {
                        // no CFP done - it is from entry point or exit point
                        msg.setContent("Operation");
                        try {
                            // Get Operation AID
                            if (currState == 0){
                                resouceId = DFInteraction.SearchInDFByName(Constants.SK_PICK_UP, myAgent)[0].getName();
                            }
                            else {
                                resouceId = DFInteraction.SearchInDFByName(Constants.SK_DROP, myAgent)[0].getName();
                            }
                        } catch (FIPAException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        AID target = DFInteraction.SearchInDFByType(Constants.DFSERVICE_TRANSPORT, myAgent)[0].getName();
                        msg.addReceiver(target);
                        addBehaviour(new ReqAGVInit(myAgent, msg));
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
                firstEnter = false;
            } else {
                if (Objects.equals(location, nextLoc)){
                    // In station: execute skill
                    addBehaviour(new ExecuteSkill());
                    finished = true;
                }
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
    private class ExecuteSkill extends SimpleBehaviour {
        private boolean finished = false;
        private boolean firstEnter = true;

        @Override
        public void action() {
            if (firstEnter) {
                if (Objects.equals(location, nextLoc) && resouceId != null) {
                    // Request SKILL execution
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(resouceId);
                    addBehaviour(new ReqResourceInit(myAgent, msg));
                }
                firstEnter = false;
            } else {
                if (resouceId == null) {
                    addBehaviour(new GetNextTask());
                    finished = true;
                }
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }

    private class ReqAGVInit extends AchieveREInitiator{

        public ReqAGVInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            System.out.println(myAgent.getLocalName() + ": AGREE transport msg received.");
        }

        @Override
        protected void handleInform(ACLMessage inform) {
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
            System.out.println(myAgent.getLocalName() + ": AGREE msg received.");
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            System.out.println(myAgent.getLocalName() + ": INFORM - " + inform.getContent());
            //addBehaviour(new GetNextTask());
            resouceId = null;
        }
    }

    private class CFPInit extends ContractNetInitiator {
        public CFPInit(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            System.out.println(myAgent.getLocalName() + ": INFORM CFP.");
            resouceId = inform.getSender();
            nextLoc = inform.getContent();
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            for (int i = 0; i < responses.size(); i++){
                ACLMessage msg = (ACLMessage) responses.get(i);
                ACLMessage reply = msg.createReply();
                String[] str = msg.getContent().split(":");
                if (!CFPflag) {
                    for (String s: str) {
                        if (executionPlan.get(currState).equals(s) && !CFPflag){
                            System.out.println("PROPOSAL: " + s);
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            CFPflag = true;
                            break;
                        } else {
                            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        }
                    }
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
