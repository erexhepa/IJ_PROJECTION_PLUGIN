package ij.plugin.filter.SME_PROJECTION_SRC;

// TODO Optimize imports by removing not necessary imports

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Created by rexhepaj on 17/03/16.
 */

public class SME_ENS_Kmean_Control {

    private SME_Plugin sme_plugin = null;

    public SME_ENS_Kmean_Control(SME_Plugin refplugin){
        sme_plugin = refplugin;
    }

    public void applyKmeans(){

        int x, y,i,j,k;
        int W = sme_plugin.getStack1().getProcessor(1).getWidth();                      // Get the image width
        int H = sme_plugin.getStack1().getProcessor(1).getHeight();                     // Get the image height

        Kmeans_(3, FFT_1D_(sme_plugin.getStack1()));
        Rearrange_Map2DImage(Image_Segmented(sme_plugin.getKmeansLabels()));

        float [][] foreground_pix =new float[W][H];
        for (x = 0; x < W; x++) {                     //Go through x coordinates
            for (y = 0; y < H; y++) {                 //Go through y coordinates
                foreground_pix [x][y]=1;
            }
        }

        for (x = 0; x < W; x++) {                     //Go through x coordinates
            for (y = 0; y < H; y++) {                 //Go through y coordinates
                if (sme_plugin.getMap2DImage()[x][y]==2)
                    foreground_pix [x][y]=0;
            }
        }

        FloatProcessor fp_sig = new FloatProcessor(foreground_pix);
        ByteProcessor bp_=fp_sig.convertToByteProcessor();
        ImagePlus imp_sig = new ImagePlus(""+sme_plugin.getImp().getTitle(),bp_);
        float [] EDM_value_array = new float[W*H];

        EDM edm_sig1 = new EDM();
        FloatProcessor fp_sig1 = edm_sig1.makeFloatEDM(bp_, 0, false);

        k=0;
        for (i=0;i < W;i++){
            for (j=0;j < H; j++){
                EDM_value_array[k]=fp_sig1.get(i,j);
                k=k+1;
            }
        }

        // Sort the values obtained after applying EDM
        Arrays.sort(EDM_value_array);
        int divide = (EDM_value_array.length)-1;
        float sigma_value_1 = ((EDM_value_array[divide*33/100])+5)*(1/5);
        float sigma_value_2 = ((EDM_value_array[divide*66/100])+5)*(1/5);
        float sigma_value_3 = ((EDM_value_array[divide*99/100])+5)*(1/5);

        imp_sig.setProcessor(fp_sig1);
        //imp_sig.show();

        // this.Blur0 = Create_Gaussian_Image(stack2, sigma_value_1, sigma_value_1);
        // this.Blur1 = Create_Gaussian_Image(stack3, sigma_value_2, sigma_value_2);
        // this.Blur2 = Create_Gaussian_Image(stack4, sigma_value_3, sigma_value_3);

        sme_plugin.setMap2DImage(Rearrange_Map2DImage(Image_Segmented(sme_plugin.getKmeansLabels())));
        sme_plugin.setMap2d( new ImagePlus("Map2d", (new FloatProcessor(sme_plugin.getMap2DImage()))));

        //imp2.show();
        //imp5_ind.show();
        //imp5.show();
        //imp6.show(); // Display the final image
        sme_plugin.getMap2d().show(); // TODO : make this current imagej image that can be grabed by the gui
        sme_plugin.setKmensImage(new ImagePlus("Map2d", (new FloatProcessor(sme_plugin.getMap2DImage()))));
    }

