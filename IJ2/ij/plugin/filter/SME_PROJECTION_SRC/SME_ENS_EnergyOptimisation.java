package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.plugin.ZAxisProfiler;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.awt.*;

/**
 * Created by rexhepaj on 17/03/16.
 */
public class SME_ENS_EnergyOptimisation {
    private SME_Plugin_Get_Manifold sme_pluginGetManifold = null;
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
    private RealVector cost         = null;
    private RealMatrix movmat       = null;
    private RealMatrix mink         = null;
    private int iter                = 1;
    private RealMatrix edgeflag     = null;
    private RealMatrix edgeflag2    = null;
    private int maxiter             = 1000;
    private RealMatrix  valk        = null;
    private RealVector  psiVector   = null;
    private double KE = 0;
    private RealVector foregroundPixelVal     = null;

    public SME_ENS_EnergyOptimisation(SME_Plugin_Get_Manifold refplugin){
        sme_pluginGetManifold = refplugin;
        initOptimisation();
    }

    public void initOptimisation(){
        ImagePlus   smlProjection = sme_pluginGetManifold.getSmlImage();
        kmeanOutput = MatrixUtils.createRealMatrix(SME_ENS_Utils.convertFloatMatrixToDoubles(sme_pluginGetManifold.getKmensImage().getProcessor().getFloatArray(),
                smlProjection.getWidth(),smlProjection.getHeight()));
        kmeanOutput = kmeanOutput.transpose();

        edgeflag = kmeanOutput;
        //SME_ENS_Utils.printRealMatrix(edgeflag.getData());
        double normFactor = 1.0/KMEAN_NORM;
        edgeflag2  = MatrixUtils.createRealMatrix(edgeflag.scalarMultiply(normFactor).getData());
        //SME_ENS_Utils.printRealMatrix(edgeflag2.getData());
        //SME_ENS_Utils.printRealMatrixStats(edgeflag2,"edgeflag2");
        //[valk,idmax]=max(timk,[],3);
        idmax       = SME_ENS_Utils.getMaxProjectionIndex(smlProjection.getImageStack()).scalarAdd(1);


        ZProjector zproject = new ZProjector();
        zproject.setMethod(ZProjector.MAX_METHOD);
        zproject.setImage(new ImagePlus("IterativeProjection", smlProjection.getImageStack()));
        zproject.doProjection();

        valk =   MatrixUtils.createRealMatrix(
                SME_ENS_Utils.convertFloatMatrixToDoubles(
                        zproject.getProjection().getImageStack().getProcessor(1).getFloatArray(),
                        smlProjection.getImageStack().getWidth(), smlProjection.getImageStack().getHeight()));
        valk            =   valk.transpose();

        initStepEnergyOpt();
        initWparam();

        // save tmp sml max projection and kmeans projection

        IJ.saveAsTiff(new ImagePlus("SML_Projection",smlProjection.getImageStack()),"smlResult.tiff");
        IJ.saveAsTiff(sme_pluginGetManifold.getKmensImage(),"kmeansResult.tiff");

        //SME_ENS_Utils.printRealMatrix(idmax.getData(),"idmax");
        SME_ENS_Utils.printRealMatrixStats(idmax,"idmax");

        idmaxk      = idmax.copy();
        idmaxki     = idmax.copy();
        mink        = idmax.copy().scalarMultiply(0).scalarAdd(1);

        // TODO code function to automatically update the step size

        cost     = MatrixUtils.createRealVector(new double[2]);
        cost.setEntry(0,100);cost.setEntry(1,10);
    }

    public void initWparam(){

    }

    public void initStepEnergyOpt(){
        foregroundPixelVal = MatrixUtils.createRealVector(new double[0]);

        for(int i=0;i<edgeflag2.getRowDimension();i++){
            for(int j=0;j<edgeflag2.getColumnDimension();j++){
                if(edgeflag2.getEntry(i,j)>0){
                    foregroundPixelVal.append(idmax.getEntry(i,j));
                }
            }
        }


        KE= foregroundPixelVal.getMaxValue()-foregroundPixelVal.getMaxValue()+1;
        step=KE/100;

        step     = sme_pluginGetManifold.getStack1().getSize()/(double)stepNumber;
    }

    public void computePSIprojection(){

    }

