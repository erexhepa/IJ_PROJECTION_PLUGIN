package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Created by rexhepaj on 17/03/16.
 */
public class SME_ENS_EnergyOptimisation {
    private SME_Plugin sme_plugin   = null;
    private final int KMEAN_NORM    = 2;
    private final double ENERGY_STEP= 0.0001;
    private RealMatrix kmeanOutput ;
    private RealMatrix rawdata2D    = null;
    private RealMatrix tmpProcess   = null;
    private double step ;

    private int stepNumber          = 100;
    private RealMatrix idmax        = null;
    private RealMatrix idmaxk       = null;
    private RealMatrix idmaxki      = null;
    private RealMatrix cost         = null;
    private RealMatrix movmat       = null;
    private RealMatrix mink         = null;
    private int iter                = 2;
    private RealMatrix edgeflag     = null;
    private RealMatrix edgeflag2    = null;
    private int maxiter             = 1000;

    public SME_ENS_EnergyOptimisation(SME_Plugin refplugin){
        sme_plugin = refplugin;
        initOptimisation();
    }

    public void initOptimisation(){
        ImagePlus   smlProjection = sme_plugin.getSmlImage();
        kmeanOutput = MatrixUtils.createRealMatrix(SME_ENS_Utils.convertFloatMatrixToDoubles(sme_plugin.getKmensImage().getProcessor().getFloatArray(),
                smlProjection.getWidth(),smlProjection.getHeight()));
        kmeanOutput = kmeanOutput.transpose();

        edgeflag = kmeanOutput;
        //SME_ENS_Utils.printRealMatrix(edgeflag.getData());
        double normFactor = 1.0/KMEAN_NORM;
        edgeflag2  = MatrixUtils.createRealMatrix(edgeflag.scalarMultiply(normFactor).getData());
        //SME_ENS_Utils.printRealMatrix(edgeflag2.getData());
        //SME_ENS_Utils.printRealMatrixStats(edgeflag2,"edgeflag2");

        idmax       = SME_ENS_Utils.getMaxProjectionIndex(smlProjection.getImageStack()).scalarAdd(1);

        // save tmp sml max projection and kmeans projection

        IJ.saveAsTiff(new ImagePlus("SML_Projection",smlProjection.getImageStack()),"smlResult.tiff");
        IJ.saveAsTiff(sme_plugin.getKmensImage(),"kmeansResult.tiff");

        //SME_ENS_Utils.printRealMatrix(idmax.getData(),"idmax");
        SME_ENS_Utils.printRealMatrixStats(idmax,"idmax");

        idmaxk      = idmax.copy();
        idmaxki     = idmax.copy();
        mink        = idmax.copy().scalarMultiply(0).scalarAdd(1);

        step     = sme_plugin.getStack1().getSize()/(double)stepNumber;
        cost     = MatrixUtils.createRealMatrix(new double[1][maxiter]);
        cost.setEntry(0,1,100);cost.setEntry(0,2,10);
    }

