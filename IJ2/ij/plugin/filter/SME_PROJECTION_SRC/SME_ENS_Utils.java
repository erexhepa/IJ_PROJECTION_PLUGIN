package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.StatUtils;

import java.text.DecimalFormat;


/**
 * Created by eltonr on 19/03/16.
 */
public final class SME_ENS_Utils {

    private static DecimalFormat df2 = new DecimalFormat("###.###");

    public static double[] convertFloatVecToDoubles(float[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }

    public static float[] convertDoubleVecFloat(double[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        float[] output = new float[input.length];

        for (int i = 0; i < input.length; i++)
        {
            output[i] = (float) input[i];
        }
        return output;
    }

    public static double[][] convertFloatMatrixToDoubles(float[][] input, int dimRows, int dimColoumns)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }

        double[][] output = new double[dimRows][dimColoumns];

        for (int i = 0; i < dimRows; i++) {
            for (int j=0 ; j< dimColoumns; j++) {
                output[i][j] = input[i][j];
            }
        }
        return output;
    }

    public static float[][] convertDoubleMatrixToFloat(double[][] input, int dimRows, int dimColoumns)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }

        float[][] output = new float[dimRows][dimColoumns];

        for (int i = 0; i < dimRows; i++) {
            for (int j=0 ; j< dimColoumns; j++) {
                output[i][j] = (float) input[i][j];
            }
        }

        return output;
    }

    public static RealMatrix stack2matrix(ImageStack imStack){

        // TODO Finish development if necessary

        RealMatrix stack2matrix = MatrixUtils.createRealMatrix(
                imStack.getProcessor(0).getHeight()*imStack.getSize(),
                imStack.getProcessor(0).getWidth()
        );

        int iPadStart,iPadEnd = 0;

        for(int i=0;i<imStack.getSize();i++){
            iPadStart   = i*imStack.getProcessor(0).getHeight();
            iPadEnd     = iPadStart+imStack.getProcessor(0).getHeight();

            /**double[][] stackSlice = imStack.getProcessor(i).convertToFloatProcessor().getBufferedImage();
             int iSlice = 0 ;
             double[] vec = stackSlice[];

             for(int j=iPadStart;j<=iPadEnd;j++){
             stack2matrix.setRowVector(j,);
             }
             **/
        }

        return(stack2matrix);
    }

    // TODO Check correspondence of java and ImageStack index for all for loops where
    // a ImageStack is accessed

    public static RealMatrix padSymetricMatrix(RealMatrix inMatrix, Boolean transMatr){
        RealMatrix padedMatrix          = null;
        RealMatrix templateMatrix       = null;

        //transpose if necessary
        if(transMatr) {
            templateMatrix = inMatrix.copy().transpose();
        }else{
            templateMatrix = inMatrix.copy();
        }

        padedMatrix          = MatrixUtils.createRealMatrix(templateMatrix.getRowDimension()+2,
                templateMatrix.getColumnDimension());

        int iPadStart = 1,iPadEnd = padedMatrix.getRowDimension()-1;

        padedMatrix.setRowVector(0,templateMatrix.getRowVector(1));
        padedMatrix.setRowVector(iPadEnd,templateMatrix.getRowVector(templateMatrix.getRowDimension()-1));

        // symetrical padding
        for(int i=iPadStart;i<iPadEnd;i++){
                padedMatrix.setRowVector(i,templateMatrix.getRowVector(i-1));
        }

        return(padedMatrix);
    }


    public static ImageStack find_base(RealMatrix inMatrix, int indexK){
        int nrowsMat = inMatrix.getRowDimension();
        int ncolsMat = inMatrix.getColumnDimension();

        int loop    =   0;
        int sz1     =   inMatrix.getRowDimension()-indexK+1;
        int sz2     =   inMatrix.getColumnDimension()-indexK+1;

        ImageStack baseMatrix           = new ImageStack(ncolsMat , nrowsMat);

        for(int inx=0;inx<indexK;inx++){
            for(int iny=0;iny<indexK;iny++){
                int rowStart = inx;int rowEnd = (inx+sz1-1);
                int colStart = iny;int colEnd = (iny+sz2-1);

                RealMatrix tmpMatrix = MatrixUtils.createRealMatrix(nrowsMat, ncolsMat);
                RealMatrix subMatrix = inMatrix.getSubMatrix(rowStart,rowEnd,colStart,colEnd).copy();
                tmpMatrix.setSubMatrix(subMatrix.getData(),rowStart,colStart);
                tmpMatrix = tmpMatrix.transpose();
                ImageProcessor ip   = (ImageProcessor) new FloatProcessor(ncolsMat,nrowsMat);
                float[][] sliceData =  SME_ENS_Utils.convertDoubleMatrixToFloat(tmpMatrix.getData(),ncolsMat,nrowsMat);

                ip.setFloatArray(sliceData);
                baseMatrix.addSlice(ip);
                loop= loop+1;
            }
        }

        int indxRetour      = (int) Math.pow(indexK,2);
        baseMatrix.deleteSlice(indxRetour);
        /**
         base(:,:,ceil((k^2)/2))=[];
         */

        return(baseMatrix);
    }

    public static ImageStack repmatMatrixVar(RealMatrix inMatrix, ImageStack base){
        //varold2=sum((base-repmat(Mold,[1 1 8])).^2,3);
        final int DIMZ              = 8;
        int nrowsMat                = inMatrix.getRowDimension();
        int ncolsMat                = inMatrix.getColumnDimension();
        ImageStack baseMatrix       = new ImageStack(ncolsMat,nrowsMat);

        for(int inz=0;inz<DIMZ;inz++){
                RealMatrix currentSlice = MatrixUtils.createRealMatrix(
                        convertFloatMatrixToDoubles(base.getProcessor(inz+1).getFloatArray(),
                                ncolsMat,nrowsMat));
                currentSlice = currentSlice.transpose();

                RealMatrix tmpSlice =   currentSlice.subtract(inMatrix);
                tmpSlice            =   realmatrixDoublepow(tmpSlice,2);
                tmpSlice            =   tmpSlice.transpose();

                baseMatrix.addSlice((ImageProcessor) new FloatProcessor(
                        SME_ENS_Utils.convertDoubleMatrixToFloat(
                                tmpSlice.getData(),ncolsMat,nrowsMat
                        )));
        }

        return(baseMatrix);
    }

    public static RealMatrix elementMultiply(RealMatrix inMatrix1,RealMatrix inMatrix2){
        int nrows = inMatrix1.getRowDimension();
        int ncols = inMatrix1.getColumnDimension();

        RealMatrix retuRealMatrix = MatrixUtils.createRealMatrix(nrows,ncols);

        for(int i=0;i<nrows;i++){
            for(int j=0;j<ncols;j++){
                retuRealMatrix.setEntry(i,j,inMatrix1.getEntry(i,j)* inMatrix2.getEntry(i,j));
            }
        }

        return(retuRealMatrix);
    }


    public static RealMatrix realmatrixDoublepow(RealMatrix inMatrix, double expPow){
        int nrows = inMatrix.getRowDimension();
        int ncols = inMatrix.getColumnDimension();

        RealMatrix retuRealMatrix = MatrixUtils.createRealMatrix(nrows,ncols);

        for(int i=0;i<nrows;i++){
            for(int j=0;j<ncols;j++){
                retuRealMatrix.setEntry(i,j, Math.pow(inMatrix.getEntry(i,j),expPow));
            }
        }

        return(retuRealMatrix);
    }

    public static ImageStack realmatrixCat(ImageStack imageStack, RealMatrix inMatrix){

        ImageStack returStack = imageStack;

        int ncols = inMatrix.getColumnDimension();
        int nrows = inMatrix.getRowDimension();

        returStack.addSlice( (ImageProcessor) new FloatProcessor(
                convertDoubleMatrixToFloat(inMatrix.getData(), nrows,ncols))) ;

        return(returStack);
    }

    public static RealMatrix getMaxProjectionIndex(ImageStack imageStack){

        double[][] maxIndex = new double[imageStack.getProcessor(1).getHeight()]
                [imageStack.getProcessor(1).getWidth()];

        for(int pixIndexI=0;pixIndexI<=imageStack.getProcessor(1).getHeight();pixIndexI++){
            for(int pixIndexJ=0;pixIndexJ<=imageStack.getProcessor(1).getHeight();pixIndexJ++){
                double maxZval = Double.MIN_VALUE;

                for(int pixIndexZ=0;pixIndexZ<=imageStack.getSize();pixIndexZ++){
                    if(imageStack.getVoxel(pixIndexJ,pixIndexI,pixIndexZ)>maxZval){
                        maxIndex[pixIndexI][pixIndexJ] = pixIndexZ;
                        maxZval = imageStack.getVoxel(pixIndexJ,pixIndexI,pixIndexZ);
                    }
                }
            }
        }

        return(MatrixUtils.createRealMatrix(maxIndex));
    }

    public static RealMatrix getMinProjectionIndex(ImageStack imageStack){

        int colDim = imageStack.getProcessor(1).getWidth();
        int rowDim = imageStack.getProcessor(1).getHeight();
        int zDim   = imageStack.getSize();

        double[][] maxIndex = new double[rowDim][colDim];

        for(int pixIndexI=0;pixIndexI<rowDim;pixIndexI++){
            for(int pixIndexJ=0;pixIndexJ<colDim;pixIndexJ++){
                double minZval = Double.MAX_VALUE;

                for(int pixIndexZ=0;pixIndexZ<zDim;pixIndexZ++){
                    if(imageStack.getVoxel(pixIndexJ,pixIndexI,pixIndexZ)<minZval){
                        maxIndex[pixIndexI][pixIndexJ] = pixIndexZ;
                        minZval = imageStack.getVoxel(pixIndexJ,pixIndexI,pixIndexZ);
                    }
                }
            }
        }

        return(MatrixUtils.createRealMatrix(maxIndex));
    }

    public static void replaceRealmatElements(RealMatrix inMatrix,double elVal, double repVal){

        int ncols = inMatrix.getColumnDimension();
        int nrows = inMatrix.getRowDimension();

        for(int i=0;i<nrows;i++){
            for(int j=0;j<ncols;j++){
                if(inMatrix.getEntry(i,j)==elVal){
                    inMatrix.setEntry(i,j,repVal);
                }
            }
        }
    }

    public static RealVector realmat2vector(RealMatrix inMatrix, int dimWalk){
        RealVector retVector = null;

        int ncols = inMatrix.getColumnDimension();
        int nrows = inMatrix.getRowDimension();

        switch (dimWalk){
            case 1:
            {
                retVector = MatrixUtils.createRealVector(inMatrix.getRow(0));
                for(int i=1;i<nrows;i++){
                    retVector.append(inMatrix.getRowVector(i));
                }
            }
            default:
            {
                retVector = MatrixUtils.createRealVector(inMatrix.getColumn(0));
                for(int i=1;i<ncols;i++){
                    retVector.append(inMatrix.getColumnVector(i));
                }
            }
        }

        return(retVector);
    }

    public static void printRealMatrix(double[][] matrix,String matrixID) {

        System.out.println("################## Matrix print - "+matrixID+ " ###################");

        for (int row = 0; row < matrix.length; row++) {
            for (int col = 0; col < matrix[row].length; col++) {
                System.out.printf("%6.2f",(matrix[row][col]));
                System.out.printf("|");
            }
            System.out.println();
        }
    }

    public static void printRealMatrixStats(RealMatrix inMatrix, String matrixID){
        double[] dataStream     =   realmat2vector(inMatrix,1).toArray();

        System.out.println("################## Summary Statistics - "+matrixID+ " ###################");
        System.out.printf("Min value : ");System.out.printf(df2.format(StatUtils.min(dataStream)));System.out.printf("\n");
        System.out.printf("Max value : ");System.out.printf(df2.format(StatUtils.max(dataStream)));System.out.printf("\n");
        System.out.printf("Sum value : ");System.out.printf(df2.format(StatUtils.sum(dataStream)));System.out.printf("\n");
        System.out.printf("Var value : ");System.out.printf(df2.format(StatUtils.variance(dataStream)));System.out.printf("\n");
        System.out.printf("Median value : ");System.out.printf(df2.format(StatUtils.percentile(dataStream,0.5)));System.out.printf("\n");
        System.out.printf("Mean value : ");System.out.printf(df2.format(StatUtils.mean(dataStream)));System.out.printf("\n");
        //System.out.printf("Mode value : ");System.out.printf(df2.format(StatUtils.mode(dataStream)));System.out.printf("\n");
        //System.out.printf("25% value : ");System.out.printf(df2.format(StatUtils.percentile(dataStream,0.25)));System.out.printf("\n");
        //System.out.printf("75% value : ");System.out.printf(df2.format(StatUtils.percentile(dataStream,0.75)));System.out.printf("\n");
    }
}
