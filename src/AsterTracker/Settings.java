/*********************************************************************************************************************
 *                                              Class Settings
 **********************************************************************************************************************/
package AsterTracker;

import AsterUtil.SphCoordinate;
import AsterUtil.Util;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream; 
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
* Class Settings contains parameters that control all aspects of program operation.
*
* @author Tony Evans
*/
public class Settings {
    
   /** Note that settings which can change during synthetic tracking and are used by the processing threads
    *  must be be marked as volatile.
    */

   /**************************************************************************************************************
    * 
    *                          Part 1 - Parameters loaded from the configuration file.
    */
    
    /*  *********************************** Synthetic Tracking parameters. */
    
    /** PA to start tracking (degrees). */
    public float minPA = 0.0f;                 

    /** PA to end tracking (degrees). */
    public float maxPA = 360.0f;                

    /** Motion to start tracking ("/m). */
    public float minMotion = 0.25f;            

    /** Motion to end tracking ("/m). */
    public float maxMotion = 9.0f;     
    
    /** Permitted track and stack error (pixels). */
    public float trkErr = 0.5f;

    /** ************************************ Display parameters. */
     
    /** Sigmas below background for black. */
    public float blackHist = 3.0f;              

    /** Sigmas above background for white. */
    public float whiteHist = 9.0f;              

    /** ************************************ Object detection parameters. */
    
    /** Aperture radius (pixels). */
    public int   aperture = 5;               

    /** Sigmas above background for detection threshold. */
    public float sigma1 = 1.9f;                

    /** Sigmas above background for star detection (removal) threshold. */
    public float sigma2 = 3.0f;            
    
    /** T-Count pixels above threshold. */
    public int tCount1;

    /** Permitted error in position measurement. */
    public float posErr;
    
   /** ************************************* FITS processing rules and parameters. */
    
    /** FITS Normalisation - sigmas below background for value 0. */
    public float blackFits = 4.5f;       

    /** FITS Normalisation - sigmas above background for value 1. */
    public float whiteFits = 7.5f;               

    /** 3x3 Blur selection. */
    public boolean doBlur = true;              

    /** Pseudo Flat Field selection. */
    public boolean flatten = false;               
    
    /** Vertical Line Removal selection. */
    public boolean deLine = false;


   /************************************************************************************************************
    *
    *                            Part 2 system generated or fixed parameters. 
    */
    
    /** Current motion ("/m). */
    public volatile float motion = 0.0f;              

    /** Current motion step. */
    public float motionStep = 0.0f;          

    /** Current PA (degrees). */
    public float PAdeg = 0.0f;            

    /** Current PA (radians). */
    public volatile float PA = 0.0f;                  

    /** Current PA step (degrees). */
    public float PAStepDeg = 5.0f;           

    /** Current PA step (radians). */
    public float PAStep = 0.0f;               

    /** Longest elapse time for any group (start of first to end of last exposure) (mins). */
    public float maxElapse;                            

    /** Total number of FITS Files loaded. */
    public int fitsCount = 0;                

    /** What to display? 1=Group Stacks, 3=Object Stacks, 4=Both overlaid. */
    public int showType = 1;    
    
    /** Are there any stacks ready to blink on the display?. */
    public volatile boolean blink = false;

    /** What to show for a Mover? false=normal view, true=show only the object stack. */ 
    public boolean objectOnly = false;             
    
    /** NAXIS1 (width) value. */
    public int naxis1;        

    /** NAXIS2 (height) value. */
    public int naxis2;        
    
    /** Rotation value (radians) from first FITS file. */
    public double crota2;                     

    /** Scale in pixels per arcsecond. */
    public float pixToArcsecs;             

    /** Mover currently being viewed. */
    public volatile Mover targetMover;     
    
    /** New Mover has been found. */
    public boolean newMover = false;

    /** Set true when all the synthetic tracking has been completed. */   
    public volatile boolean finished = false;
    
    /** Set true when processing has been paused for a review of movers. */
    public volatile boolean paused = false;
    
    /** RA and Decl of common centre of stacked images (radians). */
    public SphCoordinate refPoint;           

    /** Pixel coordinates of centre of image. */
    public Pixel refPixel;                      
 
    /** Exposure time of images (seconds). */
    public float expTime;                                           

