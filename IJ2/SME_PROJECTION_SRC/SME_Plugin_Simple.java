package SME_PROJECTION_SRC;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.process.LUT;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static ij.IJ.error;

/**
 * Created by ERexhepa on 09/01/17.
 */
public class SME_Plugin_Simple implements PlugIn {
    private ImagePlus imp;
    private ImagePlus rawImage;
    private ImagePlus projectedImage;
    private ImagePlus manifold;
    private ImagePlus[] images;

    private static String none = "Red channel";
    private static int maxChannels = 4;
    private static int nmbChannels = 3;
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
    private int index ;

    private SME_Plugin_Get_Manifold smePlugin;
    private SME_Plugin_Simple_CONF smePluginConf ;
    private SME_Plugin_Simple_BFIELD smePluginBfield;
    private ImagePlus hyperStackSME ;

    public void run(String arg) {
        if (WindowManager.getCurrentImage()==null) {
            error("No images is selected.");
            return;
        }

        if (WindowManager.getCurrentImage().getBitDepth()==24) {
            error("SME Pluing has detected a stack of RGB images, Please change to monochromatic images or hyper stack");
            return;
        }

        hyperStackSME = WindowManager.getCurrentImage();
        smePluginConf = new SME_Plugin_Simple_CONF();
        smePluginBfield = new SME_Plugin_Simple_BFIELD();

        Font headerFont = new Font("Serif", Font.ITALIC | Font.BOLD  , 18);

        if(WindowManager.getCurrentImage().isHyperStack()){
            // hyperstack color

            images = ChannelSplitter.split(hyperStackSME);

            smePluginConf.setImages(images);
            smePluginBfield.setImages(images);

            // get channel color ids
            Color[]  colors1 = new Color[images.length];
            for(int i=0;i<images.length;i++){
                hyperStackSME.setC(i+1);
                colors1[i] = (((CompositeImage) hyperStackSME).getChannelColor());
            }

            maxChannels         = images.length;
            projectionStacks    = new ImagePlus[maxChannels];

            smePluginConf.setProjectionStacks(projectionStacks);
            smePluginConf.setMaxChannels(maxChannels);
            smePluginBfield.setProjectionStacks(projectionStacks);
            smePluginBfield.setMaxChannels(maxChannels);

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

            GenericDialog gd = new GenericDialog("SME Plugin");

            gd.addMessage("Image type :",headerFont);
            gd.addMessage(" ",headerFont);
            String[] channelMessage = new String[1];
            String[] imtypeMessage = new String[2];

            imtypeMessage[0] = "Confocal";
            imtypeMessage[1] = "Bright field";

            gd.addChoice("Extract manifold from",titles, titles[0]);
            gd.addChoice("The image stack is     ",imtypeMessage,"Confocal");

            gd.addMessage("                                                                                  ",new Font("Serif", Font.ITALIC | Font.BOLD | Font.LAYOUT_NO_LIMIT_CONTEXT, 18));
            //gd.addMessage("                                 ",new Font("Serif", Font.ITALIC | Font.BOLD | Font.LAYOUT_NO_LIMIT_CONTEXT, 18));

            gd.addMessage("Extracted layers (optional) :",headerFont);
            gd.addMessage(" ");

            gd.addSlider("below manifold",0,images[0].getStackSize(),0);
            gd.addSlider("above manifold",0,images[0].getStackSize(),0);

            gd.showDialog();

            smePluginConf.setLowBuffManifold(((Scrollbar)gd.getSliders().elementAt(0)).getValue());
            smePluginConf.setHighBuffManifold(((Scrollbar)gd.getSliders().elementAt(1)).getValue());
            smePluginBfield.setLowBuffManifold(((Scrollbar)gd.getSliders().elementAt(0)).getValue());
            smePluginBfield.setHighBuffManifold(((Scrollbar)gd.getSliders().elementAt(1)).getValue());

            if (gd.wasCanceled())
                return;

            index = gd.getNextChoiceIndex();
            //images = new ImagePlus[maxChannels];

            stackSize = 0;
            width = 0;
            height = 0;

            stackSize = images[0].getStackSize();

            try {
                for (int i = 0; i < images.length; i++) {
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
            }
            catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            smePluginConf.setStackSize(stackSize);
            smePluginBfield.setStackSize(stackSize);

            LUT[] lutTable = (((CompositeImage)hyperStackSME)).getLuts();

            int indexConfocal  = gd.getNextChoiceIndex();

            if(indexConfocal==0){
                //confocal multichannel
                smePluginConf.runConfColour(index,lutTable);
            }else{
                //brighfield multichannel
                smePluginBfield.runBfieldColour(index,lutTable);
            }

        }else{
            // monochromatic image

            images = new ImagePlus[1];
            images[0] = WindowManager.getCurrentImage();

            smePluginConf.setImages(images);
            smePluginBfield.setImages(images);

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

            smePluginConf.setWidth(width);
            smePluginConf.setHeight(height);
            smePluginConf.setStackSize(stackSize);
            smePluginBfield.setWidth(width);
            smePluginBfield.setHeight(height);
            smePluginBfield.setStackSize(stackSize);


            if (width==0) {
                error("There must be at least one source image or stack.");
                return;
            }

            GenericDialog gd = new GenericDialog("SME Plugin");

            gd.addPanel(new Panel(),GridBagConstraints.WEST,new Insets(0,0,0,0));



            gd.addMessage("Image type :",headerFont);
            gd.addMessage(" ",headerFont);
            String[] channelMessage = new String[1];
            String[] imtypeMessage = new String[2];
            channelMessage[0] = "single channel";
            imtypeMessage[0] = "Confocal";
            imtypeMessage[1] = "Bright field";

            gd.addChoice("Extract manifold from",channelMessage,"single channel");
            gd.addChoice("The image stack is     ",imtypeMessage,"Confocal");

            ((Component) (gd.getChoices().elementAt(0))).setEnabled(false);
            ((Component) (gd.getChoices().elementAt(0))).setForeground(new Color(125,125,125));

            //((Component) (gd.getChoices().elementAt(0))).setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            //((Component) (gd.getChoices().elementAt(1))).setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

            //gd.addMessage("____________________________________",new Font("Serif", Font.ITALIC | Font.BOLD | Font.LAYOUT_NO_LIMIT_CONTEXT, 18));
            gd.addMessage("                                                                                  ",new Font("Serif", Font.ITALIC | Font.BOLD | Font.LAYOUT_NO_LIMIT_CONTEXT, 18));
            //gd.addMessage("                                 ",new Font("Serif", Font.ITALIC | Font.BOLD | Font.LAYOUT_NO_LIMIT_CONTEXT, 18));

            gd.addMessage("Extracted layers (optional) :",headerFont);
            gd.addMessage(" ");

            gd.addSlider("below manifold",0,images[0].getStackSize(),0);
            gd.addSlider("above manifold",0,images[0].getStackSize(),0);

            GridBagLayout grid = (GridBagLayout) gd.getLayout();
            Component comp = (Component) gd.getChoices().elementAt(0);

            GridBagConstraints c = grid.getConstraints(comp);

            c.insets.left = c.insets.left + 0 ;
            c.insets.top = 0;
            c.insets.bottom = 0;
            grid.setConstraints(comp, c);

            //((Component) (gd.getSliders().elementAt(0))).setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            //((Component) (gd.getSliders().elementAt(1))).setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

            gd.showDialog();

            smePluginConf.setLowBuffManifold(((Scrollbar)gd.getSliders().elementAt(0)).getValue());
            smePluginConf.setHighBuffManifold(((Scrollbar)gd.getSliders().elementAt(1)).getValue());
            smePluginBfield.setLowBuffManifold(((Scrollbar)gd.getSliders().elementAt(0)).getValue());
            smePluginBfield.setHighBuffManifold(((Scrollbar)gd.getSliders().elementAt(1)).getValue());

            if (gd.wasCanceled())
                return;

            if(gd.getNextBoolean()==Boolean.TRUE){
                //confocal multichannel
                smePluginConf.runConfMono(0);
            }else{
                //brighfield multichannel
                smePluginBfield.runBfieldMono(0);
            }
        }

    }

    private void processChannelsManifoldSimple() {
    }

    private void processChannelsManifoldColors() {
    }
}
