package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.awt.*;

/**
 * Created by rexhepaj on 17/03/16.
 */
public class SME_ENS_EnergyOptimisation {
    private SME_Plugin sme_plugin   = null;
    private final int KMEAN_NORM    = 3;
    private final double ENERGY_STEP= 0.0001;
    private double[][] kmeanOutput ;
    private RealMatrix rawdata2D    = null;
    private RealMatrix tmpProcess   = null;
    private double optStep ;

    private int stepNumber          = 100;
    private RealMatrix idmax        = null;
    private RealMatrix idmaxk       = null;
    private RealMatrix idmaxki      = null;
    private RealMatrix cost         = null;
    private RealMatrix movmat       = null;
    private RealMatrix mink         = null;
    private int iter                = 2;


    public SME_ENS_EnergyOptimisation(SME_Plugin refplugin){
        sme_plugin = refplugin;
    }

    public double[][] getMaxProjectionIndex(ImageStack imageStack){

        double[][] maxIndex = new double[imageStack.getProcessor(1).getWidth()]
                [imageStack.getProcessor(1).getHeight()];

        for(int pixIndexI=0;pixIndexI<=imageStack.getProcessor(1).getWidth();pixIndexI++){
            for(int pixIndexJ=0;pixIndexJ<=imageStack.getProcessor(1).getWidth();pixIndexJ++){
                double maxZval = 0;

                for(int pixIndexZ=0;pixIndexZ<=imageStack.getSize();pixIndexZ++){
                    if(maxZval>imageStack.getVoxel(pixIndexI,pixIndexJ,pixIndexZ)){
                        maxIndex[pixIndexI][pixIndexJ] = pixIndexZ;
                    }
                }
            }
        }

        return(maxIndex);
    }

    public void initOptimisation(){
        RealMatrix edgeflag = MatrixUtils.createRealMatrix(sme_plugin.getKmeanCentroids());
        RealMatrix edgeflag2  = edgeflag.scalarAdd(-1).scalarMultiply(1/KMEAN_NORM);
        kmeanOutput = getMaxProjectionIndex(sme_plugin.getStack1());

        idmax       = MatrixUtils.createRealMatrix(kmeanOutput);
        idmaxk      = idmax.copy();
        idmaxki     = idmax.copy();
        mink        = idmax.copy().scalarMultiply(0).scalarAdd(1);

        optStep     = sme_plugin.getStack1().getSize()/(double)stepNumber;
        cost     = MatrixUtils.createRealMatrix(new double[1][stepNumber]);
        cost.setEntry(0,1,100);cost.setEntry(0,2,10);
    }

    public void applyEnergyOptimisation(){
        RealMatrix idmax1 = idmaxk.copy();
        RealMatrix idmax2 = idmaxk.copy();
        RealMatrix idmaxkB,IB = null;
        ZProjector zproject = new ZProjector();
        zproject.setMethod(0);

        while(Math.abs(cost.getEntry(0,iter)-cost.getEntry(0,(iter-1)))>(ENERGY_STEP)){
            iter++;
            idmax1 = idmaxk.scalarAdd(ENERGY_STEP).copy();
            idmax2 = idmaxk.scalarAdd(-ENERGY_STEP).copy();

            idmaxkB = padSymetricMatrix(idmaxk,true);
            IB      = padSymetricMatrix(idmaxkB,true);

            ImageStack base    =    find_base(IB,3);
            zproject.setImage(new ImagePlus("IterativeProjection",base));
            zproject.doProjection();
            RealMatrix Mold    =    MatrixUtils.createRealMatrix(
                    SME_ENS_Utils.convertFloatMatrixToDoubles(
                            zproject.getProjection().getImageStack().getProcessor(1).getFloatArray(),
                    idmax1.getRowDimension(),idmax1.getColumnDimension()));
        }

        /**
         varold2=sum((base-repmat(Mold,[1 1 8])).^2,3);

         d1=abs(idmax-idmax1).*edgeflag2;
         d2=abs(idmax-idmax2).*edgeflag2;
         d0=abs(idmax-idmaxk).*edgeflag2;

         M11=idmax1-Mold;
         M12=idmax2-Mold;
         M10=idmaxk-Mold;
         */
    }

    // TODO Check correspondence of java and ImageStack index for all for loops where
    // a ImageStack is accessed

    public RealMatrix padSymetricMatrix(RealMatrix inMatrix, Boolean transMatr){
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

    public RealMatrix stack2matrix(ImageStack imStack){

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

    public ImageStack find_base(RealMatrix inMatrix, int indexK){
        ImageStack baseMatrix           = null;

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
}