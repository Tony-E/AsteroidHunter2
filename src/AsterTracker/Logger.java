
package AsterTracker;

import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import javax.swing.JFileChooser;

/**
 * Class Logger saves a log file containing properties of all ImageObjects and Movers in the system.
 * 
 * @author Tony Evans
 */
public class Logger {
    
    
    private final Settings settings;       // pointer to settings
    private File logFile;                  // the output log file
    private BufferedWriter out;            
    private String prefix;                 // prefix contains date and sequence
    private int count = 0;                 // counter for sequence number
    
    private final String header1 = "Log\t\tSeq\tx1\ty1\tpix1\ttCnt1\tsnr1\tflx1\t"
            + "x2\ty2\tpix2\ttCnt2\tsnr2\tflx2\t"
            + "x3\ty3\tpix3\ttCnt3\tsnr3\tflx3\t"
            + "motion\tPA\terMid\tscore\tstatus \r\n";
    private final DecimalFormat DF2p3 = new DecimalFormat("##0.000");
    private final DecimalFormat DFInt = new DecimalFormat("###0");

    /**
     *Constructor stores pointer to settings.
     * @param s Pointer to Settings.
     */
    public Logger(Settings s) {
        settings = s;
    }
    
    /**
     * Open the log file.
     */
    public void open() {
       /* If there is no directory for the log files, get one */
        if ("".equals(settings.logDirectory)) {
            Frame parent = new Frame();
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select folder for log files.");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            int r = fc.showSaveDialog(parent);      
            if (r == JFileChooser.APPROVE_OPTION) {
                settings.logDirectory = fc.getCurrentDirectory().toString();
            }    
        }
       /* create and open a log file and output the header line */
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date now = new Date();
        prefix = sdfDate.format(now);
        String logName = prefix + "-log.txt";
        logFile = new File(settings.logDirectory +"\\" + logName);
        try {
            out = new BufferedWriter(new FileWriter(logFile));
            out.write(header1);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Logger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Close the log file.
     */
    public void closeLog(){
        try {
            out.close();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Logger.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
   /**
    * Record data in the log file.
    * 
    * @param movers The Movers to be recorded.
    */
    public void save(ArrayList<Mover> movers) {
        String txt;
        count = 0;
        for (Mover m : movers) {
            txt = prefix + "\t" + count + "\t";
            for (ImageObject ob : m.objects) {
                txt+= DFInt.format(ob.location.x) + "\t";
                txt+= DFInt.format(ob.location.y) + "\t"; 
                txt+= DFInt.format(ob.obSize) + "\t";
                txt+= DFInt.format(ob.tCount) + "\t";
                txt+= DF2p3.format(ob.objectSNR) + "\t";
                txt+= DF2p3.format(ob.flux) + "\t";
            }
            txt+= DF2p3.format(m.motion) + "\t";
            txt+= DF2p3.format(Math.toDegrees(m.PA)) + "\t";
            txt+= DF2p3.format(m.errMid) + "\t";
            txt+= DF2p3.format(m.score) + "\t";
            txt+= m.status + "\r\n";
            try {
                out.write(txt);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Logger.class.getName()).log(Level.SEVERE, null, ex);
            } 
            count++;
        }
    }
}
