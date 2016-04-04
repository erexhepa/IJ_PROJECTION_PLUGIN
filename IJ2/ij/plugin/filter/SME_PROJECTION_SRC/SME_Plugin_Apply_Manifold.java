/** Nikita MENEZES B.**/
/** Elton REXHEPAJ **/
/** Asm SHIAVUDDIN **/
/** Sreetama BASU **/

package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.CompositeConverter;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.plugin.filter.EDM;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

//import java.util.Vector;
//import ij.util.ArrayUtil;

/**
 Plugin Description :
 **/


public class SME_Plugin_Apply_Manifold implements PlugIn {

    private ImagePlus imp;
    private ImagePlus rawImage;
    private ImagePlus projectedImage;
    private ImagePlus manifold ;


    private static String none = "*None*";
    private static int maxChannels = 4;
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

    public void run(String arg) {
        applyStackManifold();
    }

    /** Combines up to seven grayscale stacks into one RGB or composite stack. */
    public void applyStackManifold() {
        int[] wList = WindowManager.getIDList();
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
        gd.addChoice("Manifold  :", titles, macro?none:names[0]);
        gd.addChoice("Channel 1 :", titles, macro?none:names[1]);
        gd.addChoice("Channel 2 :", titles, macro?none:names[2]);
        gd.addChoice("Channel 3 :", titles, macro?none:names[3]);

        gd.addCheckbox("Create composite", createComposite);
        gd.addCheckbox("Keep source images", keep);
        gd.addCheckbox("Ignore source LUTs", ignoreLuts);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        int[] index = new int[maxChannels];

        for (int i=0; i<maxChannels; i++) {
            index[i] = gd.getNextChoiceIndex();
        }

        createComposite = gd.getNextBoolean();
        keep = gd.getNextBoolean();
        ignoreLuts = gd.getNextBoolean();
        if (!macro) {
            staticCreateComposite = createComposite;
            staticKeep = keep;
            staticIgnoreLuts = ignoreLuts;
        }

        ImagePlus[] images = new ImagePlus[maxChannels];
        int stackSize = 0;
        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int slices = 0;
        int frames = 0;
        for (int i=0; i<maxChannels; i++) {
            //IJ.log(i+"  "+index[i]+"	"+titles[index[i]]+"  "+wList.length);
            if (index[i]<wList.length) {
                images[i] = WindowManager.getImage(wList[index[i]]);
                if (width==0) {
                    width = images[i].getWidth();
                    height = images[i].getHeight();
                    stackSize = images[i].getStackSize();
                    bitDepth = images[i].getBitDepth();
                    slices = images[i].getNSlices();
                    frames = images[i].getNFrames();
                }
            }
        }
        if (width==0) {
            error("There must be at least one source image or stack.");
            return;
        }

        boolean mergeHyperstacks = false;
        for (int i=0; i<maxChannels; i++) {
            ImagePlus img = images[i];
            if (img==null) continue;
            if (img.getStackSize()!=stackSize) {
                error("SME PROJECT = The source stacks must have the same number of images.");
                return;
            }
            if (img.isHyperStack()) {
                if (bitDepth==24) {
                    error("SME PROJECT = Source hyperstacks cannot be RGB.");
                    return;
                }
                if (img.getNChannels()>1) {
                    error("SME PROJECT = Source hyperstacks cannot have more than 1 channel.");
                    return;
                }
                if (img.getNSlices()!=slices || img.getNFrames()!=frames) {
                    error("Source hyperstacks must have the same dimensions.");
                    return;
                }
                mergeHyperstacks = true;
            } // isHyperStack
            if (img.getWidth()!=width || images[i].getHeight()!=height) {
                error("The source images or stacks must have the same width and height.");
                return;
            }
            if (createComposite && img.getBitDepth()!=bitDepth) {
                error("The source images must have the same bit depth.");
                return;
            }
        }


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


    void error(String msg) {
        IJ.error("Merge Channels", msg);
    }
}