    public float [][] Rearrange_Map2DImage(float[][] Map2DImage) {

        int x, y;
        ImageProcessor ip_ = sme_plugin.getStack().getProcessor(1);  // Done just to have W and H of one image
        int W = ip_.getWidth();                      // Get the image width
        int H = ip_.getHeight();
        float mean_val_array[] = new float[3];
        float mean_val_array_duplicate[] = new float[3];


        // Create an array containing the 3 mean values
        mean_val_array[0] = sme_plugin.getMean0();
        mean_val_array[1] = sme_plugin.getMean1();
        mean_val_array[2] = sme_plugin.getMean2();

        // Sort the array values (increasing order)
        Arrays.sort(mean_val_array);

        //In a new array find the index of each mean value after having been sorted
        mean_val_array_duplicate[0] = Arrays.binarySearch(mean_val_array, sme_plugin.getMean0());
        mean_val_array_duplicate[1] = Arrays.binarySearch(mean_val_array, sme_plugin.getMean1());
        mean_val_array_duplicate[2] = Arrays.binarySearch(mean_val_array, sme_plugin.getMean2());

        //Change the labels of the 2D Map Image
        for (x = 0; x < W; x++)

        {
            for (y = 0; y < H; y++) {
                if (Map2DImage[x][y] == 0) {
                    Map2DImage[x][y] = mean_val_array_duplicate[0];
                } else if (Map2DImage[x][y] == 1) {
                    Map2DImage[x][y] = mean_val_array_duplicate[1];
                } else if (Map2DImage[x][y] == 2) {
                    Map2DImage[x][y] = mean_val_array_duplicate[2];
                }
            }
        }
        return Map2DImage;
    }


    /**
     * Create the SME_ENS_Kmeans_Engine segmented image and display it
     *
     * @param kmeansLabels : the array containing the labels obtained thanks to Kmeans_
     * @return : returns the Image matrix which will be used as a Map for the Adaptive Gaussian Filtering
     */
    public float[][] Image_Segmented(double[] kmeansLabels) {

        int x, y;
        ImageProcessor ip_ = sme_plugin.getStack1().getProcessor(1);  // Done just to have W and H of one image
        int W = ip_.getWidth();                      // Get the image width
        int H = ip_.getHeight();                     // Get the image height
        sme_plugin.setMap2DImage( new float[W][H]);

        for (x = 0; x < W; x++) {                     //Go through x coordinates
            for (y = 0; y < H; y++) {                 //Go through y coordinates
                int k = y * W + x;
                sme_plugin.getMap2DImage()[x][y] = (float) kmeansLabels[k];
            }
        }

        //Image Display in a new window
        FloatProcessor fp3 = new FloatProcessor(sme_plugin.getMap2DImage());
        ImageProcessor ip3 = fp3.convertToFloat();
        ip3.setFloatArray(sme_plugin.getMap2DImage());
        ImagePlus imp3 = new ImagePlus("SME_ENS_Kmeans_Engine Segmented Image" + sme_plugin.getImp().getTitle(), ip3);
        imp3.setProcessor(ip3);
        sme_plugin.setImp3(imp3);
        //imp3.show();

        return sme_plugin.getMap2DImage();
    }

    /**
     * Apply 1D FFT on the stack image obtained after doing the sML filtering
     * Padding is done for the images that are not power of 2
     *
     * @param stack1: stack image returned after applying sML filtering
     * @return : returns the matrix containing the 1D FFT result
     */

