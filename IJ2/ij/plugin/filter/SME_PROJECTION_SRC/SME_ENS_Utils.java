package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


/**
 * Created by eltonr on 19/03/16.
 */
public final class SME_ENS_Utils {

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

        padedMatrix          = MatrixUtils.createRealMatrix(templateMatrix.getRowDimension()*2,
                templateMatrix.getColumnDimension());

        int iPadStart,iPadEnd = 0;

        // symetrical padding
        for(int i=0;i<=padedMatrix.getRowDimension();i++){
            iPadStart   = i*2;
            iPadEnd     = iPadStart+2;

            for(int j=iPadStart;j<=iPadEnd;j++){
                padedMatrix.setRowVector(j,templateMatrix.getRowVector(i));
            }
        }

        return(padedMatrix);
    }


    public static ImageStack find_base(RealMatrix inMatrix, int indexK){
        ImageStack baseMatrix           = new ImageStack(inMatrix.getRowDimension(),
                inMatrix.getColumnDimension());

        int nrowsMat = inMatrix.getRowDimension();
        int ncolsMat = inMatrix.getColumnDimension();

        int loop    =   0;
        int sz1     =   inMatrix.getRowDimension()-indexK+1;
        int sz2     =   inMatrix.getColumnDimension()-indexK+1;

        for(int inx=0;inx<indexK;inx++){
            for(int iny=0;inx<indexK;inx++){
                RealMatrix subMatrix = inMatrix.getSubMatrix(inx,(inx+sz1-1),iny,(iny+sz2-1)).copy();

                baseMatrix.addSlice((ImageProcessor) new FloatProcessor(
                        SME_ENS_Utils.convertDoubleMatrixToFloat(
                                subMatrix.getData(),nrowsMat,ncolsMat
                        )));
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
        final int DIMZ = 8;

        ImageStack baseMatrix           = new ImageStack(inMatrix.getRowDimension(),
                inMatrix.getColumnDimension());

        int nrowsMat                = inMatrix.getRowDimension();
        int ncolsMat                = inMatrix.getColumnDimension();

        for(int inz=0;inz<DIMZ;inz++){
                RealMatrix currentSlice = MatrixUtils.createRealMatrix(
                        convertFloatMatrixToDoubles(base.getProcessor(inz+1).getFloatArray(),
                        nrowsMat,ncolsMat));
                RealMatrix tmpSlice     = currentSlice.subtract(inMatrix);
                tmpSlice     = tmpSlice.power(2);
                baseMatrix.addSlice((ImageProcessor) new FloatProcessor(
                        SME_ENS_Utils.convertDoubleMatrixToFloat(
                                tmpSlice.getData(),nrowsMat,ncolsMat
                        )));
        }

        return(baseMatrix);
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

        double[][] maxIndex = new double[imageStack.getProcessor(1).getWidth()]
                [imageStack.getProcessor(1).getHeight()];

        for(int pixIndexI=0;pixIndexI<=imageStack.getProcessor(1).getWidth();pixIndexI++){
            for(int pixIndexJ=0;pixIndexJ<=imageStack.getProcessor(1).getWidth();pixIndexJ++){
                double maxZval = Double.MIN_VALUE;

                for(int pixIndexZ=0;pixIndexZ<=imageStack.getSize();pixIndexZ++){
                    if(imageStack.getVoxel(pixIndexI,pixIndexJ,pixIndexZ)>maxZval){
                        maxIndex[pixIndexI][pixIndexJ] = pixIndexZ;
                        maxZval = imageStack.getVoxel(pixIndexI,pixIndexJ,pixIndexZ);
                    }
                }
            }
        }

        return(MatrixUtils.createRealMatrix(maxIndex));
    }

    public static RealMatrix getMinProjectionIndex(ImageStack imageStack){

        double[][] maxIndex = new double[imageStack.getProcessor(1).getWidth()]
                [imageStack.getProcessor(1).getHeight()];

        for(int pixIndexI=0;pixIndexI<=imageStack.getProcessor(1).getWidth();pixIndexI++){
            for(int pixIndexJ=0;pixIndexJ<=imageStack.getProcessor(1).getWidth();pixIndexJ++){
                double minZval = Double.MAX_VALUE;

                for(int pixIndexZ=0;pixIndexZ<=imageStack.getSize();pixIndexZ++){
                    if(imageStack.getVoxel(pixIndexI,pixIndexJ,pixIndexZ)<minZval){
                        maxIndex[pixIndexI][pixIndexJ] = pixIndexZ;
                        minZval = imageStack.getVoxel(pixIndexI,pixIndexJ,pixIndexZ);
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
}