    /** Elapse times between image groups (minutes). */
    public float[] dTime;                                            

    /** Current configuration file name. */
    public File settingsFile;
    
    /** Settings properties object. */
    private Properties config;

    /** General ini file. */
    public IniFile ini;

    /** Log file directory. */
    public String logDirectory;
    
    /** Pointer to GUI. */
    public final AsterGUI parent;

    /**
     * Constructor initialises variables.
     * @param ag The parent AsterUI.
     */
    public Settings(AsterGUI ag) {

        refPoint = new SphCoordinate(0.0, 0.0);
        refPixel = new Pixel(0,0);
        targetMover = null;
        ini = new IniFile();
        parent = ag;
        paused = false;
    }

    /**
     * Initialise the settings from the .ini file.
     */
    public void init() {
        // if there is a known settings file, load it (if not, the defaults are used).
        if (getSettingsFile()) {loadSettings();}

        // initialise the synhetic tracking parameters
        initSynthetic();
    }

    /**
     * Get motion to be used for stack and track.
     * 
     * @param showMover True if display is showing a known Mover else false uses synthetic value .
     * @return Motion of displayed Mover or Synthetic tracking.
     */
    public float getMotion(boolean showMover) {
        if (!showMover) {return motion;} else {return targetMover.motion;}
    }

    /**
     * Get PA to be used for stack and track. Includes rotation.
     * @param showMover True if display is showing a known Mover else uses synthetic value.
     * @return PA of displayed Mover or Synthetic tracking.
     */
    public float getPA(boolean showMover) {
        if (!showMover) {return (float) (PA + crota2);} else {return (float) (targetMover.PA + crota2);}
    }

    /**
     * Get the configuration file name from the ini file (if any).
     * @return 
     */
    public boolean getSettingsFile() {
        settingsFile = new File(ini.getProperty("settingsfile"));
        return settingsFile.exists();
    }

    /**
     * Store the configuration file name in the ini file.
     */
    public void setSettingsFile() {
        ini.putProperty("settingsdirectory", settingsFile.getParent());
        ini.putProperty("settingsfile", settingsFile.getPath());
    }

    /**
     * File open dialog to identify and load a configuration file.
     */
    public void findSettingsFile() {
        
        // setup file selection dialog 
        Frame frame = new Frame();
        JFileChooser fc = new JFileChooser(ini.getProperty("settingsdirectory"));
        fc.setMultiSelectionEnabled(false);

        // add a filter for cfg files
        FileNameExtensionFilter ff = new FileNameExtensionFilter("Settings files","cfg");
        fc.addChoosableFileFilter(ff);

        // show file selection dialog
        int r = fc.showOpenDialog(frame);

        // if response NOK quit 
        if (r != JFileChooser.APPROVE_OPTION) {return;}

        // get the settings file
        settingsFile = fc.getSelectedFile();
    }

    /**
     * Load the settings from a configuration file.
     */
    public final void loadSettings() {
        config = new Properties();
        try (FileInputStream in = new FileInputStream(settingsFile)) {
            config.load(in);
            blackHist =  Util.s2f(config.getProperty("black"),0f);
            whiteHist = Util.s2f(config.getProperty("white"),0f);
            minPA = Util.s2f(config.getProperty("minPA"), 0.0f);
            maxPA = Util.s2f(config.getProperty("maxPA"), 0.0f);
            minMotion= Util.s2f(config.getProperty("minMotion"), 0.0f);
            maxMotion= Util.s2f(config.getProperty("maxMotion"), 0.0f);
            aperture = Util.s2i(config.getProperty("aperture"), 0);
            sigma1 = Util.s2f(config.getProperty("sigma1"), 0.0f);
            sigma2 = Util.s2f(config.getProperty("sigma2"), 0.0f);
            doBlur = Boolean.parseBoolean(config.getProperty("blur"));
            deLine = Boolean.parseBoolean(config.getProperty("deline"));
            flatten = Boolean.parseBoolean(config.getProperty("flatten"));
            tCount1 = Util.s2i(config.getProperty("tCount1"), 0);
            trkErr = Util.s2f(config.getProperty("maxErr"), 0.0f);
            posErr = Util.s2f(config.getProperty("midErr1"), 0.0f);
            logDirectory = config.getProperty("logdirectory");
        }
        catch (Exception e){
            parent.showInfo();
        }
        parent.showInfo();
    }