    public double[][] FFT_1D_(ImageStack stack1) {
        //TODO: add check step to control for number of slices being a power of 2

        int x_coord, y_coord, z_coord, z_fft, i, j, k, l;
        int size1_ = stack1.getSize();                   // Size of the stack image
        ImageProcessor ip_FFT = stack1.getProcessor(1);  // Step done just to have W and H of one image
        ImageProcessor ip = sme_plugin.getImp().getProcessor();
        ImageProcessor ip_zero = ip.duplicate();
        int W = ip_FFT.getWidth();                      // Get the image width
        int H = ip_FFT.getHeight();                     // Get the image height

        if (!ispower2(size1_)) {

            // Find the closest power of 2 in order to find the number of stacks with 0 to add to the original stack
            double result_ = Math.ceil(Math.log(size1_) / Math.log(2.0d));
            double padding_zero = Math.pow(2.0, result_);
            double number_of_zero = padding_zero - size1_; //Number of stacks to add

            // Create ImageProcessor containing only pixels which value is 0
            for (i = 0; i < W; i++) {
                for (j = 0; j < H; j++) {
                    ip_zero.putPixelValue(i, j, 0);
                }
            }

            int size2_ = sme_plugin.getStack1().getSize();

            // Set the pixel values
            for (l = size1_; l < padding_zero; l++) {
                sme_plugin.getStack1().addSlice(new FloatProcessor(W,H));
            }
            System.out.println("Padding Done !");
        }

        size1_ = sme_plugin.getStack1().getSize();                   // Size of the stack image
        ip_FFT = sme_plugin.getStack1().getProcessor(1);  // Step done just to have W and H of one image

        W = ip_FFT.getWidth();                      // Get the image width
        H = ip_FFT.getHeight();                     // Get the image height

        // Part of the program for the case where the padding is not needed
        SME_ENS_Complex[] z = new SME_ENS_Complex[size1_];
        SME_ENS_Complex[] z_fft_value;
        float abs_array[] = new float[size1_];
        double final_FFT_result_[][] = new double[W * H][size1_];

        for (x_coord = 0; x_coord < W; x_coord++) {              //Go through x coordinates
            for (y_coord = 0; y_coord < H; y_coord++) {          //Go through y coordinates
                for (z_coord = 0; z_coord < size1_; z_coord++) { //Go through each slice (z coordinates)
                    z[z_coord] = new SME_ENS_Complex(sme_plugin.getStack1().getVoxel(x_coord, y_coord, z_coord), 0);
                }

                // Apply FFT on the pixels having the same x,y coordinates and put results in z_fft_value
                z_fft_value = SME_ENS_Filter_FFT_.fft(z);
                //z_fft_value[0] = new SME_ENS_Complex(0, 0);        // replace the first value of the FFT by 0
                //z_fft_value[size1_ - 1] = new SME_ENS_Complex(0, 0); // replace the last value of the FFT by 0


                //Calculate absolute value
                for (z_fft = 0; z_fft < size1_; z_fft++) {
                    float absolute_ = (float) Math.sqrt((Math.pow(z_fft_value[z_fft].re(), 2.0)) +
                            (Math.pow(z_fft_value[z_fft].im(), 2.0)));
                    abs_array[z_fft] = absolute_;
                }

                //Add the created array in a new matrix with a width size of z and a height size of W*H
                int k_ = y_coord * W + x_coord;
                for (z_coord = 0; z_coord < size1_; z_coord++) {
                    final_FFT_result_[k_][z_coord] = abs_array[z_coord];
                }
            }
        }

        // Find the max value along (x,y) in the matrix that is needed for normalization
        int nmbDimFFT       = size1_/2-1;
        double[][] fft_norm_truncated = new double[W*H][nmbDimFFT];
        double[] vect_max   = new double[nmbDimFFT];
        double[] vect_min   = new double[nmbDimFFT];

        vect_min[0] = 0 ; vect_max[0] = 1 ;

        for (i = 0; i < nmbDimFFT; i++) {
            double largest_ = Double.MIN_VALUE;
            double smallest = Double.MAX_VALUE;

            for (j = 0; j < W * H; j++) {

                if (final_FFT_result_[j][i] >= largest_){
                    largest_ = final_FFT_result_[j][i];
                    vect_max[i] = largest_;
                }
                if (final_FFT_result_[j][i] <= smallest) {
                    smallest = final_FFT_result_[j][i];
                    vect_min[i] = smallest;
                }
            }


        }

        // Normalization
        for (i = 0; i < W * H; i++) {
            for (j = 0; j < nmbDimFFT; j++) {
                fft_norm_truncated[i][j] = final_FFT_result_[i][j] / (vect_max[j]-vect_min[j]);
            }
        }

        return final_FFT_result_;
    }

