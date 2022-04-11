package Product;

import jade.core.Agent;
import java.util.ArrayList;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class ProductAgent extends Agent {    
    
    String id;
    ArrayList<String> executionPlan = new ArrayList<>();
    // TO DO: Add remaining attributes required for your implementation
    
    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        this.id = (String) args[0];
        this.executionPlan = this.getExecutionList((String) args[1]);
        System.out.println("Product launched: " + this.id + " Requires: " + executionPlan);
        
        // TO DO: Add necessary behaviour/s for the product to control the flow
        // of its own production 
        
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
