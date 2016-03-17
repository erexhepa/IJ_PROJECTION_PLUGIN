package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Created by rexhepaj on 17/03/16.
 */
public class SME_ENS_Sml {

    private ImagePlus imagePlus = null;

    public SME_ENS_Sml(ImagePlus imgStack){
        imagePlus = imgStack;
    }

    /**
     * Applying the sML filter on the Original ImageStack
     *
     */
    public void applyFilter() {

        float[] kernel1_X = {0, 0, 0, -1, 2, -1, 0, 0, 0}; // Kernel for the X axis
        float[] kernel2_Y = {0, -1, 0, 0, 2, 0, 0, -1, 0}; // Kernel for the Y axis

        ImageStack stack1 = imagePlus.getStack();                    // ImagePlus into ImageStack
        int W = imagePlus.getWidth();                      // Get the image width
        int H = imagePlus.getHeight();                     // Get the image height
        int i, j, slice;                            // Define the type of i, j and slice (equivalent to z axis)
        int size_ = stack1.getSize();               // Size of the stack image

        for (slice = 1; slice <= size_; slice++) {  //Go through each slice

            // Work on the duplicated images.
            ImageProcessor ip_copy1_X = imagePlus.getProcessor().duplicate();
            ImageProcessor ip_copy1_Y = imagePlus.getProcessor().duplicate();

            ImageProcessor ip = stack1.getProcessor(slice);                     // Returns an ImageProcessor for the specified slice
            ImageProcessor ip_sum = ip.createProcessor(W, H);    //Create an empty ImageProcessor

            // Apply the convolution on the duplicated ImageProcessor
            ip_copy1_X.convolve(kernel1_X, 3, 3);                // Make the convolution on the X axis
            ip_copy1_X.abs();                                    // absolute value of each pixel
            ip_copy1_Y.convolve(kernel2_Y, 3, 3);                // Make the convolution on the Y axis
            ip_copy1_Y.abs();                                    // absolute value of each pixel

            for (i = 0; i < W; ++i) {
                for (j = 0; j < H; ++j) {
                    int a = ip_copy1_X.get(i, j);                // get the pixel value in the resultant image after convolution (X axis)
                    int b = ip_copy1_Y.get(i, j);                // get the pixel value in the resultant image after convolution (Y axis)
                    int sum = a + b;                             // add the 2 pixel values
                    ip_sum.putPixel(i, j, sum);                  // put the result of the addition in the newly created ImageProcessor
                }
            }
            stack1.setPixels(ip_sum.getPixels(), slice);         // Assigns a pixel array to the specified slice
        }

        //Image display in new window
        //this.imp2 = new ImagePlus("sML_" + imp.getTitle(), stack1);
        //imp2.setStack(stack1, 1, size_, 1);
        //imp2.setCalibration(imp2.getCalibration());
        //imp2.show();
    }

    public ImageStack getFilteredImg(){
        return imagePlus.getImageStack();
    }
}
