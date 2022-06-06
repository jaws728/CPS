/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Libraries;

import Utilities.Constants;
//import Utilities.MultipartRequestUtility;
import coppelia.CharWA;
import coppelia.IntW;
import coppelia.remoteApi;
import jade.core.Agent;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.util.List;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class SimResourceLibrary implements IResource {

    public remoteApi sim;
    public int clientID = -1;
    Agent myAgent;
    final long timeout = 30000;

    @Override
    public void init(Agent a) {
        this.myAgent = a;
        if(sim == null) sim = new remoteApi();
        sim = new remoteApi();
        int port = 0;
        switch(myAgent.getLocalName()){
            case "GlueStation1": port=19997; break;
            case "GlueStation2": port=19998; break;
            case "QualityControlStation1": port=19999; break;
            case "QualityControlStation2": port=20000; break;
            case "Operator": port=20001; break;
        }
        clientID = sim.simxStart("127.0.0.1", port, true, true, 5000, 5);
        if (clientID != -1) {
            System.out.println(this.myAgent.getAID().getLocalName() + " initialized communication with the simulation.");
        }
    }

    @Override
    public boolean executeSkill(String skillID) {
        sim.simxSetStringSignal(clientID, myAgent.getLocalName(), new CharWA(skillID), sim.simx_opmode_blocking);
        IntW opRes = new IntW(-1);
        long startTime = System.currentTimeMillis();
        while ((opRes.getValue() != 1) && (System.currentTimeMillis() - startTime < timeout)) {
            sim.simxGetIntegerSignal(clientID, myAgent.getLocalName(), opRes, sim.simx_opmode_blocking);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimResourceLibrary.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        sim.simxClearIntegerSignal(clientID, myAgent.getLocalName(), sim.simx_opmode_blocking);

        if(skillID.equalsIgnoreCase(Constants.SK_QUALITY_CHECK)) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            switch(this.myAgent.getLocalName()){
                // TO DO: example: use an instance of the InspectionModel class for each station case to classify the product image on
                // the corresponding image path. The simulation should store images in the images folder with the name of the station + .jpg.
                // e.g. "images/QualityControlStation1.jpg"
				// This can then be used to adapt the control logic based on the inspection result.
                case "QualityControlStation1": /*TBD*/ break;
                case "QualityControlStation2": /*TBD*/ break;
            }
        }

        if (opRes.getValue() == 1) {
            return true;
        }
        return false;
    }

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
            case "Operator":
                skills = new String[2];
                skills[0] = Utilities.Constants.SK_PICK_UP;
                skills[1] = Utilities.Constants.SK_DROP;
                return skills;
        }
        return null;
    }

}
