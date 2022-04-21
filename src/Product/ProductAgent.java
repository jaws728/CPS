package Product;

import Utilities.DFInteraction;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;

import java.util.ArrayList;
import java.util.Vector;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class ProductAgent extends Agent {

    String id;
    ArrayList<String> executionPlan = new ArrayList<>();
    // String nextPos;
    // String currPos;
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
        FSMBehaviour fsmb = new FSMBehaviour();
        FSMachine(fsmb);
        this.addBehaviour(fsmb);
        this.addBehaviour(new GetNextTask());

        //2. FIPA
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("responder", false));
        this.addBehaviour(new ReqTransportInit(this, msg));
        this.addBehaviour(new ReqResourceInit(this, msg));
        ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
        msg.addReceiver(new AID("responder", false));
        this.addBehaviour(new CFPInit(this, msgCFP));

        //3. Register Agent
        //DFInteraction.RegisterInDF(this, this.id);
    }

    private void FSMachine(FSMBehaviour fsmb){
        int last = this.executionPlan.size() - 1;
        // Registering states
        fsmb.registerFirstState(new SimpleState(this, this.executionPlan.get(0)),"S1");
        for (int i = 1; i < last - 1; i++) {
            fsmb.registerState(new SimpleState(this, this.executionPlan.get(i)),"S"+(i+1));
        }
        fsmb.registerLastState(new SimpleState(this, this.executionPlan.get(last)),"S"+(last+1));

        // Registering Transitions
        for (int i = 1; i < last; i++){
            fsmb.registerTransition("S"+i, "S"+(i+1), i - 1);
        }
    }

    private class SimpleState extends SimpleBehaviour {

        private boolean finished = false;
        int step = 0;
        String currState;

        public SimpleState(Agent a, String currState){
            super(a);
            this.currState = currState;
        }

        @Override
        public void action() {
            System.out.println("SB: " + currState + " - step: " + ++step);
            // TODO
            if (step == 4)
                finished = true;
        }

        @Override
        public boolean done() {
            return finished;
        }

        @Override
        public int onEnd() {
            /*
            switch (currState){
                case 'A': return 0;
                case 'B': return 1;
                case 'C': return 2;
            }*/
            // TODO
            System.out.println("On end of a state " + step);
            return -1;
        }
    }

    private class GetNextTask extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("Getting next task...");
            // TODO
        }
    }

    private class ReqTransportInit extends AchieveREInitiator{

        public ReqTransportInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            super.handleAgree(agree);
            System.out.println(myAgent.getLocalName() + ": AGREE msg received.");
            // TODO
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            super.handleInform(inform);
            System.out.println(myAgent.getLocalName() + ": INFORM msg received.");
            // TODO
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
            // TODO
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            super.handleInform(inform);
            System.out.println(myAgent.getLocalName() + ": INFORM msg received.");
            // TODO
        }
    }

    private class CFPInit extends ContractNetInitiator {

        public CFPInit(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            super.handleInform(inform);
            System.out.println(myAgent.getLocalName() + ": INFORM CFP.");
            // TODO
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            super.handleAllResponses(responses, acceptances);
            System.out.println(myAgent.getLocalName() + ": ALL PROPOSAL received.");
            ACLMessage msg = (ACLMessage) responses.get(0);
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            acceptances.add(reply);
            // TODO
        }
    }

    @Override
    protected void takeDown() {
        super.takeDown(); //To change body of generated methods, choose Tools | Templates.
    }

    private ArrayList<String> getExecutionList(String productType){
        switch(productType){
            case "A": return Utilities.Constants.PROD_A;
            case "B": return Utilities.Constants.PROD_B;
            case "C": return Utilities.Constants.PROD_C;
        }
        return null;
    }

}