    /**
     * SaveAs settings into a new configuration file.
    */
     public void saveAs() {
        
        // set up and show file save dialog
        Frame frame = new Frame();
        JFileChooser fc = new JFileChooser(ini.getProperty("settingsdirectory"));
        FileNameExtensionFilter ff = new FileNameExtensionFilter("cfg","cfg");
        fc.addChoosableFileFilter(ff);
        int r = fc.showSaveDialog(frame);

        // if chooser closes ok.. 
        if (r == JFileChooser.APPROVE_OPTION) {
            
            // check for correct suffix
            settingsFile = fc.getSelectedFile();
            String path = settingsFile.getPath();
            if ((path.endsWith(".cfg"))){
                settingsFile = new File(path);
            } else  {
                settingsFile = new File(path+".cfg");
            }
            
            // check if file of that name already exists and ask if ok to overwrite.
            if (settingsFile.exists()) {
               Toolkit.getDefaultToolkit().beep();
               int opt= JOptionPane.showConfirmDialog(null,"File already exists. Overwrite it?",
                       "File Exists",JOptionPane.YES_NO_OPTION);
               if (opt!=0) {return;}
            }
            
            // save settings to identified file
            save();

            // Store config file name and path in the ini file
            setSettingsFile();
        }
    }

    /**
     * Save settings to the current configuration file.
     */
    public void save() {
        
        // test that a file is available
        if (settingsFile == null) {return;}   // TODO should give the user a message here
        config = new Properties();
        try (FileInputStream in = new FileInputStream(settingsFile)) {
            config.load(in);
        }
        catch (Exception e) {
        }
        
        // set all the variable settings as properties
        config.setProperty("black", ""+ blackHist);
        config.setProperty("white", ""+ whiteHist);
        config.setProperty("minPA", ""+ minPA);
        config.setProperty("maxPA", ""+ maxPA);
        config.setProperty("minMotion", ""+ minMotion);
        config.setProperty("maxMotion", ""+ maxMotion);
        config.setProperty("aperture", ""+ aperture);
        config.setProperty("sigma1", ""+ sigma1);
        config.setProperty("sigma2", ""+ sigma2);
        config.setProperty("blur", ""+doBlur);
        config.setProperty("flatten", ""+flatten);
        config.setProperty("deline", ""+deLine);
        config.setProperty("tCount1", ""+tCount1);
        config.setProperty("maxErr", ""+trkErr);
        config.setProperty("midErr1", ""+posErr);
        config.setProperty("logdirectory", logDirectory);

        // save the configuration file
        try (FileOutputStream out = new FileOutputStream(settingsFile)) {
            config.store(out, "Astertect Settings");
        }
        catch (Exception e) {
        }

        // show the new configuration file name in the GUI
        parent.showInfo();
    }

   /** 
    * Initialise synthetic tracking. 
    */
    public void initSynthetic() {
        
        // set initial motion and PA
        motion = minMotion;
        PAdeg = minPA;
        PA = (float) Math.toRadians(minPA);

        // calculate initial motion and PA steps
        motionStep = 0.25f;  
        PAStepDeg = 45f;
        PAStep = (float) Math.toRadians(PAStepDeg);
    }

   /**
    * Go to next step of synthetic tracking.
    * @return true if all synthetic tracking is complete.
    */
    public boolean nextStep() {
        
        // calculate optimum step sizes
        motionStep =(4.0f * trkErr * pixToArcsecs)/(maxElapse); 
        float dPA = (4.0f * trkErr * pixToArcsecs)/(motion * maxElapse);
        PAStepDeg = (float) Math.min( Math.toDegrees(dPA),45);
                
        // advance to next PA
        PAdeg = PAdeg + PAStepDeg;

        // if PA over max, reset and advance to new motion 
        if (PAdeg > maxPA){
            PAdeg = minPA;
            motion = motion + motionStep;
            // if finished synthetic tracking set flag and retern end=true. 
            if (motion > maxMotion) {
                finished = true;
                return true;
            }
        }
       
        // set up PA in radians
        PA = (float) Math.toRadians(PAdeg);
        PAStep = (float) Math.toRadians(PAStepDeg);
       
        // return false indicates there is a new track to be processed
        return false;
    }
}
