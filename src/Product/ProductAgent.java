package Product;

import Utilities.Constants;
import Utilities.DFInteraction;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
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
    boolean endCFP = false;
    boolean CFPFlag = false;
    private int currState = -1;
    private AID resouceId = null;
    private String origin = null;
    private String destin = null;
    private boolean tranpFailed = false;

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        this.id = (String) args[0];
        this.executionPlan = this.getExecutionList((String) args[1]);
        System.out.println("Product launched: " + this.id + " Requires: " + executionPlan);
        this.destin = this.executionPlan.get(0);

        // Necessary behaviour/s to control the flow of its own production
        this.addBehaviour(new GetNextTask());
    }

    // Get the next state - if there is no next task - put planEnded to true
    private class GetNextTask extends OneShotBehaviour {
        @Override
        public void action() {
            resouceId = null;
            if (currState < executionPlan.size() - 1) {
                currState++;
                System.out.println("Next task get: " + executionPlan.get(currState));
                // The AGV is started on source - it will put product if the pickup skill is executed
                addBehaviour(new SendCFP());
            } else {
                System.out.println("Ended all tasks");
            }
        }
    }

    // TODO: 2CFPs in skB and qc + transport before drop + source location - CFP before all resources

    // Sending CFP requests to all resources
    private class SendCFP extends SimpleBehaviour {
        private boolean finished = false;
        private boolean firstEnter = true;

        @Override
        public void action() {
            if (firstEnter) {
                ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
                try {
                    DFAgentDescription[] target = DFInteraction.SearchInDFByName(executionPlan.get(currState), myAgent);
                    for (DFAgentDescription t : target) {
                        msgCFP.addReceiver(t.getName());
                    }
                    addBehaviour(new CFPInit(myAgent, msgCFP));
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
                firstEnter = false;
            } else {
                if (endCFP) {
                    if (!CFPFlag) {
                        // send CFP again
                        endCFP = false;
                        firstEnter = true;
                        System.out.println("Sending CFP again...");
                    } else {
                        if (resouceId != null) {
                            // INFORM received
                            System.out.println("All CFP HANDLED.");
                            addBehaviour(new AGVControl());
                            CFPFlag = false;
                            endCFP = false;
                            finished = true;
                        }
                    }
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
                if (!Objects.equals(origin, destin) && origin != null) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent(origin + ":" + destin + ":" + id);
                    try {
                        AID target = DFInteraction.SearchInDFByType(Constants.DFSERVICE_TRANSPORT, myAgent)[0].getName();
                        msg.addReceiver(target);
                        addBehaviour(new ReqAGVInit(myAgent, msg));
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                } else {
                    origin = destin;
                }
                firstEnter = false;
            } else {
                if (Objects.equals(origin, destin)){
                    // In station: execute skill
                    addBehaviour(new ExecuteSkill());
                    finished = true;
                }
                if (tranpFailed) {
                    tranpFailed = false;
                    firstEnter = true;
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
                // Request SKILL execution
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(resouceId);
                msg.setContent(executionPlan.get(currState));
                addBehaviour(new ReqResourceInit(myAgent, msg));
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
            origin = destin;
        }

        @Override
        protected void handleFailure(ACLMessage failure) {
            System.out.println(myAgent.getLocalName() + ": Transportation FAILED...");
            tranpFailed = true;
        }
    }

    private class ReqResourceInit extends AchieveREInitiator{

        public ReqResourceInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            System.out.println(myAgent.getLocalName() + ": AGREE resource msg received.");
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            System.out.println(myAgent.getLocalName() + ": INFORM resource - " + inform.getContent());
            if (inform.getContent() != null) {
                if ("NOK".equalsIgnoreCase(inform.getContent())) {
                    //currState = 0;
                    int i = 0;
                    for (String ex: executionPlan) {
                        if (ex.contains("g")) {
                            break;
                        }
                        i++;
                        //if (ex.equals(Constants.SK_GLUE_TYPE_A) || ex.equals(Constants.SK_GLUE_TYPE_B) || ex.equals(Constants.SK_GLUE_TYPE_C)) {
                        //    break;
                        //}
                    }
                    currState = i - 1;
                }
            }
            resouceId = null;
        }

        @Override
        protected void handleFailure(ACLMessage failure) {
            System.out.println(myAgent.getLocalName() + ": FAILED...");
            currState -= 1;
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
            destin = inform.getContent();
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            for (int i = 0; i < responses.size(); i++){
                ACLMessage msg = (ACLMessage) responses.get(i);

                if (msg.getPerformative() != ACLMessage.REFUSE) {
                    ACLMessage reply = msg.createReply();
                    if (!CFPFlag) {
                        String[] str = msg.getContent().split(":");
                        for (String s : str) {
                            if (executionPlan.get(currState).equals(s)) {
                                System.out.println("PROPOSAL: " + s);
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                CFPFlag = true;
                                break;
                            }
                        }
                    }
                    if (reply.getPerformative() != ACLMessage.ACCEPT_PROPOSAL) {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    }
                    acceptances.add(i, reply);
                }
            }
            endCFP = true;
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
