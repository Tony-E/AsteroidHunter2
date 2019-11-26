/*********************************************************************************************************************
 *                                            Class FITSSelector
 *********************************************************************************************************************/
package AsterTracker;

import AsterUtil.AsterIni;
import java.awt.*;
import java.io.*;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Class FitsSelector provides the user with a file selection dialog in which to select the Fits files. 
 * @author Tony
 */
public class FitsSelector {
    // ini file to store current directory
    private final AsterIni ini;    
    // list of files selected by the user 
    private File[] files; 
    // pointer to Settings */
    private final Settings settings;                     

   /**
    * Constructor stores pointer to Settings and creates an ini file in which to store the 
    * source directory.
    * @param p Settings.
    */
    public FitsSelector(Settings p) {
        ini = new AsterIni();                    // create an ini file object
        settings = p;                            // store pointer to settings 
    }

   /**
     * Present the user with a file selection dialog and gets a list of files.
     * @return The set of files selected by user.
     **/
    public File[] openFiles() {
        // set up a file open dialog
        Frame parent = new Frame();
        JFileChooser fc;
        FileNameExtensionFilter ff;
        fc = new JFileChooser(ini.getProperty("asterdirectory"));
        fc.setMultiSelectionEnabled(true);

        // add a filter for fit files
        ff = new FileNameExtensionFilter("FITS files","fit");
        fc.addChoosableFileFilter(ff);

        // show file open dialog 
        int r = fc.showOpenDialog(parent);

        // if response OK, sort the file list and save directory where they came from in the ini file
        if (r == JFileChooser.APPROVE_OPTION) {
            files = fc.getSelectedFiles();
            ini.putProperty("asterdirectory", files[0].getParent());
            Arrays.sort(files);
            return files;
        }
        return null;
    }
}

