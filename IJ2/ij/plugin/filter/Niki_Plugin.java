/** Nikita MENEZES B.**/
/** Elton REXHEPAJ **/
/** Asm SHIAVUDDIN **/
/** Sreetama BASU **/

package ij.plugin.filter;
import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.util.ArrayUtil;

import java.util.Arrays;
import java.util.Vector;


/**
 Plugin Description :
 **/


public class Niki_Plugin implements PlugInFilter {

    public ImagePlus imp;
    public ImagePlus imp2;
    public ImagePlus imp3;
    public ImagePlus imp5;
    public ImagePlus imp5_ind;
    public ImagePlus imp6;

    public ImageStack stack;
    public double[] kmeansLabels ;
    public double[][] kmeanCentroids;
    protected float mean0;
    protected float mean1;
    protected float mean2;
    public ImageStack Blur0;
    public ImageStack Blur1;
    public ImageStack Blur2;
    public  ImageStack stack1;


    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL + STACK_REQUIRED; // Works for stack images
    }


    public void run(ImageProcessor ip) {
        stack = imp.getStack();                  // ImagePlus into ImageStack
        this.stack1 = stack.duplicate();   // Duplicates the original stack image
        ImageStack stack2 = stack.duplicate();   // Duplicates the original stack image
        ImageStack stack3 = stack.duplicate();   // Duplicates the original stack image
        ImageStack stack4 = stack.duplicate();   // Duplicates the original stack image

//       showDialog(stack2, stack3, stack4);
//        sML(stack1);
//        Kmeans_(3, FFT_1D_(stack1));
//        Gaussian_Filter_(Image_Segmented(kmeansLabels));

        Sigma_choice_auto();
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /** Applying the sML filter on the Original ImageStack
     * @param stack1 : Original image stack duplicated
     * @return : returns the stack image after sML filtering is done
     */

    public ImageStack sML(ImageStack stack1) {

        float[] kernel1_X = {0, 0, 0, -1, 2, -1, 0, 0, 0}; // Kernel for the X axis
        float[] kernel2_Y = {0, -1, 0, 0, 2, 0, 0, -1, 0}; // Kernel for the Y axis

        ImagePlus imp_sml = imp.duplicate();

        ImageProcessor ip = imp_sml.getProcessor();
        stack1 = imp_sml.getStack();                    // ImagePlus into ImageStack
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

        //Image display in new window
        this.imp2 = new ImagePlus("sML_"+imp.getTitle(),stack1);
        imp2.setStack(stack1, 1, size_, 1);
        imp2.setCalibration(imp2.getCalibration());

        //imp2.show();

        return stack1;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /** Apply 1D FFT on the stack image obtained after doing the sML filtering
     * Padding is done for the images that are not power of 2
     *
     * @param stack1: stack image returned after applying sML filtering
     * @return : returns the matrix containing the 1D FFT result
     */

    public double[][] FFT_1D_(ImageStack stack1) {

        int x_coord, y_coord, z_coord, z_fft, i, j, k, l;
        int size1_ = stack.getSize();                   // Size of the stack image
        ImageProcessor ip_FFT = stack.getProcessor(1);  // Step done just to have W and H of one image
        ImageProcessor ip = imp.getProcessor();
        ImageProcessor ip_zero = ip.duplicate();
        int W = ip_FFT.getWidth();                      // Get the image width
        int H = ip_FFT.getHeight();                     // Get the image height


        // Make the padding and apply the FFT for a number of stacks that is not power of 2
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
                    z_fft_value[0]=new Complex(0,0);
                    z_fft_value[size1_-1]=new Complex(0,0);


                    //Calculate absolute value
                    for (z_fft = 0; z_fft < size2_; z_fft++) {
                        float absolute_ = (float) Math.sqrt((Math.pow(z_fft_value[z_fft].re(), 2.0)) + (Math.pow(z_fft_value[z_fft].im(), 2.0)));
                        abs_array[z_fft] = absolute_;
                    }

                    //Add the created array in a new matrix with a width size of z and a height size of W*H
                    int k_=y_coord*W+x_coord;
                    for (z_coord=0;z_coord<size1_;z_coord++){
                        final_FFT_result_[k_][z_coord] = abs_array[z_coord];
                    }
                }
            }

            // Find the max value along (x,y) in the matrix that is needed for normalization
            double[] vect_max= new double[W];
            for (i = 0 ; i < size2_; i++) {
                double largest_ = final_FFT_result_[i][1];
                for (j=0 ; j < W*H ; j++) {
                    if (final_FFT_result_[j][i] > largest_)
                        largest_ = final_FFT_result_[j][i];
                }
                vect_max[i]=largest_;
            }

            // Normalization
            for (i = 0; i < W*H ; i++) {
                for (j = 0; j < size2_; j++) {
                    final_FFT_result_[i][j] = final_FFT_result_[i][j] / vect_max[j];
                }
            }
            return final_FFT_result_;
        }

        // Part of the program for the case where the padding is not needed
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
                z_fft_value[0]=new Complex(0,0);        // replace the first value of the FFT by 0
                z_fft_value[size1_-1]=new Complex(0,0); // replace the last value of the FFT by 0


                //Calculate absolute value
                for (z_fft = 0; z_fft < size1_; z_fft++) {
                    float absolute_ = (float) Math.sqrt((Math.pow(z_fft_value[z_fft].re(), 2.0)) + (Math.pow(z_fft_value[z_fft].im(), 2.0)));
                    abs_array[z_fft] = absolute_;
                }

                //Add the created array in a new matrix with a width size of z and a height size of W*H
                int k_=y_coord*W+x_coord;
                for (z_coord=0;z_coord<size1_;z_coord++){
                    final_FFT_result_[k_][z_coord] = abs_array[z_coord];
                }
            }
        }

        // Find the max value along (x,y) in the matrix that is needed for normalization
        double[] vect_max= new double[W];
        for (i = 0 ; i < size1_; i++) {
            double largest_ = final_FFT_result_[i][1];
            for (j=0 ; j < W*H ; j++) {
                if (final_FFT_result_[j][i] > largest_)
                    largest_ = final_FFT_result_[j][i];
            }
            vect_max[i]=largest_;
        }

        // Normalization
        for (i = 0; i < W*H ; i++) {
            for (j = 0; j < size1_; j++) {
                final_FFT_result_[i][j] = final_FFT_result_[i][j] / vect_max[j];
            }
        }
        return final_FFT_result_;
    }

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

    public void Kmeans_(int numClust_, double [][] result_fft) {
        int m;
        int slice_num = stack.getSize();

        Kmeans kmeans_Niki = new Kmeans(result_fft, numClust_,false);
        kmeans_Niki.calculateClusters();
        this.kmeanCentroids     =   kmeans_Niki.getClusterCenters();
        this.kmeansLabels       =   kmeans_Niki.getClusterLabels();

        for (m = 0; m < slice_num; m++) {
            this.mean0 += kmeanCentroids[0][m]/ slice_num;
            this.mean1 += kmeanCentroids[1][m]/ slice_num;
            this.mean2 += kmeanCentroids[2][m]/ slice_num;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /** Create the Kmeans segmented image and display it
     *
     * @param kmeansLabels : the array containing the labels obtained thanks to Kmeans_
     * @return : returns the Image matrix which will be used as a Map for the Adaptive Gaussian Filtering
     */
    public float [][] Image_Segmented (double [] kmeansLabels) {

        int x, y;
        ImageProcessor ip_ = stack.getProcessor(1);  // Done just to have W and H of one image
        int W = ip_.getWidth();                      // Get the image width
        int H = ip_.getHeight();                     // Get the image height
        float Map2DImage[][] = new float[W][H];

        for (x = 0; x < W; x++) {                     //Go through x coordinates
            for (y = 0; y < H; y++) {                 //Go through y coordinates
                int k = y*W+x;
                Map2DImage[x][y] = (float)kmeansLabels[k];
            }
        }

        //Image Display in a new window
        FloatProcessor fp3 = new FloatProcessor(Map2DImage);
        ImageProcessor ip3 = fp3.convertToFloat();
        ip3.setFloatArray(Map2DImage);
        ImagePlus imp3 = new ImagePlus("Kmeans Segmented Image"+imp.getTitle(),ip3);
        imp3.setProcessor(ip3);
        this.imp3=imp3;
        //imp3.show();

        return Map2DImage;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /** Create the filtered images
     * @param stack2: new duplicated stack image from the Original Stack Image
     * @param sigmaX : Sigma value for the X axis
     * @param sigmaY : Sigma value for the Y axis
     * @return : returns the image obtained after applying the Gaussian Filter
     */

    public ImageStack Create_Gaussian_Image(ImageStack stack2, double sigmaX, double sigmaY) {

        int slice;                                                     // Define the type of slice
        int size3_ = stack2.getSize();                                 // Size of the stack image
        ImageProcessor ip_Gauss;                                       // Work on the duplicated images.
        GaussianBlur blurimg = new GaussianBlur();                     // Create ImageType
        ImageStack Gauss_Stack = stack2.duplicate();                   // We duplicate the original stack image .Create new empty stack to put the pixel values obtained after blurring blur

        for (slice = 1; slice <= size3_; slice++) {                    // Go through each slice
            ip_Gauss = Gauss_Stack.getProcessor(slice);                     // Returns an ImageProcessor for the specified slice using the Original stack image
            blurimg.blurGaussian(ip_Gauss, sigmaX, sigmaY, 0.01);      // Apply the blurring on the given slice
        }
        return Gauss_Stack;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /** Apply the adaptive gaussian filtering to the original image by using the Kmeans Map obtained previously
     * @param Map2DImage: map image obtain after applying Kmeans_ function and reshaping the KmeansLabel array into an image
     *                  matrix called Map2DImage.
     */

    public void Gaussian_Filter_(float[][] Map2DImage) {

        int x,y,z,slice,i,j,w,sl,k1_;
        int k_=0;
        int i_=0;
        int j_=0;
        ImageProcessor ip_ = stack.getProcessor(1);  // Done just to have W and H of one image
        int W = ip_.getWidth();                      // Get the image width
        int H = ip_.getHeight();
        int Size_stack = stack.getSize();


        float mean_val_array [] = new float[3];
        float mean_val_array_duplicate [] = new float[3];
        double [] array = new double[Size_stack];
        double [][] Image_AGF_reshape =new double[W*H][Size_stack];
        float [][] Image_AGF_final_projection_blur = new float[W][H];
        float [][] Image_AGF_final_projection = new float[W][H];


        // Create an array containing the 3 mean values
        mean_val_array[0]=mean0;
        mean_val_array[1]=mean1;
        mean_val_array[2]=mean2;

        // Sort the array values (increasing order)
        Arrays.sort(mean_val_array);

        //In a new array find the index of each mean value after having been sorted
        mean_val_array_duplicate[0]=Arrays.binarySearch(mean_val_array,mean0);
        mean_val_array_duplicate[1]=Arrays.binarySearch(mean_val_array,mean1);
        mean_val_array_duplicate[2]=Arrays.binarySearch(mean_val_array,mean2);

        //Change the labels of the 2D Map Image
        for (x=0;x<W;x++){
            for (y=0;y<H;y++){
                if(Map2DImage[x][y]==0){
                    Map2DImage[x][y]=mean_val_array_duplicate[0];
                }
                else if (Map2DImage[x][y]==1){
                    Map2DImage[x][y]=mean_val_array_duplicate[1];
                }
                else if (Map2DImage[x][y]==2){
                    Map2DImage[x][y]=mean_val_array_duplicate[2];
                }
            }
        }

        //Image displayed in a new window
//        FloatProcessor fp4 = new FloatProcessor(Map2DImage);
//        ImageProcessor ip4 = fp4.convertToFloat();
//        ip4.setFloatArray(Map2DImage);
//        ImagePlus imp4 = new ImagePlus("AGF_After_Relabeling_"+imp.getTitle(),ip4);
//        imp4.setProcessor(ip4);
//        //this.imp4=imp4;
//        imp4.show("Relabeled Image ");
//


        for (i=0;i<W;i++) {
            for (j = 0; j < H; j++) {

                if (Map2DImage[i][j] == 0) { //Map2DImage is the image containing the new labels once relabeled (step done previously)
                    for (slice = 0; slice < Size_stack; slice++) {
                        array[slice] = Blur2.getVoxel(i, j, slice);
                    }
                } else if (Map2DImage[i][j] == 1) {
                    for (slice = 0; slice < Size_stack; slice++) {
                        array[slice] = Blur1.getVoxel(i, j, slice);
                    }
                }
                if (Map2DImage[i][j] == 2) {
                    for (slice = 0; slice < Size_stack; slice++) {
                        array[slice] = Blur0.getVoxel(i, j, slice);
                    }
                }

                // reshape each array found previously and added in a new matrix which is Image_AGF_toreshape
                for (z = 0; z < Size_stack; z++) {
                    Image_AGF_reshape[i_][j_] = array[z];
                    j_=j_+1;
                }
                i_=i_+1;
                j_=0;
            }
        }

        // Find maximum value for each pixel in the stack = projection
        double[] vect_max2= new double[W*H];
        double[]index_j= new double[W*H];  // index where the maximum pixel value was taken


        //Looking for the max value for each pixel in among all the stacks
        for (i =0 ; i < W*H ; i++) {
            double largest_2 = Image_AGF_reshape[i][0];  //Consider the first value of the stack for one given pixel as the largest
            for (j=0 ; j < Size_stack; j++) {
                if (Image_AGF_reshape[i][j] > largest_2) // Compare the value contained in largest_2 to the next value
                    largest_2 = Image_AGF_reshape[i][j]; // if the new value is greater that the one contained in largest_2
                // than it will be considered as the new largest value
            }
            vect_max2[i]=largest_2; // all the maximum values are stored in this vector "vect_max2"
        }

        // Here we are looking for the stack index from where the max pixel was taken
        int f = 0;
        for (i =0 ; i < W*H ; i++) {
            for (j=0 ; j < Size_stack; j++) {
                if (Image_AGF_reshape[i][j] == vect_max2[i])
                    f=j;
            }
            index_j[i]=f;
            f=0;
        }

        //Image where the labels are represented = Map
        float [][] index_representation = new float[W][H];
        k1_=0;
        for (i=0;i<W;i++){
            for (j=0;j<H;j++){
                index_representation[i][j]=(float)index_j[k1_];
                k1_=k1_+1;
            }
        }


        // Add the new max values found of the corresponding blurred image in a new matrix
        for (x = 0; x < W; x++) {
            for (y = 0; y < H; y++) {
                Image_AGF_final_projection_blur[x][y] = (float) vect_max2[k_];
                k_=k_+1;
            }
        }

        // Add the new max values found of the corresponding original image in a new matrix using index_j vector to
        // extract the right pixel
        w=0;
        for (x = 0; x < W; x++) {
            for (y = 0; y < H; y++) {
                sl=(int)index_j[w];
                Image_AGF_final_projection[x][y] = (float)stack.getVoxel(x,y,sl); // get the pixel of the original image at the right stack (getVoxel)
                // using index_j
                w=w+1;
            }
        }

        //Image displayed in a new window Image_AGF_final_projection_
        FloatProcessor fp5_ind = new FloatProcessor(index_representation);
        ImageProcessor ip5_ind = fp5_ind.convertToFloat();
        ImagePlus imp5_ind = new ImagePlus("Index_image"+imp.getTitle(),ip5_ind);
        ip5_ind.setFloatArray(index_representation);
        imp5_ind.setProcessor(ip5_ind);
        this.imp5_ind=imp5_ind;
        //imp5_ind.show();

        //Image displayed in a new window Image_AGF_final_projection_blur
        FloatProcessor fp5 = new FloatProcessor(Image_AGF_final_projection_blur);
        ImageProcessor ip5 = fp5.convertToFloat();
        ImagePlus imp5 = new ImagePlus("BLUR_Final_Projected_Image"+imp.getTitle(),ip5);
        ip5.setFloatArray(Image_AGF_final_projection_blur);
        imp5.setProcessor(ip5);
        this.imp5=imp5;
        //imp5.show();

        //Image displayed in a new window Image_AGF_final_projection_
        FloatProcessor fp6 = new FloatProcessor(Image_AGF_final_projection);
        ImageProcessor ip6 = fp6.convertToFloat();
        ImagePlus imp6 = new ImagePlus("Final_Projected_Image"+imp.getTitle(),ip6);
        ip6.setFloatArray(Image_AGF_final_projection);
        imp6.setProcessor(ip6);
        this.imp6=imp6;
        //imp6.show();
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////


    public void Sigma_choice_auto() {

        ImageProcessor ip_ = stack.getProcessor(1);  // Done just to have W and H of one image
        int W = ip_.getWidth();                      // Get the image width
        int H = ip_.getHeight();
        int Size_stack = stack.getSize();
        int k1_ = 0;
        int x, y, z, i, j, k;

        float[][] vect_max3 = new float[W][H];
        float[][] index_j2 = new float[W][H];  // index where the maximum pixel value was taken
        float pix_center, pix_1,pix_2,pix_3,pix_4,pix_5,pix_6,pix_7,pix_8, mean_,var_,sum_, STD_,somme_;
        float [][] STD_values = new float[W][H];


        //Looking for the max value for each pixel in among all the stacks
        for (x = 0; x < W; x++) {
            for (y = 0; y < H; y++) {
                double largest_3 = stack.getVoxel(x, y, 0);
                for (z = 0; z < Size_stack; z++) {
                    if (stack.getVoxel(x, y, z) > largest_3)
                        largest_3 = stack.getVoxel(x, y, z);
                }
                vect_max3[x][y] = (float)largest_3;
            }
        }

        //Looking for the slice index where the maximum pixel value is taken
        int f = 0;
        for (i = 0; i < W; i++) {
            for (j = 0; j < H; j++) {
                for (k = 0; k < Size_stack; k++) {
                    if (stack.getVoxel(i, j, k) == vect_max3[i][j])
                        f = k;
                }
                index_j2[i][j] = f;
                f = 0;
            }
        }

        // Padding for convolution STD
        //Create a matrix with zero
        float [][] index_j2_padded = new float[W+1][H+1];
        for (i=0;i<(W+1);i++){
            for(j=0;j<(H+1);j++){
                index_j2_padded [i][j]=0;
            }
        }

        //Add the index obtained previously in this new matrix
        for (i=1;i<(W);i++){
            for(j=1;j<(H);j++){
                index_j2_padded [i][j]=index_j2[i-1][j-1];
            }
        }

        //STD kernel 3x3 on index value
        for (i = 1; i < (W-1); i++) {
            for (j = 1; j < (H-1); j++) {
                pix_center = index_j2_padded[i][j];
                pix_1 = index_j2_padded[i - 1][j - 1];
                pix_2 = index_j2_padded[i][j - 1];
                pix_3 = index_j2_padded[i + 1][j - 1];
                pix_4 = index_j2_padded[i - 1][j];
                pix_5 = index_j2_padded[i + 1][j];
                pix_6 = index_j2_padded[i - 1][j + 1];
                pix_7 = index_j2_padded[i][j + 1];
                pix_8 = index_j2_padded[i + 1][j + 1];

                mean_ = (pix_1 + pix_2 + pix_3 + pix_4 + pix_5 + pix_6 + pix_7 + pix_8 + pix_center)/9;
                sum_ = (pix_center - mean_) * (pix_center - mean_)+(pix_1 - mean_) * (pix_1 - mean_)+(pix_2 - mean_) * (pix_2 - mean_)+(pix_3 - mean_) * (pix_3 - mean_)+
                        (pix_4 - mean_) * (pix_4 - mean_)+(pix_5 - mean_) * (pix_6 - mean_)+(pix_7 - mean_) * (pix_7 - mean_)+(pix_8 - mean_) * (pix_8 - mean_);
                var_ = sum_ / 9;
                STD_ = (float)Math.sqrt(var_);

                STD_values[i-1][j-1]=STD_;
            }
        }

        //Find the largest value
        float largest_4=0;
        for (i = 0; i < W; i++) {
            for (j=0;j< H;j++){
                STD_values[i][0] = largest_4;
                if (STD_values[i][j]>largest_4)
                    largest_4=STD_values[i][j];
            }
        }

        // Find the cut off
        float thres_ = largest_4/16;

        // Apply the threshold
        for (i = 0; i < W; i++) {
            for (j = 0; j < H; j++) {
                if (STD_values[i][j]<= thres_)
                    STD_values[i][j]=0;
                else
                    STD_values[i][j]=255;
            }
        }


        FloatProcessor fp_sig = new FloatProcessor(STD_values);
        ByteProcessor bp_=fp_sig.convertToByteProcessor();
        ImagePlus imp_sig = new ImagePlus(""+imp.getTitle(),bp_);
        float [] EDM_value_array = new float[W*H];


        EDM edm_sig1 = new EDM();
        FloatProcessor fp_sig1 = edm_sig1.makeFloatEDM(bp_, 0, true);

        k=0;
        int val_EDM;
        for (i=0;i < W;i++){
            for (j=0;j < H; j++){
                EDM_value_array[k]=fp_sig1.getPixel(i,j);
                k=k+1;
            }
        }

        // Sort the values obtained after applying EDM
        Arrays.sort(EDM_value_array);
        int divide = (EDM_value_array.length)-1;
        float sigma_value_1 = EDM_value_array[divide/3];
        float sigma_value_2 = EDM_value_array[divide/2];
        float sigma_value_3 = EDM_value_array[divide];

//

        imp_sig.setProcessor(fp_sig1);
        imp_sig.show();


//            double [] vc=new double[W];
//            for (i = 0; i < W ; i++) {
//                vc[i]=1;
//            }


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

        GenericDialog gd = new GenericDialog("Sigma Values for Gaussian Filtering");

        //DialogBox
        gd.addMessage("Sigma values should be given in increasing order");

        gd.addCheckbox("Input sigma values manually ", true);
        gd.addCheckbox("Use automated way to generate sigma values ", false);
        gd.addCheckbox("Show SML image", false);
        gd.addCheckbox("Show blurred images", false);
        gd.addCheckbox("Show Label Map Image", false);
        gd.addCheckbox("Show max projection from blurred images ", false);
        gd.showDialog();
        gd.getCheckboxes();

        // Get the checkboxes "values" = True or False
        boolean ind1=gd.getNextBoolean();
        boolean ind2=gd.getNextBoolean();
        boolean ind3=gd.getNextBoolean();
        boolean ind4=gd.getNextBoolean();
        boolean ind5=gd.getNextBoolean();
        boolean ind6=gd.getNextBoolean();

        if (ind1) { // if true for checkbox 1
            //Default values
            double sigma1=0;
            double sigma2=5;
            double sigma3=10;

            //open a new generic dialog
            GenericDialog gd1 = new GenericDialog("Sigma Values for Gaussian Filtering");
            gd1.addNumericField("Sigma1: ", sigma1, 0);
            gd1.addNumericField("Sigma2: ", sigma2, 0);
            gd1.addNumericField("Sigma3: ", sigma3, 0);
            gd1.showDialog(); // show the window and wait for the user input
            gd1.getChoices(); // take the values given by the user and replace the default values

            //Get values given by the user and assign them to each variable
            sigma1=gd1.getNextNumber();
            sigma2=gd1.getNextNumber();
            sigma3=gd1.getNextNumber();

            //Apply the blurring using the sigma values given by user
            this.Blur0 = Create_Gaussian_Image(stack2, sigma1, sigma1);
            this.Blur1 = Create_Gaussian_Image(stack3, sigma2, sigma2);
            this.Blur2 = Create_Gaussian_Image(stack4, sigma3, sigma3);

            sML(stack1);
            Kmeans_(3, FFT_1D_(stack1));
            Gaussian_Filter_(Image_Segmented(kmeansLabels));
        }

        if (ind2){
            //implement the automated way to find sigma
            imp.show();
//            sML(stack1);
//            Kmeans_(3, FFT_1D_(stack1));
//            Gaussian_Filter_(Image_Segmented(kmeansLabels));
        }

        if(ind3) {
            imp2.show();
        }

        if(ind4) {
            //Create the filtered images with the different sigma values needed for hte Adaptive Gaussian Filter
            ImagePlus imp7 = new ImagePlus("Blur0_"+imp.getTitle(),stack);
            imp7.setStack(Blur0, 1, stack2.getSize(), 1);
            imp7.show();
            ImagePlus imp8 = new ImagePlus("Blur1_"+imp.getTitle(),stack);
            imp8.setStack(Blur1, 1, stack2.getSize(), 1);
            imp8.show();
            ImagePlus imp9 = new ImagePlus("Blur2_"+imp.getTitle(),stack);
            imp9.setStack(Blur2, 1, stack2.getSize(), 1);
            imp9.show();
        }

        if(ind5) {
            imp5_ind.show();
        }

        if(ind6) {
            imp5.show();
        }

        imp6.show(); // Display the final image
    }
}