    public void applyEnergyOptimisation() {
        RealMatrix idmax1 = idmaxk.copy();
        RealMatrix idmax2 = idmaxk.copy();
        RealMatrix idmaxkB, IB = null;
        ZProjector zproject = new ZProjector();
        zproject.setMethod(0);

        while (Math.abs(cost.getEntry(iter) - cost.getEntry((iter - 1))) > (ENERGY_STEP)) {

            if(iter>=maxiter){
                break;
            }

            iter++;
            idmax1 = idmaxk.scalarAdd(step).copy();
            idmax2 = idmaxk.scalarAdd(-step).copy();

            idmaxkB = SME_ENS_Utils.padSymetricMatrix(idmaxk, Boolean.TRUE);
            IB = SME_ENS_Utils.padSymetricMatrix(idmaxkB, Boolean.TRUE);

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

            RealMatrix d1 = SME_ENS_Utils.elementMultiply(idmax.subtract(idmax1),edgeflag2,Boolean.TRUE);
            RealMatrix d2 = SME_ENS_Utils.elementMultiply(idmax.subtract(idmax2),edgeflag2,Boolean.TRUE);
            RealMatrix d0 = SME_ENS_Utils.elementMultiply(idmax.subtract(idmaxk),edgeflag2,Boolean.TRUE);

            RealMatrix M11 = idmax1.subtract(Mold);
            RealMatrix M12 = idmax2.subtract(Mold);
            RealMatrix M10 = idmaxk.subtract(Mold);

            RealMatrix s1 = SME_ENS_Utils.realmatrixDoublepow(varold2.add(
                    SME_ENS_Utils.elementMultiply(M11, idmax1.subtract(Mold.add(M11.scalarMultiply(1 / (double) 9))),Boolean.FALSE)
            ).scalarMultiply(1 / (double) 8), 0.5).scalarMultiply(9);
            RealMatrix s2 = SME_ENS_Utils.realmatrixDoublepow(varold2.add(
                    SME_ENS_Utils.elementMultiply(M12, idmax2.subtract(Mold.add(M12.scalarMultiply(1 / (double) 9))),Boolean.FALSE)
            ).scalarMultiply(1 / (double) 8), 0.5).scalarMultiply(9);
            RealMatrix s0 = SME_ENS_Utils.realmatrixDoublepow(varold2.add(
                    SME_ENS_Utils.elementMultiply(M10, idmaxk.subtract(Mold.add(M10.scalarMultiply(1 / (double) 9))),Boolean.FALSE)
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
            cost = cost.append(costIterStep);
            step = step*0.99;

            System.out.println(Integer.toString(iter));
            System.out.println(Double.toString(costIterStep));

            IJ.showStatus("ENS PLUGIN ENERGY OPTIMISATION - STEP :: "+
                    Integer.toString(iter) + " - COST = " + Double.toString(costIterStep));
        }

        sme_pluginGetManifold.setCostData(SME_ENS_Utils.realvec2Stack(cost));
        sme_pluginGetManifold.setSmePlotmaker(new SME_Data_Profiler());
        ((SME_Data_Profiler) sme_pluginGetManifold.getSmePlotmaker()).run(new ImagePlus("Cost data",sme_pluginGetManifold.getCostData()));
        RealVector xVal = MatrixUtils.createRealVector(new double[cost.getDimension()]);
        RealVector yVal = cost.add(cost);

        for(int j=0;j<xVal.getDimension();j++){
            xVal.setEntry(j,(double) j);
        }

        //((SME_Data_Profiler) sme_pluginGetManifold.getSmePlotmaker()).getPlot().addPoints(xVal.toArray(),yVal.toArray(), Plot.LINE);
        //((SME_Data_Profiler) sme_pluginGetManifold.getSmePlotmaker()).getPlot().addLegend("Legend Plot");
    }

    public void setOutputManifold(){
        double norm_factor  =   sme_pluginGetManifold.getStack1().getSize();
        int dimW            =   sme_pluginGetManifold.getStack1().getWidth();
        int dimH            =   sme_pluginGetManifold.getStack1().getHeight();

        RealMatrix normMnold = idmaxk.scalarMultiply(1/norm_factor).scalarMultiply(255);
        float[][] mfoldFlaot = SME_ENS_Utils.convertDoubleMatrixToFloat(normMnold.transpose().getData(),dimW,dimH);
        ImagePlus smeManifold = new ImagePlus("",((ImageProcessor) new FloatProcessor(mfoldFlaot)));
        sme_pluginGetManifold.setMfoldImage(smeManifold);
        sme_pluginGetManifold.getMfoldImage().show();
    }

    public void setOutputSME(){
        double norm_factor  =   sme_pluginGetManifold.getStack1().getSize();
        int dimW            =   sme_pluginGetManifold.getStack1().getWidth();
        int dimH            =   sme_pluginGetManifold.getStack1().getHeight();

        ImageStack rawStack  = sme_pluginGetManifold.getStack();
        RealMatrix projMnold = MatrixUtils.createRealMatrix(dimH,dimW);

        for(int i=0;i<dimH;i++){
            for(int j=0;j<dimW;j++){
                int zIndex = ((int) Math.round(idmaxk.getEntry(i,j)))-1;
                projMnold.setEntry (i,j,rawStack.getVoxel(j,i,zIndex));
            }
        }

        float[][] mfoldFlaot = SME_ENS_Utils.convertDoubleMatrixToFloat(projMnold.transpose().getData(),dimW,dimH);
        ImagePlus smeManifold = new ImagePlus("",((ImageProcessor) new FloatProcessor(mfoldFlaot)));
        sme_pluginGetManifold.setSmeImage(smeManifold);
        sme_pluginGetManifold.getSmeImage().show();
    }

}