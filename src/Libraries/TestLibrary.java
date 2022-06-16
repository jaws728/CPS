package Libraries;

import Resource.InspectionModel;
import jade.core.Agent;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class TestLibrary implements IResource {

    private Agent myAgent;

    private final String IMG_ABS_PATH = "C:\\Users\\Public\\CPS_lab3\\images";
    @Override
    public void init(Agent myAgent) {
        this.myAgent = myAgent;
        System.out.println("Test library has been successfully initialized for agent: " + myAgent.getLocalName());
    }

    @Override
    public int executeSkill(String skillID) {
        try {
            switch (skillID) {
                case Utilities.Constants.SK_GLUE_TYPE_A: {
                    Thread.sleep(200);
                    return 1; //Math.random() > 0.5;
                }
                case Utilities.Constants.SK_GLUE_TYPE_B: {
                    Thread.sleep(300);
                    return 1; //Math.random() > 0.5;
                }
                case Utilities.Constants.SK_GLUE_TYPE_C: {
                    Thread.sleep(400);
                    return 1;
                }
                case Utilities.Constants.SK_PICK_UP:
                    Thread.sleep(1000);
                    return 1;
                case Utilities.Constants.SK_DROP:
                    Thread.sleep(1000);
                    return 1;
                case Utilities.Constants.SK_QUALITY_CHECK:
                    Thread.sleep(200);
                    String quality = "";
                    switch(this.myAgent.getLocalName()){
                        case "QualityControlStation1":
                            quality = qualityCheck(IMG_ABS_PATH + "\\QualityControlStation1.jpg");
                            break;
                        case "QualityControlStation2":
                            quality = qualityCheck(IMG_ABS_PATH + "\\QualityControlStation2.jpg");
                            break;
                    }
                    if ("NOK".equalsIgnoreCase(quality)) {
                        return 0; //redo
                    }
                    return 1;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(TestLibrary.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    private String qualityCheck(String path) {
        final String MODEL_PATH = "C:\\Users\\Public\\CPS_lab3\\src\\Resource\\lab3_model1.h5";
        InspectionModel inspectionModel = new InspectionModel(MODEL_PATH);
        INDArray input = inspectionModel.loadImage(path, 512, 512, 3);
        int pred = inspectionModel.predict(input);
        return  (pred == 1 ? "OK" : "NOK");
    }


//    @Override
//    public boolean launchProduct(String productID) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public boolean finishProduct(String productID) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

    @Override
    public String[] getSkills() {
        String[] skills;
        switch (myAgent.getLocalName()) {
            case "GlueStation1":
                skills = new String[2];
                skills[0] = Utilities.Constants.SK_GLUE_TYPE_A;
                skills[1] = Utilities.Constants.SK_GLUE_TYPE_B;
                return skills;
            case "GlueStation2":
                skills = new String[2];
                skills[0] = Utilities.Constants.SK_GLUE_TYPE_A;
                skills[1] = Utilities.Constants.SK_GLUE_TYPE_C;
                return skills;
            case "QualityControlStation1":
                skills = new String[1];
                skills[0] = Utilities.Constants.SK_QUALITY_CHECK;
                return skills;
            case "QualityControlStation2":
                skills = new String[1];
                skills[0] = Utilities.Constants.SK_QUALITY_CHECK;
                return skills;
            case "Source":
                skills = new String[2];
                skills[0] = Utilities.Constants.SK_PICK_UP;
                skills[1] = Utilities.Constants.SK_DROP;
                return skills;
        }
        return null;
    }
}
