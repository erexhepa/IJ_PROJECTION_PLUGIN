package ij.plugin.filter;
import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import java.util.ArrayList;

/**
 When creating a new plugin and in order to debug it in ImageJ
 it has to be added in the filter IJ_Props.txt at the right place
 that is to say in the "filters" in our case
 **/

/** This plugin needs a "setup part" and a "run" part (cf.PlugInFilter) which are compulsory
 so that it can work in ImageJ
 **/

/**To add OpenCv as an external library we have to go in File>ProjectStructure and
 add a new library which is OpenCV in our case But not used in this plugin
 **/


public class Niki_Plugin implements PlugInFilter {

    public ImagePlus imp;
    public ImageStack stack;

    public double[] kmeansLabels ;
    public double[][] kmeanCentroids;

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
        showDialog(stack2, stack3, stack4);
        sML(stack1);
        Kmeans_(3, FFT_1D_(stack1));
        Gaussian_Filter_(stack5, stack2, stack3, stack4);
        Max_Proj();
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /** Applying the sML filter on the Original ImageStack
     *
     * @param stack1 : Original image stack duplicated
     * @return : returns the stack image after sML filtering is done
     */

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

    /** Apply 1D FFT on the stack image obtained after doing the sML filtering
     * Padding is done for the images that are not power of 2
     *
     * @param stack1: stack image obtained after applying sML filtering
     * @return : returns the matrix containing the 1D FFT result
     */

    public double[][] FFT_1D_(ImageStack stack1) {

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
            double final_FFT_result_ [][] = new double [W*H][size2_];         // Create a new empty vector to put the final result of FFT

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
//                    System.out.println("Max value : ");
//                    System.out.println(largest);

                    // Normalization for same (x,y) coordinates and changing z
                    float normalized_values[] = new float[size2_];
                    for (i = 0; i < abs_array.length; i++) {
                        normalized_values[i] = abs_array[i] / largest;
//                        System.out.println(normalized_values[i]);
                    }

                    //Add the created array in a new matrix with a width size of z and a height size of W*H
                    int k_=y_coord*W+x_coord;
                    for (z_coord=0;z_coord<size2_;z_coord++){
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
        double final_FFT_result_ [][] = new double [W*H][size1_];

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
                int k_=y_coord*W+x_coord;
                for (z_coord=0;z_coord<size1_;z_coord++){
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

    /** Apply the Kmeans for segmentation
     *
     * @param numClust_ : number of clusters
     * @param result_fft : matrix that contains
     * the results after applying the FFT_1D = width is equal to the number of stacks in the image
     * and length of result_fft is equal to the number of pixels of the image
     */

    public void Kmeans_(int numClust_, double [][] result_fft)

    {
        int i,j,k,m;
        float mean0 = 0;
        float mean1 = 0;
        float mean2 = 0;
        int slice_num = stack.getSize();

        Kmeans kmeans_Niki = new Kmeans(result_fft, numClust_,false);
        kmeans_Niki.calculateClusters();

        // ca t'en as pas besoin
        ArrayList Clust_array []= kmeans_Niki.getClusters();

        // ca c'est les centroid feature vector et il faudra utiliser ceci pour definir les centroids mais tu devrait
        // peut-etre declarer ca comme une variable de classe comme cela tu garde les valeurs une fois sorti de la
        // methode Kmeans_? regard comme je l'ai fait en bas.
        double[][] Clust_Center =   kmeans_Niki.getClusterCenters();
        this.kmeanCentroids     =   kmeans_Niki.getClusterCenters();
        this.kmeansLabels       =   kmeans_Niki.getClusterLabels();

        System.out.println("Kmeans Done! ^^");

        System.out.println("Calculating Mean of the Clust_Centers");
        for (m = 0; m < slice_num; m++) {
            mean0 += Clust_Center[0][m] / slice_num;
            mean1 += Clust_Center[1][m] / slice_num;
            mean2 += Clust_Center[2][m] / slice_num;
        }
        System.out.println(mean0);
        System.out.println(mean1);
        System.out.println(mean2);
        System.out.println("Mean of Clust_Centers DONE !");


        System.out.println("Create the Map 2D Image with labels");

//         System.out.println(Clust_array[0].size());
//         System.out.println(Clust_array[1].size());
//         System.out.println(Clust_array[2].size());
//         System.out.println(result_fft.length);
//         ArrayList Map_array[]= new ArrayList[];


        System.out.println("Map 2D Image DONE !");


    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /** Create the filtered images
     * @param stack2: new duplicated stack image from the Original Stack Image
     * @param sigmaX : Sigma value for the X axis
     * @param sigmaY : Sigma value for the Y axis
     * @return : returns the image obtained after applying the Gaussian Filter
     */

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

    /** Apply to the highest pic (mean of the Kmean) the lowest sigma value
     * @param stack2
     * @param Gauss_Stack1
     * @param Gauss_Stack2
     * @param Gauss_Stack3
     */

    public void Gaussian_Filter_(ImageStack stack2,ImageStack Gauss_Stack1,ImageStack Gauss_Stack2,ImageStack Gauss_Stack3) { // ADD THE KMEANS ARRAY RESULT AS ARGUMENT

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
                        //System.out.println("1");
                    }
                    if (TEST_Array[i][j] == 2) {
                        //System.out.println("2");
                    }
                    if (TEST_Array[i][j] == 3) {
                        //System.out.println("3");
                    }
                }
            }
        }
        System.out.println("AGF Done !");
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    public void Max_Proj(){

        System.out.println("Maximum Projection Done!");

    }


    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /**Ask the user for the parameters
     * Here the filtered images are created with the sigma values given by the user
     * The first sigma has to be the lowest and the 3rd one the highest. Here the default values are 5, 15 and 20.
     * The argument given below are the duplicated stack images on which the gaussian filter is applied and
     * that will be used for the Adaptive Gaussian Filter
     *
     * @param stack2 : image stack duplicate for 1st filtered image
     * @param stack3 : image stack duplicate for 2nd filtered image
     * @param stack4 : image stack duplicate for 3rd filtered image
     */


    public void showDialog(ImageStack stack2,ImageStack stack3,ImageStack stack4) {
        int sigma1= 5;
        int sigma2= 15;
        int sigma3= 20;

        GenericDialog gd = new GenericDialog("Sigma Values for Gaussian Filtering");
        gd.addNumericField("Sigma1: ", sigma1, 0);
        gd.addNumericField("Sigma2: ", sigma2, 0);
        gd.addNumericField("Sigma3: ", sigma3, 0);

        gd.showDialog();

        // Create the filtered images with the different sigma values needed for hte Adaptive Gaussian Filter
        ImageStack Blur1 = Create_Gaussian_Image(stack2, sigma1, sigma1);
        ImageStack Blur2 = Create_Gaussian_Image(stack3, sigma2, sigma2);
        ImageStack Blur3 = Create_Gaussian_Image(stack4, sigma3, sigma3);

    }
}