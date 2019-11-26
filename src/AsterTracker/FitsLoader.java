 /*********************************************************************************************************************
 *                                               Class FITSLoader
 **********************************************************************************************************************/
package AsterTracker;



import AsterUtil.Util;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.swing.JTextArea;

/**
 * Class FitsLoader loads each of the Fits files and creates FitsImage objects. 
 * 
 * <p>The FitsImage objects 
 * are allocated to ImageGroup objects according to the need for stacking.</p>
 *
 * @author Tony Evans
 **/
public class FitsLoader {

    /* these items are held here as they are needed to set up the FitsImage objects */
    private File[] fitsFiles;                    // Files selected by the user
    private int bitpix = 0;                      // number of bits per pixel (BITPIX keyword)
    private int naxis1 = 0;                      // number of pixels in x axix (NAXIS1 keyword)
    private int naxis2 = 0;                      // number of pixels in y axis (NAXIS2 keyword)
    
    /* internal working values */
    private FitsImage fi;                        // FITSimage currently being constructed
    private ImageGroup gr;                       // image group currently being created
    private final Settings settings;             // pointer to settings
    private int grpno;                           // group index number
    
    /* switches used to check validity of a file */
    private boolean confirmWCS;                  // WCS data is present
    private boolean confirmFITS;                 // the file has a valid SIMPLE keyword

    /**
     * Constructor.
     * @param p Pointer to Settings.
     */
    public FitsLoader(Settings p) {
        settings = p;                            // store pointer to settings
        grpno = 0;                               // group index number
    }
    
    /**
     * Create and use a FitsSelector to obtain the file list from the user.
     */
    public void selectFiles() {
        FitsSelector fs = new FitsSelector(settings);
        fitsFiles = fs.openFiles();
        if (fitsFiles != null) settings.fitsCount = fitsFiles.length;
    }

    /**
     * Load Fits files into FitsImage objects and gather the FitsImages into ImageGroups.
     *
     * @param groups The ArrayList of ImageGroups into which newly created groups are added.
     * @param comment The text field where comments can be written.
     */
    public void loadFiles(ArrayList<ImageGroup> groups, JTextArea comment) {
        // if no files, quit
        if (fitsFiles == null) {
            comment.append("No files selected.\n");
            return;
        }

        // clear out any old ImageGroups
        groups.clear();

        // for each of the files...
        for (File file : fitsFiles) {
            
            // initialise switches used to check validity of files
            confirmWCS = false;
            confirmFITS = false;
         

            // if this is first file of a group, creat a new ImageGroup
            if (file.getName().contains("_0_")) {
                grpno+=1;
                gr = new ImageGroup(grpno, settings);
                groups.add(gr);
            }
           
            // if no image group has been formed cannot continue 
            if (gr == null) {
                comment.append("First file must start a group. \n");
                return;
            }

            // read the file into byte array 
            byte[] inBytes = new byte[(int) file.length()];
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                fis.read(inBytes);
                fis.close();
             } catch (IOException e) {
                 comment.append("Error; " + e.getMessage() + "\n");
                 return;
             }
            
            // process the 80-column cards at the front of the file. k points to the end of the cards
            int k = loadCards(inBytes);
            
            // if not FITS or no WCS data found, quit with message
            if (!confirmWCS) {
                comment.append("NO WCS data - " + file.getName() + "\n");
                continue;
            }
            if (!confirmFITS) {
                comment.append("Not a FITS file - " + file.getName() + "\n");
                continue;
            }
      
            // calc location of start of pixel data and load pixels into inPix array
            float q = (k/2880.0f);
            k = (int) Math.ceil(q);
            k = k*2880;  
            loadImage(inBytes,k);
        }
      
        // each group must now set up reference time stamps and elapse times
        for (ImageGroup g : groups) {
            g.setRefTime();
        }
 
        // record the maximum of the group elapse times (longest group) in the settings
        settings.maxElapse = 0;
        for (ImageGroup g : groups) {
            if (g.groupElapse>settings.maxElapse) {settings.maxElapse = g.groupElapse;}
        }
     
