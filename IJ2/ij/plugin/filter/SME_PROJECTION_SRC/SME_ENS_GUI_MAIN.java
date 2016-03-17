package ij.plugin.filter.SME_PROJECTION_SRC;

import ij.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Created by rexhepaj on 16/03/16.
 */
public class SME_ENS_GUI_MAIN extends JFrame implements ActionListener {

    // Set ImageJ graphical components
    private ImagePlus currentImage ;
    private ImagePlus processedImage;
    private ImagePlus tmpImage ;
    public ImageStack imageStack;

    private BorderLayout borderLayout1  = new BorderLayout();
    private JButton batchRunButton      = new JButton();
    private JPanel controlPanel         = new JPanel();
    private JComboBox standartProjectionMethods = new JComboBox();

    private JButton smlRunButton        = new JButton();
    private JButton kmeanRunButton      = new JButton();
    private JButton enoptRunButton      = new JButton();
    private JButton saveImButton        = new JButton();

    private GridBagLayout gridLayoutMain    = new GridBagLayout();

    private GridBagConstraints cMain    = new GridBagConstraints();
    private GridBagConstraints cTop     = new GridBagConstraints();
    private GridBagConstraints cBottom  = new GridBagConstraints();
    private GridBagConstraints cCenter  = new GridBagConstraints();

    // application content
    private boolean mRunning;
    private SME_KMeans_Paralel mKMeans;
    private SME_Cluster[] clustersKmeans;

    // Define standart projection methods
    //{"Average Intensity", "Max Intensity", "Min Intensity",
    // "Sum Slices", "Standard Deviation", "Median"};
    private static final String AVG_INT = "Average Intensity";
    private static final String MAX_INT = "Max Intensity";
    private static final String MIN_INT = "MIN Intensity";
    private static final String SUM_SLICE = "Sum Slices";
    private static final String STD_INT = "Standard Deviation";
    private static final String MED_INT = "Median Deviation";

    private double[][] coordinates ;

    private static final String BASIC_KMEANS = "Basic K-Means Clustering";
    private static final String BENCHMARKED_KMEANS = "Benchmarked K-Means Clustering";
    private static final String CONCURRENT_KMEANS = "Concurrent K-Means Clustering";

    /**
     * Constructor called to initialize the graphical interface
     */
    public SME_ENS_GUI_MAIN() {
        System.out.print("Debug");
        //super("FrameDemo");
    }

    public void initGUI() {
        buildGUI();
    }

    /**
     * Method to initialise all graphical components for display
     */
    public void buildGUI() {

        // initialise the action listeners
        standartProjectionMethods.addActionListener(this);


        // define grid constraints
        cCenter.fill = GridBagConstraints.BOTH;
        cTop.fill = GridBagConstraints.NONE;
        cBottom.fill = GridBagConstraints.NONE;

        // Define the GUI overall panel

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        controlPanel = (JPanel) this.getContentPane();

        // Grap the screen size and set the size of the GUI to half each dimension

        GraphicsDevice gd   = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int widthScreen     = gd.getDisplayMode().getWidth();
        int heightScreen    = gd.getDisplayMode().getHeight();

        setSize(new Dimension(heightScreen, widthScreen));
        setTitle("SME 3D PROJECTION METHOD - ENS COMPUTATIONAL BIOLOGY GROUP");

        // Initialise the different GUI building components

        standartProjectionMethods.addItem(AVG_INT);
        standartProjectionMethods.addItem(MAX_INT);
        standartProjectionMethods.addItem(MED_INT);
        standartProjectionMethods.addItem(MIN_INT);
        standartProjectionMethods.addItem(STD_INT);
        standartProjectionMethods.addItem(SUM_SLICE);

        batchRunButton.setText("RUN SME - BATCH MODE");
        batchRunButton.addActionListener(this);

        saveImButton.setText("RUN SME - SAVE THE MANIFOLD AS JPG");
        saveImButton.addActionListener(this);

        smlRunButton.setText("RUN SME - SML (STEP1)");
        smlRunButton.addActionListener(this);

        kmeanRunButton.setText("RUN SME - KMEAN (STEP2)");
        kmeanRunButton.addActionListener(this);

        enoptRunButton.setText("RUN SME - ENERGY OPTIMISATION (STEP3)");
        enoptRunButton.addActionListener(this);

        //Initialize the control panel
        controlPanel.setLayout(gridLayoutMain);
        cMain.fill = GridBagConstraints.HORIZONTAL;

        cMain.ipady = 10;
        cMain.ipadx = 10;
        cMain.weightx = 0;
        cMain.gridwidth = 1;
        cMain.gridx = 0;
        cMain.gridy = 0;
        controlPanel.add(standartProjectionMethods, cMain);

        cMain.ipady = 10;
        cMain.ipadx = 10;
        cMain.weightx = 0.5;
        cMain.gridwidth = 1;
        cMain.gridx = 1;
        cMain.gridy = 0;
        controlPanel.add(smlRunButton, cMain);

        cMain.ipady = 10;
        cMain.ipadx = 10;
        cMain.weightx = 0.5;
        cMain.gridwidth = 1;
        cMain.gridx = 2;
        cMain.gridy = 0;
        controlPanel.add(kmeanRunButton, cMain);

        cMain.ipady = 10;
        cMain.ipadx = 10;
        cMain.weightx = 0.5;
        cMain.gridwidth = 1;
        cMain.gridx = 3;
        cMain.gridy = 0;
        controlPanel.add(enoptRunButton, cMain);

        // add images
        cMain.ipady = 10;
        cMain.ipadx = 10;
        cMain.weightx = 1;
        cMain.gridwidth = 2;
        cMain.gridx = 0;
        cMain.gridy = 1;
        SME_ENS_Image_Component imcontent = new SME_ENS_Image_Component(currentImage,Boolean.TRUE, currentImage.getWidth(), currentImage.getHeight(), "blabla");

        controlPanel.add(imcontent, cMain);

        // add batch run control buttons
        cMain.ipady = 10;
        cMain.ipadx = 10;
        cMain.weightx = 0;
        cMain.gridwidth = 2;
        cMain.gridx = 0;
        cMain.gridy = 2;
        controlPanel.add(batchRunButton, cMain);

        // add batch save control buttons
        cMain.ipady = 10;
        cMain.ipadx = 10;
        cMain.weightx = 0;
        cMain.gridwidth = 2;
        cMain.gridx = 2;
        cMain.gridy = 2;
        controlPanel.add(saveImButton, cMain);

        controlPanel.setVisible(Boolean.TRUE);
        this.setVisible(Boolean.TRUE);
    }

    // Action listeners

    public synchronized void actionPerformed(ActionEvent e) {

        // switch depending on the source of the action button the according response

        if (e.getSource() == batchRunButton && !mRunning) {
            System.out.println("new line");
        }
    }

   // Getter and setters

    public ImagePlus getCurrentImage() {
        return currentImage;
    }

    public void setCurrentImage(ImagePlus currentImage) {
        this.currentImage = currentImage;
    }

    public ImagePlus getProcessedImage() {
        return processedImage;
    }

    public void setProcessedImage(ImagePlus processedImage) {
        this.processedImage = processedImage;
    }

    public ImagePlus getTmpImage() {
        return tmpImage;
    }

    public void setTmpImage(ImagePlus tmpImage) {
        this.tmpImage = tmpImage;
    }
}