    /**
     * Apply the SME_ENS_Kmeans_Engine for segmentation
     *
     * @param numClust_  : number of clusters
     * @param result_fft : matrix that contains
     *                   the results after applying the FFT_1D = width is equal to the number of stacks in the image
     *                   and length of result_fft is equal to the number of pixels of the image
     */

    public void Kmeans_(int numClust_, double[][] result_fft) {
        int m;
        int slice_num = sme_plugin.getStack().getSize();
        boolean CONCURRENT_KMEANS = true;
        int threadcount = 5;
        final int numClust = numClust_;
        final double[][]  coordClust = result_fft;

        if (CONCURRENT_KMEANS) {

            long randomSeed = 1000;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel(UIManager.
                                getSystemLookAndFeelClassName());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                    sme_plugin.getGui_main().validate();

                    // Center the window
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    Dimension frameSize = sme_plugin.getGui_main().getSize();
                    if (frameSize.height > screenSize.height) {
                        frameSize.height = screenSize.height;
                    }
                    if (frameSize.width > screenSize.width) {
                        frameSize.width = screenSize.width;
                    }
                    sme_plugin.getGui_main().setLocation((screenSize.width - frameSize.width) / 2,
                            (screenSize.height - frameSize.height) / 2);
                    sme_plugin.getGui_main().setVisible(true);
                    //frame.actionPerformedRun();
                    //clustersKmean = frame.getClustersKmeans();*/
                }
            });


            //     = mKMeans.getClusters();



            /*this.kmeansLabels = new double[result_fft.length];
            this.kmeanCentroids = new double[numClust_][result_fft[0].length];

            //iterate through the clusters to get all points
            for(int indxClusters=0;indxClusters<clustersKmean.length;indxClusters++){
                //iterate through all the clusters points to put them in the original order
                int[] indxPoints = clustersKmean[indxClusters].getMemberIndexes();
                //TODO merge lines below
                double[] centerCoord = clustersKmean[indxClusters].getCenter();
                this.kmeanCentroids[indxClusters]=centerCoord;

                for(int indxPointclusters=0;indxPointclusters<indxPoints.length;indxPointclusters++){
                    this.kmeansLabels[indxPoints[indxPointclusters]]=indxClusters;
                }
            }
            //this.kmeanCentroids = OBSKmeans_Niki.getClusterCenters();
            //this.kmeansLabels = OBSKmeans_Niki.getClusterLabels();*/
            SME_ENS_Kmeans_Engine OBSKmeans_Niki = new SME_ENS_Kmeans_Engine(result_fft, numClust_, false);
            OBSKmeans_Niki.calculateClusters();
            sme_plugin.setKmeanCentroids(OBSKmeans_Niki.getClusterCenters());
            sme_plugin.setKmeansLabels(OBSKmeans_Niki.getClusterLabels());

        }else{
            SME_ENS_Kmeans_Engine OBSKmeans_Niki = new SME_ENS_Kmeans_Engine(result_fft, numClust_, false);
            OBSKmeans_Niki.calculateClusters();
            sme_plugin.setKmeanCentroids(OBSKmeans_Niki.getClusterCenters());
            sme_plugin.setKmeansLabels(OBSKmeans_Niki.getClusterLabels());
        }

        for (m = 0; m < slice_num; m++) {
            sme_plugin.setMean0(sme_plugin.getMean0() + (float)sme_plugin.getKmeanCentroids()[0][m]);
            sme_plugin.setMean1(sme_plugin.getMean1() + (float)sme_plugin.getKmeanCentroids()[1][m]);
            sme_plugin.setMean2(sme_plugin.getMean2() + (float)sme_plugin.getKmeanCentroids()[2][m]);
        }

    }

    private boolean ispower2(int input){
        while(((input != 2) && input % 2 == 0) || input == 1) {
            input = input /2;
        }

        return input == 2;
    }
}