        // post message saying what has been done */ 
        comment.append("" + fitsFiles.length + " files loaded in " + grpno + "stacking groups.\n");
    }
    
   /**
    * Process the header "Cards" in the byte buffer until the END card is found.
    */
    private int loadCards(byte[] bytes) {
        int k = 0;
        String keyword;
        
        do {
            // get an 80-column eader record and split into tokens
            String card = new String(bytes,k,80);
            String[] tokens = card.split("[ =/]+");
            k+=80;
            keyword = tokens[0];

            // look for the first (mandatory) cards and store info
            if (keyword.equals("SIMPLE"))   {confirmFITS = true;}
            if (keyword.equals("BITPIX"))   {bitpix = Util.s2i(tokens[1],0);}
            if (keyword.equals("NAXIS1"))   {naxis1 = Util.s2i(tokens[1],0);}
            if (keyword.equals("NAXIS2"))   {naxis2 = Util.s2i(tokens[1],0);
            
               /* now we have the image size we can create a new FitsImage */
                fi = new FitsImage(naxis1,naxis2, settings);
            }
            
            //get the rest of the useful keyword values
            if (keyword.equals("BZERO"))    {fi.bzero  = (int) Util.s2d(tokens[1],0);}
            if (keyword.equals("DATE-OBS")) {fi.JD.setDate(tokens[1]);}
            if (keyword.equals("EXPTIME"))  {fi.exptime = Util.s2f(tokens[1], 60);}

            // if CRPIX1 is found, WCS data must be present
            if (keyword.equals("CRPIX1"))   {
                fi.fitsRefPix.x = (int) Util.s2d(tokens[1],0);
                confirmWCS = true; 
            }
            // handle all the other relevant keywords
            if (keyword.equals("CRPIX2"))   {fi.fitsRefPix.y = (int) Util.s2d(tokens[1],0);}
            if (keyword.equals("CRVAL1"))   {fi.fitsRef.coord[0] = Math.toRadians(Util.s2d(tokens[1],0));}
            if (keyword.equals("CRVAL2"))   {fi.fitsRef.coord[1] = Math.toRadians(Util.s2d(tokens[1],0));}
            if (keyword.equals("CDELT1"))   {fi.cdelt1 = Math.toRadians(Util.s2d(tokens[1], 0));}
            if (keyword.equals("CDELT2"))   {fi.cdelt2 = Math.toRadians(Util.s2d(tokens[1], 0));}
            if (keyword.equals("CROTA2"))   {fi.crota2 = - Math.toRadians(Util.s2d(tokens[1], 0));}
            if (keyword.equals("FILTER"))   {fi.filter = tokens[1].replace("'","");}
            if (keyword.equals("CBLACK"))   {fi.cblack = Util.s2i(tokens[1], 0);}
            if (keyword.equals("CWHITE"))   {fi.cwhite = Util.s2i(tokens[1], 0);}
            if (keyword.equals("PEDESTAL")) {fi.pedestal = Util.s2i(tokens[1], 0);}
            if (keyword.equals("TELESCOP")) {fi.telescop = tokens[1]+tokens[2];}

        } while(!keyword.equals("END"));

        // when the END keyword has been found adjust cdelt1 and add the FITSImage to its Group
        if (confirmWCS) {
            // cdelt1 scale times COS(Decl) to obtain true angular scale of RA
            fi.cdelt1 = fi.cdelt1/Math.cos(fi.fitsRef.coord[1]);
            // save pixel scale in settings as arcseconds per pixel
            settings.pixToArcsecs = (float) Math.abs(3600*Math.toDegrees(fi.cdelt2));
            // add FITS to its group 
            gr.add(fi);
        }
        
        //return k which is a pointer to the end of the header data
        return k;
    }
    
   /**
    * Capture the pixel data and store it as float values.
    */
    private void loadImage(byte[] inBytes, int k) {

        // calc size of buffer and create buffer for reading pixel data
        int bbSize = naxis1*naxis2 * (int) bitpix/8;
        ByteBuffer bb = ByteBuffer.wrap(inBytes, k, bbSize );

        // read pixel data and store in the array
        for (int j = 0; j<naxis2; j++) {
            for (int i = 0; i<naxis1; i++) {
                // the raw data is in signed integers with values -32767 to +32767
                float pix = bb.getShort() + fi.bzero;
                fi.pixels[i][j] = pix;
            }
        }
    }
}

