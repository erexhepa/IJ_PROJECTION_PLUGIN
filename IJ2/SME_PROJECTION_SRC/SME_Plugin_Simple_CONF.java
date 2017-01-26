package SME_PROJECTION_SRC;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by rexhepaj on 22/04/16.
 */
public class SME_Plugin_Simple_CONF implements PlugIn {
    private ImagePlus imp;
    private ImagePlus rawImage;
    private ImagePlus projectedImage;
    private ImagePlus manifold;
    private ImagePlus[] images;

    private static String none = "Red channel";
    private int maxChannels = 4;
    private int nmbChannels = 3;
    private static String[] colors = {"Channel1 stack1 to appy the manifold",
            "Channel2 stack1 to appy the manifold",
            "Channel3 stack1 to appy the manifold",
            "Channel4 stack1 to appy the manifold"};

    private static boolean staticCreateComposite = true;
    private static boolean staticKeep;
    private static boolean staticIgnoreLuts;
    private ImagePlus manifoldModel;
    private byte[] blank;
    private boolean ignoreLuts;
    private boolean autoFillDisabled;
    private String firstChannelName;
    private ImagePlus[] projectionStacks ;
    private int stackSize = 0;
    private int width = 0;
    private int height = 0;

    private int lowBuffManifold = 0;
    private int highBuffManifold = 0;

    private SME_Plugin_Get_Manifold smePlugin;


    public void run(String arg) {
        processChannelsManifold();
    }

    public void getManifold(int indexChannel){
        smePlugin = new SME_Plugin_Get_Manifold();

        smePlugin.setLowBuffManifold(this.getLowBuffManifold());
        smePlugin.setHighBuffManifold(this.getHighBuffManifold());

        smePlugin.initProgressBar();

        smePlugin.setup("Manifold channel",images[indexChannel]);
        smePlugin.runSimple(false);

        //IJ.showStatus("Running SML");
        runSmlStep();
        smePlugin.updateProgressbar(0.1);
        //IJ.showStatus("Running KMEANS");
        runKmeansStep();
        smePlugin.updateProgressbar(0.3);
        //IJ.showStatus("Running Energy Optimisation");
        runEnoptStep();

        //IJ.showStatus("Finished");
    }

    public void runSmlStep(){
        smePlugin.runSml(false,false);

    }

    public void runKmeansStep(){
        smePlugin.runKmeans(false);

    }

    public void runEnoptStep(){
        smePlugin.runEnergyOptimisation(false);
    }

