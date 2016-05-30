package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static java.util.stream.Collectors.toList;

/**
 * Created by rexhepaj on 22/04/16.
 */
public class SME_Plugin_Simple implements PlugIn {
    private ImagePlus imp;
    private ImagePlus rawImage;
    private ImagePlus projectedImage;
    private ImagePlus manifold;
    private ImagePlus[] images;

    private static String none = "*None*";
    private static int maxChannels = 4;
    private static int nmbChannels = 3;
    private static String[] colors = {"Channel1 stack1 to appy the manifold",
            "Channel2 stack1 to appy the manifold",
            "Channel3 stack1 to appy the manifold",
            "Channel4 stack1 to appy the manifold"};

    private static boolean staticCreateComposite = true;
    private static boolean staticKeep;
    private static boolean staticIgnoreLuts;

    private byte[] blank;
    private boolean ignoreLuts;
    private boolean autoFillDisabled;
    private String firstChannelName;
    private ImagePlus[] projectionStacks ;
    private int stackSize = 0;
    private int width = 0;
    private int height = 0;
    private SME_Plugin_Get_Manifold smePlugin;

    public void run(String arg) {
        processChannelsManifold();
    }

    public void getManifold(){
        smePlugin = new SME_Plugin_Get_Manifold();
        smePlugin.initProgressBar();

        smePlugin.setup("Manifold channel",images[0]);
        smePlugin.runSimple(false);


        //IJ.showStatus("Running SML");
        runSmlStep();
        smePlugin.updateProgressbar(0.1);
        //IJ.showStatus("Running KMEANS");
        runKmeansStep();
        smePlugin.updateProgressbar(0.3);
        //IJ.showStatus("Running Energy Optimisation");
        runEnoptStep();
        smePlugin.updateProgressbar(1);
        //IJ.showStatus("Finished");
    }

    public void runSmlStep(){
        smePlugin.runSml(false);

    }

    public void runKmeansStep(){
        smePlugin.runKmeans(false);

    }

    public void runEnoptStep(){
        smePlugin.runEnergyOptimisation(false);
    }

    /** Combines up to seven grayscale stacks into one RGB or composite stack. */
    public void processChannelsManifold() {
        int[] wList         = WindowManager.getIDList();
        projectionStacks    = new ImagePlus[maxChannels];

        if (wList==null) {
            error("No images are open.");
            return;
        }else if(wList.length<1){
            error("There must be at least two images open: 1) Stack to project and 2) Manifold");
            return;
        }

        String[] titles = new String[wList.length+1];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            titles[i] = imp!=null?imp.getTitle():"";
        }

        titles[wList.length] = none;
        String[] names = getInitialNamesAllstacks(titles);
        boolean createComposite = staticCreateComposite;
        boolean keep = staticKeep;
        ignoreLuts = staticIgnoreLuts;

        String options = Macro.getOptions();
        boolean macro = IJ.macroRunning() && options!=null;
        if (macro) {
            createComposite = keep = ignoreLuts = false;
            options = options.replaceAll("red=", "c1=");
            options = options.replaceAll("green=", "c2=");
            options = options.replaceAll("blue=", "c3=");
            options = options.replaceAll("gray=", "c4=");
            Macro.setOptions(options);
        }

        GenericDialog gd = new GenericDialog("SME APPLY MANIFOLD TO OPEN STACKS");
        gd.addChoice("Channel 0 : Extract manifold  : (uint8 0-255)", titles, macro?none:names[0]);
        gd.addChoice("Channel 1 : Apply manifold (Mapped to RED in composite)", titles, macro?none:names[1]);
        gd.addChoice("Channel 2 : Apply manifold (Mapped to GREEN in composite)", titles, macro?none:names[2]);
        gd.addChoice("Channel 3 : Apply manifold (Mapped to Blue in composite)", titles, macro?none:names[3]);

        //gd.addCheckbox("Create composite", createComposite);
        //gd.addCheckbox("Keep source images", keep);
        //gd.addCheckbox("Ignore source LUTs", ignoreLuts);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        int[] index = new int[maxChannels];

        for (int i=0; i<maxChannels; i++) {
            index[i] = gd.getNextChoiceIndex();
        }

        images = new ImagePlus[maxChannels];

        stackSize = 0;
        width = 0;
        height = 0;

        int slices = 0;
        int frames = 0;
        for (int i=0; i<maxChannels; i++) {

            //IJ.log(i+"  "+index[i]+"	"+titles[index[i]]+"  "+wList.length);
            if (index[i]<wList.length) {
                images[i] = WindowManager.getImage(wList[index[i]]);
                if(width<images[i].getWidth()){width = images[i].getWidth();}
                if(height<images[i].getHeight()){height = images[i].getHeight();}
                if(stackSize<images[i].getStackSize()){stackSize = images[i].getStackSize();}
            }
        }

        if (width==0) {
            error("There must be at least one source image or stack.");
            return;
        }

        // run manifold extraction on the first channel
        getManifold();

        ImagePlus manifoldModel = smePlugin.getMfoldImage();

        boolean mergeHyperstacks = false;

        ArrayList<ImagePlus> listChannels = new ArrayList<>(1);
        for(int i=0; i<maxChannels; i++){
            if(images[i]==null) break;
            listChannels.add(images[i]);
        }

        /**List<ImagePlus> processedImages = listChannels.stream().
                map(channelIt ->{
                    ImagePlus itIm =  applyStackManifold(((ImagePlus)channelIt).getStack(), manifoldModel);
                    //itIm.show();
                    return itIm;})
                .collect(toList());**/

        ForkJoinPool forkJoinPool = new ForkJoinPool(8);
        CompletableFuture<List<ImagePlus>> processedImages =  CompletableFuture.supplyAsync(()->

                listChannels.parallelStream().
                map(channelIt ->{
                  ImagePlus itIm =  applyStackManifold(((ImagePlus)channelIt).getStack(), manifoldModel);
                  //itIm.show();
                  return itIm;})
                .collect(toList()),
                forkJoinPool
        );
    }

    private String[] getInitialNamesAllstacks(String[] titles) {
        String[] names = new String[maxChannels];
        for (int i=0; i<maxChannels; i++)
            names[i] = getNameStack(i+1, titles);
        return names;
    }

    private String getNameStack(int channel, String[] titles) {
        if (autoFillDisabled)
            return none;
        String str = "C"+channel;
        String name = null;
        for (int i=titles.length-1; i>=0; i--) {
            if (titles!=null && titles[i].startsWith(str) && (firstChannelName==null||titles[i].contains(firstChannelName))) {
                name = titles[i];
                if (channel==1)
                    firstChannelName = name.substring(3);
                break;
            }
        }
        if (name==null) {
            for (int i=titles.length-1; i>=0; i--) {
                int index = titles[i].indexOf(colors[channel-1]);
                if (titles!=null && index!=-1 && (firstChannelName==null||titles[i].contains(firstChannelName))) {
                    name = titles[i];
                    if (channel==1 && index>0)
                        firstChannelName = name.substring(0, index-1);
                    break;
                }
            }
        }
        if (channel==1 && name==null)
            autoFillDisabled = true;
        if (name!=null)
            return name;
        else
            return none;
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

    void error(String msg) {
        IJ.error("Merge Channels", msg);
    }
}
