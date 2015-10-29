package ij.plugin.filter;
import ij.*;
import ij.process.*;

/*
When creating a new plugin and in order to debug it in ImageJ
it has to be added in the filter IJ_Props.txt at the right place
that is to say in the "filters" in our case
*/

/* This plugin needs a "setup part" and a "run" part (cf.PlugInFilter) which are compulsory
so that it can work in ImageJ
 */

/*To add OpenCv as an external library we have to go in File>ProjectStructure and
add a new library which is OpenCV in our case But not used in this plugin*/


public class Niki_Plugin implements PlugInFilter {

    public ImagePlus imp;
    public ImageStack stack;
    float[] kernel1_X = {0, 0, 0, -1, 2, -1, 0, 0, 0}; // Kernel for the X axis
    float[] kernel2_Y = {0, -1, 0, 0, 2, 0, 0, -1, 0}; // Kernel for the Y axis


    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL + STACK_REQUIRED; // Works for stack images
    }


    public void run(ImageProcessor ip) {
        stack = imp.getStack();                  // ImagePlus into ImageStack
        ImageStack stack1 = stack.duplicate();   // Duplicates the original stack image
        ImageStack stack2 = stack.duplicate();   // Duplicates the original stack image
        ImageStack stack3 = stack.duplicate();   // Duplicates the original stack image
        ImageStack stack4 = stack.duplicate();   // Duplicates the original stack image
        ImageStack stack5 = stack.duplicate();   // Duplicates the original stack image

        System.out.println("Original Image duplicates Done !");

        sML(stack1);
        Kmeans_(3, FFT_1D_(stack1));
        ImageStack Blur1 = Create_Gaussian_Image(stack2, 5, 5);
        ImageStack Blur2 = Create_Gaussian_Image(stack3, 10, 10);
        ImageStack Blur3 = Create_Gaussian_Image(stack4, 15, 15);
        Gaussian_Filter_(stack5, Blur1, Blur2, Blur3);
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    public ImageStack sML(ImageStack stack1) {

        ImageProcessor ip = imp.getProcessor();
        stack1 = imp.getStack();                    // ImagePlus into ImageStack
        int W = ip.getWidth();                      // Get the image width
        int H = ip.getHeight();                     // Get the image height
        int i, j, slice;                            // Define the type of i, j and slice (equivalent to z axis)
        int size_ = stack1.getSize();               // Size of the stack image

        for (slice = 1; slice <= size_; slice++) {  //Go through each slice

            // Work on the duplicated images.
            ImageProcessor ip_copy1_X = ip.duplicate();
            ImageProcessor ip_copy1_Y = ip.duplicate();

            ip = stack1.getProcessor(slice);                     // Returns an ImageProcessor for the specified slice
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
        imp.setStack(stack1, 1, size_, 1);                       // Conversion of the ImageStack into ImagePLus in order to display it
        imp.updateAndDraw();                                     // Update the image

        System.out.println("sML Done !");
        return stack1;
    }
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    public float[][] FFT_1D_(ImageStack stack1) {

        int x_coord, y_coord, z_coord, z_fft, i, j, k, l;
        int size1_ = stack.getSize();                   // Size of the stack image
        int depth1_ = stack.getBitDepth();
        ImageProcessor ip_FFT = stack.getProcessor(1);  // Done just to have W and H of one image
        ImageProcessor ip = imp.getProcessor();
        ImageProcessor ip_zero = ip.duplicate();

        int W = ip_FFT.getWidth();                      // Get the image width
        int H = ip_FFT.getHeight();                     // Get the image height


        // Make the padding for number of stacks that are not power of 2
        if (size1_ % 2 != 0) {

            // Find the closest power of 2 in order to find the number of stacks with 0 to add to the original stack
            double result_ = Math.ceil(Math.log(size1_) / Math.log(2.0d));
            double padding_zero = Math.pow(2.0, result_);
            double number_of_zero = padding_zero - size1_; //Number of stacks to add

            // Create ImageProcessor containing only pixels which value is 0
            for (i = 0; i < W; i++) {
                for (j = 0; j < H; j++) {
                    ip_zero.putPixel(i, j, 0);
                }
            }

            // Add the missing slices to the stack image so that the number of slice is a power of 2
            for (k = 1; k <= (int) number_of_zero; k++) {
                stack.addSlice(ip_zero);
            }

            // Size of the stack image after doing the padding with 0
            int size2_ = stack.getSize();
            int depth2_ = stack.getBitDepth();

            // Set the pixel values
            for (l = size1_ + 1; l < size2_; l++) {
                stack.setPixels(ip_zero.getPixels(), l);
            }
            System.out.println("Padding Done !");

            // Get the right input data for the FFT function
            Complex[] z = new Complex[size2_];                              //Creates a vector type Complex named z and of size = stack size after padding
            Complex[] z_fft_value;
            float abs_array[] = new float[size2_];                          // Create an empty array of size "size2_"
            float final_FFT_result_[][] = new float[W * H][size2_];         // Create a new empty vector to put the final result of FFT

            for (x_coord = 0; x_coord < W; x_coord++) {                     //Go through x coordinates
                for (y_coord = 0; y_coord < H; y_coord++) {                 //Go through y coordinates
                    for (z_coord = 0; z_coord < size2_; z_coord++) {        //Go through each slice (z coordinates)
                        z[z_coord] = new Complex(stack.getVoxel(x_coord, y_coord, z_coord), 0);
                    }

                    // Apply FFT on the pixels having the same x,y coordinates and put results in z_fft_value
                    z_fft_value = Filter_FFT_.fft(z);

                    //Calculate absolute value
                    for (z_fft = 0; z_fft < size2_; z_fft++) {
                        float absolute_ = (float) Math.sqrt((Math.pow(z_fft_value[z_fft].re(), 2.0)) + (Math.pow(z_fft_value[z_fft].im(), 2.0)));
                        abs_array[z_fft] = absolute_;
                    }

                    // Find the max value for normalization
                    float largest = abs_array[0];
                    for (i = 1; i < abs_array.length; i++) {
                        if (abs_array[i] > largest)
                            largest = abs_array[i];
                    }
//                    System.out.println("Max value : ");System.out.println(largest);

                    // Normalization for same (x,y) coordinates and changing z
                    float normalized_values[] = new float[size2_];
                    for (i = 0; i < abs_array.length; i++) {
                        normalized_values[i] = abs_array[i] / largest;
//                        System.out.println(normalized_values[i]);
                    }

                    //Add the created array in a new matrix with a width size of z and a height size of W*H
                    int k_ = y_coord * W + x_coord;
                    for (z_coord = 0; z_coord < size2_; z_coord++) {
                        final_FFT_result_[k_][z_coord] = normalized_values[z_coord];
                    }
                }
            }
            System.out.println("FFT Done for N not power of 2  ! Yeahhh :) !");
            return final_FFT_result_;
        }

        // Get the right input data for the FFT function
        Complex[] z = new Complex[size1_];
        Complex[] z_fft_value;
        float abs_array[] = new float[size1_];
        float final_FFT_result_[][] = new float[W * H][size1_];

        for (x_coord = 0; x_coord < W; x_coord++) {              //Go through x coordinates
            for (y_coord = 0; y_coord < H; y_coord++) {          //Go through y coordinates
                for (z_coord = 0; z_coord < size1_; z_coord++) { //Go through each slice (z coordinates)
                    z[z_coord] = new Complex(stack.getVoxel(x_coord, y_coord, z_coord), 0);
                }

                // Apply FFT on the pixels having the same x,y coordinates and put results in z_fft_value
                z_fft_value = Filter_FFT_.fft(z);

                //Calculate absolute value
                for (z_fft = 0; z_fft < size1_; z_fft++) {
                    float absolute_ = (float) Math.sqrt((Math.pow(z_fft_value[z_fft].re(), 2.0)) + (Math.pow(z_fft_value[z_fft].im(), 2.0)));
                    abs_array[z_fft] = absolute_;
                }

                // Find the max value for normalization
                float largest = abs_array[0];
                for (i = 1; i < abs_array.length; i++) {
                    if (abs_array[i] > largest)
                        largest = abs_array[i];
                }

                // Normalization for same (x,y) coordinates and changing z
                float normalized_values[] = new float[size1_];
                for (i = 0; i < abs_array.length; i++) {
                    normalized_values[i] = abs_array[i] / largest;
                }

                //Add the created array in a new matrix with a width size of z and a height size of W*H
                int k_ = y_coord * W + x_coord;
                for (z_coord = 0; z_coord < size1_; z_coord++) {
                    final_FFT_result_[k_][z_coord] = normalized_values[z_coord];
                }
            }
        }
        System.out.println("FFT Done for N power of 2  !");
        return final_FFT_result_;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    public void Kmeans_(int k_clust, float[][] result_fft)

    {

        System.out.println("Kmeans Done! ^^");

    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    public ImageStack Create_Gaussian_Image(ImageStack stack2, int sigmaX, int sigmaY) {

        int slice;                                                     // Define the type of "sliceé
        int size3_ = stack2.getSize();                                 // Size of the stack image
        ImageProcessor ip_Gauss;                                       // Work on the duplicated images.
        GaussianBlur blur = new GaussianBlur();                        // Create ImageType
        ImageStack Gauss_Stack = stack2.duplicate();                   // We duplicate the original stack image .Create new empty stack to put the pixel values obtained after blurring blur

        for (slice = 1; slice <= size3_; slice++) {                    // Go through each slice
            ip_Gauss = stack2.getProcessor(slice);                     // Returns an ImageProcessor for the specified slice using the Original stack image
            blur.blurGaussian(ip_Gauss, sigmaX, sigmaY, 0.02);         // Apply the blurring on the given slice
        }
        System.out.println("Gauss_Stack Created !");
        return Gauss_Stack;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // Apply to the highest pic (mean of the Kmean) the lowest sigma value
    public void Gaussian_Filter_(ImageStack stack2, ImageStack Gauss_Stack1, ImageStack Gauss_Stack2, ImageStack Gauss_Stack3) { // ADD THE KMEANS ARRAY RESULT AS ARGUMENT

        ImageProcessor ip = imp.getProcessor();
        int W3 = ip.getWidth();              // Get the image width
        int H3 = ip.getHeight();             // Get the image height
        int i, j, slice;        // Define the type of i, j and slice (equivalent to z axis)
        int size3_ = stack2.getSize();       // Size of the stack image

        float[][] TEST_Array = {{1, 2, 1, 3, 2, 2, 3, 1, 1}, {1, 1, 1, 1, 2, 3, 3, 1, 1}, {2, 2, 2, 2, 2, 3, 1, 1, 3}};

        //Creer une image stack vide et passer dans chaque nouvelle slice une fois la 1ere slice faite
        for (slice = 1; slice <= size3_; slice++) {
            for (i = 0; i < TEST_Array.length; i++) {
                for (j = 0; j < 9; j++) {
                    if (TEST_Array[i][j] == 1) {
                        //prendre la slice de ip_Gauss_new1
                        //prendre le pixel ayant meme x y que le pixel dans ArrayTEST
                        //remplacer valeur
                        System.out.println("1");
                    }
                    if (TEST_Array[i][j] == 2) {
                        System.out.println("2");
                    }
                    if (TEST_Array[i][j] == 3) {
                        System.out.println("3");
                    }
                }
            }
        }
        System.out.println("AGF Done !");
    }
}
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////