    /** Combines up to seven grayscale stacks into one RGB or composite stack. */
    public void processChannelsManifold() {

        if (WindowManager.getCurrentImage()==null) {
            error("No images is selected.");
            return;
        }

        if (WindowManager.getCurrentImage().getBitDepth()==24) {
            error("SME Pluing has detected a stack of RGB images, Please change to monochromatic images or hyper stack");
            return;
        }

        if(WindowManager.getCurrentImage().isHyperStack()){
            // hyperstack color
            try {
                processChannelsManifoldColors();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }else{
            // monochromatic image
            processChannelsManifoldSimple();
        }
    }

    public void setLowBuffManifold(int lowBuffManifold) {
        this.lowBuffManifold = lowBuffManifold;
    }

    public int getLowBuffManifold() {
        return lowBuffManifold;
    }

    public int getHighBuffManifold() {
        return highBuffManifold;
    }

    public void setHighBuffManifold(int highBuffManifold) {
        this.highBuffManifold = highBuffManifold;
    }

    public void processChannelsManifoldSimple() {
        images = new ImagePlus[1];
        images[0] = WindowManager.getCurrentImage();

        stackSize = 0;
        width = 0;
        height = 0;

        int slices = 0;
        int frames = 0;
        int i ;

        {
                i = 0;
                if(width<images[i].getWidth()){width = images[i].getWidth();}
                if(height<images[i].getHeight()){height = images[i].getHeight();}
                if(stackSize<images[i].getStackSize()){stackSize = images[i].getStackSize();}
        }


        if (width==0) {
            error("There must be at least one source image or stack.");
            return;
        }

        GenericDialog gd = new GenericDialog("SME Stacking");

        gd.addMessage("Image type :");
        String[] channelMessage = new String[1];
        channelMessage[0] = "single channel";

        gd.addChoice("Extract manifold from ",channelMessage,"single channel");

        gd.addSlider("How many layers to add below manifold ?",0,images[0].getStackSize(),0);
        gd.addSlider("How many layers to add above manifold ?",0,images[0].getStackSize(),0);
        //gd.addCheckbox("Keep source images", keep);
        //gd.addCheckbox("Ignore source LUTs", ignoreLuts);
        gd.showDialog();

        this.setLowBuffManifold(((Scrollbar)gd.getSliders().elementAt(0)).getValue());
        this.setHighBuffManifold(((Scrollbar)gd.getSliders().elementAt(1)).getValue());

        if (gd.wasCanceled())
            return;

        // run manifold extraction on the first channel
        getManifold(0);

        manifoldModel = smePlugin.getMfoldImage();
        //manifoldModel.show();
        smePlugin.getSmeImage().show();
        smePlugin.getSmeImage().setTitle("SME PROJECTION - CONFOCAL");
        smePlugin.updateProgressbar(1);
    }

    public void processChannelsManifoldColors() throws NoSuchMethodException {
        ImagePlus hyperStackSME = WindowManager.getCurrentImage();
        images = ChannelSplitter.split(hyperStackSME);

        // get channel color ids
        Color[]  colors1 = new Color[images.length];
        for(int i=0;i<images.length;i++){
            hyperStackSME.setC(i+1);
            colors1[i] = (((CompositeImage) hyperStackSME).getChannelColor());
        }

        maxChannels         = images.length;
        projectionStacks    = new ImagePlus[maxChannels];
        String[] titles = new String[images.length];

        for(int i=0;i<images.length;i++){
            if(colors1[i].equals(Color.RED))
                titles[i] = "Channel-RED";
            else if(colors1[i].equals(Color.GREEN))
                titles[i] = "Channel-GREEN";
            else if(colors1[i].equals(Color.BLUE))
                titles[i] = "Channel-BLUE";
            else if(colors1[i].equals(Color.GRAY))
                titles[i] = "Channel-GRAY";
            else if(colors1[i].equals(Color.CYAN))
                titles[i] = "Channel-CYAN";
            else if(colors1[i].equals(Color.MAGENTA))
                titles[i] = "Channel-MAGENTA";
            else if(colors1[i].equals(Color.yellow))
                titles[i] = "Channel-YELLOW";
        }

        String[] names = titles;
        //ignoreLuts = staticIgnoreLuts;

        GenericDialog gd = new GenericDialog("SME Stacking");
        gd.addChoice("Extract manifold from", titles, titles[0]);

        gd.addSlider("How many layers to add above manifold ?",0,images[0].getStackSize(),0);
        gd.addSlider("How many layers to add below manifold ?",0,images[0].getStackSize(),0);
        //gd.addCheckbox("Keep source images", keep);
        //gd.addCheckbox("Ignore source LUTs", ignoreLuts);
        gd.showDialog();

        this.setLowBuffManifold(((Scrollbar)gd.getSliders().elementAt(0)).getValue());
        this.setHighBuffManifold(((Scrollbar)gd.getSliders().elementAt(1)).getValue());

        if (gd.wasCanceled())
            return;

        int index = gd.getNextChoiceIndex();
        //images = new ImagePlus[maxChannels];

        stackSize = 0;
        width = 0;
        height = 0;

        int slices = 0;
        int frames = 0;

        stackSize = images[0].getStackSize();

        for(int i=0;i<images.length;i++) {
            Object pixVal = images[i].getStack().getPixels(1);
            Method setType = images[i].getStack().getClass().getDeclaredMethod("setType", Object.class);
            setType.setAccessible(true);
            try {
                setType.invoke(images[i].getStack(), pixVal);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        //for(int i=0;i<images.length;i++){
        //    Object pixVal = images[i].getStack().getPixels(1);
        //    images[i].getStack().setType(pixVal);
        //}

        // run manifold extraction on the first channel
        getManifold(index);

        manifoldModel = smePlugin.getMfoldImage();
        //manifoldModel.show();
        //smePlugin.getSmeImage().show();

        ArrayList<ImagePlus> listChannels = new ArrayList<>(1);
        for(int i=0; i<maxChannels; i++){
            if(images[i]==null) break;
            listChannels.add(images[i]);
        }

        List<ImagePlus> processedImages = listChannels.stream().
                map(channelIt ->{
                    ImagePlus itIm =  applyStackManifolfWithMarge(smePlugin,channelIt.getImageStack(),
                            smePlugin.getManifoldIndex(),lowBuffManifold,highBuffManifold);
                    return itIm;})
                .collect(toList());

        ImagePlus[] vecChannels = new ImagePlus[images.length];

        for(int i=0; i<processedImages.size(); i++){
            if(images[i]==null) break;
            vecChannels[i]= processedImages.get(i);
        }

        RGBStackMerge channelMerger = new RGBStackMerge();
        ImagePlus mergedHyperstack  = channelMerger.mergeHyperstacks(vecChannels,false);
        LUT[] lutTable = (((CompositeImage)hyperStackSME)).getLuts();
        ((CompositeImage) mergedHyperstack).setLuts(lutTable);

        mergedHyperstack.show();
        mergedHyperstack.setTitle("SME PROJECTION - CONFOCAL");

        /*ForkJoinPool forkJoinPool = new ForkJoinPool(8);
        CompletableFuture<List<ImagePlus>> processedImages =  CompletableFuture.supplyAsync(()->
                        listChannels.parallelStream().
                                map(channelIt ->{
                                    ImagePlus itIm =  applyStackManifold(((ImagePlus)channelIt).getStack(), manifoldModel);
                                    itIm.show();
                                    return itIm;})
                                .collect(toList()),
                forkJoinPool
        );*/

        smePlugin.updateProgressbar(1);
        smePlugin.getSmeImage().setTitle("SME PROJECTION - CONFOCAL");
    }


    public void runConfColour(int index,LUT[] lutTable){
        getManifold(index);

        manifoldModel = smePlugin.getMfoldImage();

        //TODO add comment below to avoid showing the manifold
        //manifoldModel.show();

        //smePlugin.getSmeImage().show();

        ArrayList<ImagePlus> listChannels = new ArrayList<>(1);
        for(int i=0; i<maxChannels; i++){
            if(images[i]==null) break;
            listChannels.add(images[i]);
        }

        List<ImagePlus> processedImages = listChannels.stream().
                map(channelIt ->{
                    //ImagePlus itIm =  applyStackManifold(((ImagePlus)channelIt).getStack(), manifoldModel);
                    ImagePlus itIm =  applyStackManifolfWithMarge(smePlugin,channelIt.getImageStack(),
                            smePlugin.getManifoldIndex(),lowBuffManifold,highBuffManifold);
                    return itIm;})
                .collect(toList());

        ImagePlus[] vecChannels = new ImagePlus[images.length];

        for(int i=0; i<processedImages.size(); i++){
            if(images[i]==null) break;
            vecChannels[i]= processedImages.get(i);
        }

        RGBStackMerge channelMerger = new RGBStackMerge();
        ImagePlus mergedHyperstack  = channelMerger.mergeHyperstacks(vecChannels,false);

        ((CompositeImage) mergedHyperstack).setLuts(lutTable);

        mergedHyperstack.show();
        mergedHyperstack.setTitle("SME PROJECTION - CONFOCAL");

        /*ForkJoinPool forkJoinPool = new ForkJoinPool(8);
        CompletableFuture<List<ImagePlus>> processedImages =  CompletableFuture.supplyAsync(()->

                        listChannels.parallelStream().
                                map(channelIt ->{
                                    ImagePlus itIm =  applyStackManifold(((ImagePlus)channelIt).getStack(), manifoldModel);
                                    itIm.show();
                                    return itIm;})
                                .collect(toList()),
                forkJoinPool
        );*/

        smePlugin.updateProgressbar(1);
        smePlugin.getSmeImage().setTitle("SME PROJECTION - CONFOCAL");
    }

    public void runConfMono(int index){
        // run manifold extraction on the first channel
        getManifold(0);

        manifoldModel = smePlugin.getMfoldImage();
        manifoldModel.show();

        //TODO add comment below to avoid showing the manifold
        //manifoldModel.show();

        smePlugin.getSmeImage().show();
        smePlugin.getSmeImage().setTitle("SME PROJECTION - CONFOCAL");
        smePlugin.updateProgressbar(1);
    }

    public ImagePlus applyStackManifold(ImageStack imStack, ImagePlus manifold){
        int dimW            =   imStack.getWidth();
        int dimH            =   imStack.getHeight();

        RealMatrix projMnold    = MatrixUtils.createRealMatrix(SME_ENS_Utils.convertFloatMatrixToDoubles(manifold.getProcessor().getFloatArray(),dimW,dimH)).transpose();

        for(int j=0;j<dimH;j++){
            for(int i=0;i<dimW;i++){
                int zIndex = ((int) Math.round(stackSize*(projMnold.getEntry(j,i)/255)));
                projMnold.setEntry (j,i,imStack.getVoxel(i,j,zIndex-1));
            }
        }

        float[][] mfoldFlaot = SME_ENS_Utils.convertDoubleMatrixToFloat(projMnold.transpose().getData(),dimW,dimH);
        ImagePlus smeManifold = new ImagePlus("",((ImageProcessor) new FloatProcessor(mfoldFlaot)));

        return(smeManifold);
    }

    public ImagePlus applyStackManifolfWithMarge(SME_Plugin_Get_Manifold sme_pluginGetManifold, ImageStack imStack,
                                                 ImagePlus manifold, int lowLevel, int highLevel){

        double norm_factor  =   sme_pluginGetManifold.getStack1().getSize();
        int dimW            =   sme_pluginGetManifold.getStack1().getWidth();
        int dimH            =   sme_pluginGetManifold.getStack1().getHeight();

        RealMatrix projMnold    = MatrixUtils.createRealMatrix(SME_ENS_Utils.convertFloatMatrixToDoubles(manifold.getProcessor().getFloatArray(),dimW,dimH)).transpose();
        RealMatrix projMnoldRaw    = MatrixUtils.createRealMatrix(SME_ENS_Utils.convertFloatMatrixToDoubles(manifold.getProcessor().getFloatArray(),dimW,dimH)).transpose();

        ImageStack rawStack  = imStack;

        for(int i=0;i<dimH;i++){
            for(int j=0;j<dimW;j++){
                int zIndex = ((int) Math.round(projMnoldRaw.getEntry(i,j)))-1;
                if(lowLevel==0 & highLevel==0){
                    projMnold.setEntry(i,j,rawStack.getVoxel(j,i,zIndex));
                }else {
                    ImageStack voxelData = rawStack.crop(j,i,0,1,1,sme_pluginGetManifold.getStack().getSize());

                    // set to 0 all pixels which fall outside the local manifold-voxel neighboorhood
                    int iterUp = zIndex-highLevel;
                    int iterDown = zIndex+lowLevel;

                    if(iterUp<0)
                        iterUp=0;
                    if(iterDown>=(sme_pluginGetManifold.getStack().getSize()-1))
                        iterDown=sme_pluginGetManifold.getStack().getSize()-1;

                    for(int ivox=0;ivox<iterUp;ivox++){
                        voxelData.setVoxel(0,0,ivox,0);
                    }

                    for(int ivox=iterDown;ivox<sme_pluginGetManifold.getStack().getSize();ivox++){
                        voxelData.setVoxel(0,0,ivox,0);
                    }

                    ZProjector zproject = new ZProjector();
                    zproject.setMethod(ZProjector.MAX_METHOD);
                    zproject.setImage(new ImagePlus("IterativeProjection", voxelData));
                    zproject.doProjection();

                    projMnold.setEntry(i,j,zproject.getProjection().getStack().getVoxel(0,0,0));
                }
            }

        }

        float[][] mfoldFlaot = SME_ENS_Utils.convertDoubleMatrixToFloat(projMnold.transpose().getData(),dimW,dimH);
        ImagePlus smeManifold = new ImagePlus("ProjectionSME",((ImageProcessor) new FloatProcessor(mfoldFlaot)));
        sme_pluginGetManifold.setSmeImage(smeManifold);
        ImagePlus smeManifold2 = new ImagePlus("",((ImageProcessor) new FloatProcessor(mfoldFlaot)));

        return(smeManifold2);
    }

    void error(String msg) {
        IJ.error("Merge Channels", msg);
    }

    public ImagePlus getImp() {
        return imp;
    }

    public void setImp(ImagePlus imp) {
        this.imp = imp;
    }

    public ImagePlus getRawImage() {
        return rawImage;
    }

    public void setRawImage(ImagePlus rawImage) {
        this.rawImage = rawImage;
    }

    public ImagePlus getProjectedImage() {
        return projectedImage;
    }

    public void setProjectedImage(ImagePlus projectedImage) {
        this.projectedImage = projectedImage;
    }

    public ImagePlus getManifold() {
        return manifold;
    }

    public void setManifold(ImagePlus manifold) {
        this.manifold = manifold;
    }

    public ImagePlus[] getImages() {
        return images;
    }

    public void setImages(ImagePlus[] images) {
        this.images = images;
    }

    public static String getNone() {
        return none;
    }

    public static void setNone(String none) {
        SME_Plugin_Simple_CONF.none = none;
    }

    public int getMaxChannels() {
        return maxChannels;
    }

    public void setMaxChannels(int maxChannels) {
        this.maxChannels = maxChannels;
    }

    public int getNmbChannels() {
        return nmbChannels;
    }

    public void setNmbChannels(int nmbChannels) {
        this.nmbChannels = nmbChannels;
    }

    public static String[] getColors() {
        return colors;
    }

    public static void setColors(String[] colors) {
        SME_Plugin_Simple_CONF.colors = colors;
    }

    public static boolean isStaticCreateComposite() {
        return staticCreateComposite;
    }

    public static void setStaticCreateComposite(boolean staticCreateComposite) {
        SME_Plugin_Simple_CONF.staticCreateComposite = staticCreateComposite;
    }

    public static boolean isStaticKeep() {
        return staticKeep;
    }

    public static void setStaticKeep(boolean staticKeep) {
        SME_Plugin_Simple_CONF.staticKeep = staticKeep;
    }

    public static boolean isStaticIgnoreLuts() {
        return staticIgnoreLuts;
    }

    public static void setStaticIgnoreLuts(boolean staticIgnoreLuts) {
        SME_Plugin_Simple_CONF.staticIgnoreLuts = staticIgnoreLuts;
    }

    public ImagePlus getManifoldModel() {
        return manifoldModel;
    }

    public void setManifoldModel(ImagePlus manifoldModel) {
        this.manifoldModel = manifoldModel;
    }

    public byte[] getBlank() {
        return blank;
    }

    public void setBlank(byte[] blank) {
        this.blank = blank;
    }

    public boolean isIgnoreLuts() {
        return ignoreLuts;
    }

    public void setIgnoreLuts(boolean ignoreLuts) {
        this.ignoreLuts = ignoreLuts;
    }

    public boolean isAutoFillDisabled() {
        return autoFillDisabled;
    }

    public void setAutoFillDisabled(boolean autoFillDisabled) {
        this.autoFillDisabled = autoFillDisabled;
    }

    public String getFirstChannelName() {
        return firstChannelName;
    }

    public void setFirstChannelName(String firstChannelName) {
        this.firstChannelName = firstChannelName;
    }

    public ImagePlus[] getProjectionStacks() {
        return projectionStacks;
    }

    public void setProjectionStacks(ImagePlus[] projectionStacks) {
        this.projectionStacks = projectionStacks;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public SME_Plugin_Get_Manifold getSmePlugin() {
        return smePlugin;
    }

    public void setSmePlugin(SME_Plugin_Get_Manifold smePlugin) {
        this.smePlugin = smePlugin;
    }


}
