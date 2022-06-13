package Resource;

import java.io.File;
import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
//import java.io.File;
//import org.nd4j.linalg.io.ClassPathResource;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class InspectionModel {

    private MultiLayerNetwork model;

    // TODO: Initialize the model in the constructor using loadModel method
    public InspectionModel(String modelPath) {
        this.model = loadModel(modelPath);
    }

    // TODO: Load the sequential model using KerasModelImport
    // Relevant classes: MultiLayerNetwork, KerasModelImport
    public MultiLayerNetwork loadModel(String filepath) {
        MultiLayerNetwork model = null;
        //String simpleMlp = new ClassPathResource(filepath).getFile().getPath();
        try {
            // filepath needs to be absolute
            //String fullModel = new ClassPathResource(filepath).getFile().getPath();
            //final String fullModel = new File(filepath).getAbsolutePath();
            //model = KerasModelImport.importKerasSequentialModelAndWeights(fullModel);
            model = KerasModelImport.importKerasSequentialModelAndWeights(filepath);
        } catch (IOException | UnsupportedKerasConfigurationException | InvalidKerasConfigurationException e) {
            e.printStackTrace();
        }
        return model;
    }

    // TODO: Load the image and return the corresponding array. The input shape should match the one from training
    // Relevant classes: NativeImageLoader, INDArray
    public INDArray loadImage(String filepath, int height, int width, int channels) {
        INDArray image = null;
        try {
            // Load the image file
            File f = new File(filepath);

            //Use the NativeImageLoader to convert to a numerical matrix
            NativeImageLoader loader = new NativeImageLoader(height, width, channels);

            //Load the image into an INDArray
            image = loader.asMatrix(f);

            // Conversion from NCHW to NHWC
            // https://github.com/eclipse/deeplearning4j/issues/8975
            image = image.permute(0, 2, 3, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    // TODO: Classify the image by feeding the array from the loadImage method to the model
    // Relevant classes: INDArray, Nd4j
    public int predict(INDArray imageInput) {
        int pred = -1;
        //INDArray input = Nd4j.create(DataType.FLOAT, 256, 100);
        INDArray output = model.output(imageInput);
        pred = (int) output.maxNumber().intValue();
        return pred;
    }

}