    public void applyEnergyOptimisation() {
        RealMatrix idmax1 = idmaxk.copy();
        RealMatrix idmax2 = idmaxk.copy();
        RealMatrix idmaxkB, IB = null;
        ZProjector zproject = new ZProjector();
        zproject.setMethod(0);

        while (Math.abs(cost.getEntry(0, iter) - cost.getEntry(0, (iter - 1))) > (ENERGY_STEP)) {
            if(iter>=maxiter){
                break;
            }

            iter++;
            idmax1 = idmaxk.scalarAdd(step).copy();
            idmax2 = idmaxk.scalarAdd(-step).copy();

            idmaxkB = SME_ENS_Utils.padSymetricMatrix(idmaxk, true);
            IB = SME_ENS_Utils.padSymetricMatrix(idmaxkB, true);

            ImageStack base = SME_ENS_Utils.find_base(IB, 3);
            zproject.setImage(new ImagePlus("IterativeProjection", base));
            zproject.setMethod(0);
            zproject.doProjection();
            int nrowsIB     = base.getHeight();
            int ncolsIB     = base.getWidth();

            //TODO remove this initialisation of Imagestac
            //ImageStack zprojStack = zproject.getProjection().getImageStack();
            //float[][] dataArray   = zproject.getProjection().getProcessor().getFloatArray();

            RealMatrix Mold =   MatrixUtils.createRealMatrix(
                    SME_ENS_Utils.convertFloatMatrixToDoubles(
                            zproject.getProjection().getImageStack().getProcessor(1).getFloatArray(),
                            ncolsIB, nrowsIB));
            Mold            =   Mold.transpose();

            ImageStack varoldStack = SME_ENS_Utils.repmatMatrixVar(Mold, base);

            zproject.setImage(new ImagePlus("IterativeProjection", varoldStack));
            zproject.setMethod(3);
            zproject.doProjection();
            int rowDim = varoldStack.getHeight();
            int colDim = varoldStack.getWidth();

            RealMatrix varold2 = MatrixUtils.createRealMatrix(
                    SME_ENS_Utils.convertFloatMatrixToDoubles(
                            zproject.getProjection().getImageStack().getProcessor(1).getFloatArray(),
                            colDim, rowDim));
            varold2             = varold2.transpose();

            RealMatrix d1 = SME_ENS_Utils.elementMultiply(idmax.subtract(idmax1),edgeflag2,true);
            RealMatrix d2 = SME_ENS_Utils.elementMultiply(idmax.subtract(idmax2),edgeflag2,true);
            RealMatrix d0 = SME_ENS_Utils.elementMultiply(idmax.subtract(idmaxk),edgeflag2,true);

            RealMatrix M11 = idmax1.subtract(Mold);
            RealMatrix M12 = idmax2.subtract(Mold);
            RealMatrix M10 = idmaxk.subtract(Mold);

            RealMatrix s1 = SME_ENS_Utils.realmatrixDoublepow(varold2.add(
                    SME_ENS_Utils.elementMultiply(M11, idmax1.subtract(Mold.add(M11.scalarMultiply(1 / (double) 9))),false)
            ).scalarMultiply(1 / (double) 8), 0.5).scalarMultiply(9);
            RealMatrix s2 = SME_ENS_Utils.realmatrixDoublepow(varold2.add(
                    SME_ENS_Utils.elementMultiply(M12, idmax2.subtract(Mold.add(M12.scalarMultiply(1 / (double) 9))),false)
            ).scalarMultiply(1 / (double) 8), 0.5).scalarMultiply(9);
            RealMatrix s0 = SME_ENS_Utils.realmatrixDoublepow(varold2.add(
                    SME_ENS_Utils.elementMultiply(M10, idmaxk.subtract(Mold.add(M10.scalarMultiply(1 / (double) 9))),false)
            ).scalarMultiply(1 /(double) 8), 0.5).scalarMultiply(9);

            RealMatrix c1 = d1.add(s1);
            RealMatrix c2 = d2.add(s2);
            RealMatrix c0 = d0.add(s0);

            ImageStack catStack = new ImageStack( c0.getColumnDimension() , c0.getRowDimension());
            catStack = SME_ENS_Utils.realmatrixCat(
                    SME_ENS_Utils.realmatrixCat(
                            SME_ENS_Utils.realmatrixCat(catStack,
                                    c0), c1), c2);

            zproject.setImage(new ImagePlus("IterativeProjection", catStack));
            zproject.setMethod(2);
            zproject.doProjection();
            RealMatrix minc = MatrixUtils.createRealMatrix(
                    SME_ENS_Utils.convertFloatMatrixToDoubles(
                            zproject.getProjection().getImageStack().getProcessor(1).getFloatArray(),
                            colDim, rowDim));
            minc    =   minc.transpose();

            RealMatrix shiftc   =   SME_ENS_Utils.getMinProjectionIndex(catStack);
            //shiftc              =   shiftc.transpose();
            //shiftc              =   shiftc.scalarAdd(-1);
            SME_ENS_Utils.replaceRealmatElements(shiftc,1,step);
            SME_ENS_Utils.replaceRealmatElements(shiftc,2,-step);

            idmaxk  =   idmaxk.add(shiftc);
            RealVector costIter = SME_ENS_Utils.realmat2vector(minc,0);
            double costIterStep = costIter.getL1Norm()/(minc.getRowDimension()*minc.getColumnDimension());
            cost.setEntry(0,iter,costIterStep);

            step=step*0.99;

            System.out.println(Integer.toString(iter));
            System.out.println(Double.toString(costIterStep));
        }
    }

    public void setOutputManifold(){
        double norm_factor  =   sme_plugin.getStack1().getSize();
        int dimW            =   sme_plugin.getStack1().getWidth();
        int dimH            =   sme_plugin.getStack1().getHeight();

        RealMatrix normMnold = idmaxk.scalarMultiply(1/norm_factor).scalarMultiply(255);
        float[][] mfoldFlaot = SME_ENS_Utils.convertDoubleMatrixToFloat(normMnold.getData(),dimH,dimW);
        ImagePlus smeManifold = new ImagePlus("",((ImageProcessor) new FloatProcessor(mfoldFlaot)));
        sme_plugin.setMfoldImage(smeManifold);
    }

    public void setOutputSME(){
        double norm_factor  =   sme_plugin.getStack1().getSize();
        int dimW            =   sme_plugin.getStack1().getWidth();
        int dimH            =   sme_plugin.getStack1().getHeight();

        ImageStack rawStack  = sme_plugin.getStack();
        RealMatrix projMnold = MatrixUtils.createRealMatrix(dimH,dimW);

        for(int i=0;i<dimH;i++){
            for(int j=0;j<dimW;j++){
                int zIndex = (int) Math.round(idmaxk.getEntry(i,j));
                projMnold.setEntry (i,j,rawStack.getVoxel(j,i,zIndex));
            }
        }

        float[][] mfoldFlaot = SME_ENS_Utils.convertDoubleMatrixToFloat(projMnold.getData(),dimH,dimW);
        ImagePlus smeManifold = new ImagePlus("",((ImageProcessor) new FloatProcessor(mfoldFlaot)));
        sme_plugin.setSmeImage(smeManifold);
    }

}