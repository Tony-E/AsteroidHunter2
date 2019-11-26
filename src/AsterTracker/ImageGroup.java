/**********************************************************************************************************************
 *                                              Class ImageGroup
 **********************************************************************************************************************/
package AsterTracker;

import AsterUtil.DateTime;
import AsterUtil.SphCoordinate;
import AsterUtil.Tester;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class ImageGroup represents a group of FITS images to be used for stacking. The class owns a set
 * of FITSImages with StackedImages of the group and a BufferedImage of the current Group Stack ready to display.
 * <p>
 * Methods include those to perform various stacking operations,searching for ImageObjects and
 * drawing the stacked image ready for display.
 *
 * @author Tony Evans
 */
public class ImageGroup {

    /* local copies of some relevant keywords from FITS Cards */
    private int naxis1;            // NAXIS1 keyword
    private int naxis2;            // NAXIS2 keyword
    
    /** The exposure time of each image (seconds). */
    public float exptime;         // EXPTIME keyword

    /** The set of FITS images in this group. */
    public ArrayList<FitsImage> fitsImages;

    /** Untracked stacked image including fixed stars. */
    public StackedImage aStack;

    /* Other stacks */
    private StackedImage wStack;          // workplace to store any stack being processed
    private StackedImage oStack;          // stack without stars, tracked at synthetic or specific mover rate
    private StackedImage dStack;          // pointer to which of the stacks is to be drawn on the screen next

    /** Reference RA/DEC (radians) for this group's reference pixel. */
    public SphCoordinate groupRef = new SphCoordinate(0.0, 0.0);

    /** Reference time-stamp for this group's stack.  */
    public DateTime groupRefTime = new DateTime();

    /** Elapse time from 1st to last FITS in this group (minutes). */
    public float groupElapse;

    /* group properties */
    private final int grpno;                // index number of this group
    private BufferedImage img;              // buffer containing drawn image
    private Graphics g;                     // graphics context for the image buffer
    private final Settings settings;        // pointer to Settings
    private final Aperture ap;              // aperture definition for use when looking for objects 
    private int minPix;                     // minimum pixels-over-threshold required for valid object.
    private final float[] edgeBands;                // min and max edgeBands for depleted edges

    /** Number of objects found in latest scan. */
    public int obCount;
    
    private final Tester tester;

    /** List of potential objects found in latest scan. */
    public ArrayList<ImageObject> objects;

    /**
     * Constructor initialises variables.
     *
     * @param grpno The group sequence number.
     * @param p     Point to the Settings object.
     */
    public ImageGroup(int grpno, Settings p) {
        fitsImages = new ArrayList<>();    // empty array of fitsImages
        objects = new ArrayList<>();       // empty objects list
        this.grpno = grpno;                // set index number of this group
        this.settings = p;                 // set pointer to settings
        ap = new Aperture();               // create an aperture.
        edgeBands = new float[4];            // empty edgeBands
        
        tester = new Tester(settings, 1361, 851, 3);
    }

    /**
     * Add a FitsImage to the group. The FITSImage is added to the group. If it is the first FITS in the group its
     * NAXIS and EXPTIME parameters are adopted by the group.
     *
     * @param f the FitsImage to be added.
     */
    public void add(FitsImage f) {
        fitsImages.add(f);
        if (fitsImages.size() == 1) {
            this.naxis1 = f.naxis1;
            this.naxis2 = f.naxis2;
            this.exptime = f.exptime;

        }
    }

    /**
     * Median stack the Fits images. The set if FITS in this group are median stacked with no tracking and the
     * results are put in aStack.
     */
    public void stack() {

        // create the Stacked Image objects if not already available
        if (aStack == null) {
            aStack = new StackedImage(settings);
        }
        if (oStack == null) {
            oStack = new StackedImage(settings);
        }
        if (dStack == null) {
          dStack = new StackedImage(settings);  
        }

        // initialise stacking
        int stackCount = fitsImages.size();                  // number of images in the group
        float[] pixList = new float[stackCount];             // pixel group for median selection
        int median = Math.round(stackCount / 2);             // index of median pixel in pixList

        // stack median
        for (int j = 0; j < naxis2; j++) {             
            for (int i = 0; i < naxis1; i++) {
                Arrays.fill(pixList, 0.0f);                 
                for (int k = 0; k < stackCount; k++) {
                    FitsImage fit = fitsImages.get(k);
                    int x = i - Math.round(fit.offset.x);
                    int y = j - Math.round(fit.offset.y);
                    if ((x > 0) && (x < naxis1) && (y > 0) && (y < naxis2)) {
                        pixList[k] = fit.pixels[x][y];
                    }
                }
                Arrays.sort(pixList);                        // sort pixList into value order
                aStack.pixels[i][j] = pixList[median];       // store the median pixel in the average stack
            }
        }

        // histogram the new stack
        aStack.doHist();
    }

    /**
     * The FITS in this group are stacked and tracked using the synthetic tracking parameters or
     * the motion/PA of a specific Mover.
     *
     * @param showMover False = use synthetic tracking parameters from the Settings. True = use motion and PA of
     * the Mover specified in Settings.targetMover.
     */
    public void reStack(boolean showMover) {
        // edgeBands will contain the sizes of the top/bottom/left/right edges that are not overlapped by all imaes
        Arrays.fill(edgeBands,  0.0f);
        
        // calculate the stacking offsets
        for (FitsImage fit : fitsImages) {
            fit.setSTOffset(groupRefTime, showMover, edgeBands);
        }

        // initialise stacking
        int stackCount = fitsImages.size();

        // stack avaerage
        for (int j = 0; j < naxis2; j++) {
            for (int i = 0; i < naxis1; i++) {
                float pix = 0;
                for (int k = 0; k < stackCount; k++) {
                    FitsImage fit = fitsImages.get(k);
                    int x = i - fit.STOffset.x;
                    int y = j - fit.STOffset.y;
                    if ((x > 0) && (x < naxis1) && (y > 0) && (y < naxis2)) {
                        pix += fit.pixels[x][y];
                    }
                }
                oStack.pixels[i][j] = pix / stackCount;
            }
        }
        // reestablish histogram
        oStack.doHist();
    }

    /**
     * Convert a stacked image into a BufferedImage stretched according to Black/White values set 
     * by the histogram. Exactly which version of the stack is drawn depends on user options currently
     * shown in the Settings. The entire stacked image is placed in the buffer and it is up to the Display
     * Manager to decide which bits to show.
     *
     * @return A buffered image of the stack ready to display.
     */
    public BufferedImage draw() {
        // create image buffer and graphics if not already available
        if (img == null) {
            img = new BufferedImage(naxis1, naxis2, BufferedImage.TYPE_INT_RGB);
            g = img.getGraphics();
        }

        // decide which stack to draw from depending on showType
        switch (settings.showType) {
            case 1:
                dStack = aStack;
                break;
            case 3:
                dStack = oStack;
                break;
            case 4:
                dStack = aStack;
                break;
        }

        // only redraw the image if drawn switch has been set off
        if (!dStack.drawn || settings.newMover) {
            float wb = dStack.white - dStack.black;
            /* draw each pixels in 0-255 grascale */
            float p;
            for (int j = 0; j < naxis2; j++) {
                for (int i = 0; i < naxis1; i++) {

                    // TODO logic of switches showType and object could be improved here
                    // get pixel from stack 
                    if (settings.objectOnly) {
                        p = oStack.bgrnd;
                    } else {
                        p = dStack.pixels[i][j];
                    }
                    // if showtype 4 add the pixel from the object stack
                    if (settings.showType == 4) {
                        p += (oStack.pixels[i][j] - oStack.bgrnd);
                    }
                    // set black and white limits
                    if (p > dStack.white) {
                        p = dStack.white;
                    }
                    if (p < dStack.black) {
                        p = dStack.black;
                    }
                    // convert to 0-255 greyscale and propigate same for R, G and B
                    int pix = (int) (255 * (p - dStack.black) / (wb));
                    pix = (pix << 16) | ((pix << 8)) | pix;
                    img.setRGB(i, j, pix);
                }
            }

            // if there is a Movers draw red rings round it
            Mover m = settings.targetMover;
            if (m != null) {
               g.setColor(Color.RED);
               int x = Math.round(m.objects.get(grpno - 1).location.x);
               int y = Math.round(m.objects.get(grpno - 1).location.y);
               g.drawOval(x - 7, y - 7, 14, 14);
            }
    
            // note that the image has been drawn */
            dStack.drawn = true;
        }
        return img;
    }

    /**
     * Scan the object-stack looking for potential objects. The stack produced from stack and track is 
     * scanned for potential objects. When a pixel over the threshold is found, the region around it is 
     * examined in detail and, subject to conditions, an ImageObject is created.
     */
    public void findObjects() {

       
        // Create an aperture with an appropriate track length and pa
        float track = (float) ((settings.motion * exptime) / (60.0f * settings.pixToArcsecs));
        ap.setCentroid(settings.aperture, track, settings.PA);

        // minPix is min pixels > threshold and includes extra pixels from the tracklength
        minPix = settings.tCount1 + (int) track;

        // A limit is set to ensure the range of pixels examined by the aperture do not go outside the edge of
        // the image when Centre of Brightness is adjusted.*/
        int limit = 4 * ap.apRadius;
        
        // The edgeBands created during stack & track are re-set to include the limit 
        edgeBands[0]+= limit;
        edgeBands[1]= naxis1 - (limit - edgeBands[1]);
        edgeBands[2]+= limit;
        edgeBands[3]= naxis2 - (limit - edgeBands[3]);
        

        // empty the object list
        objects.clear();

        // put a working copy of the Group Object stack in wStack 
        if (wStack == null) { wStack = new StackedImage(settings);}
        wStack.copy(oStack);
        
        // Scan image for a pixel over threshold, when found use CheckObject
        for (int j = (int) edgeBands[2]; j < edgeBands[3]; j++) {
            for (int i = (int) edgeBands[0]; i < edgeBands[1]; i++) {
                float f = wStack.pixels[i][j];
                if (f > wStack.thold) {
                    checkObject(i, j);
                }
            }
        }

        // make a note of how many objects were discovered (for display only)
        obCount = objects.size();
    }

   /**
     * Check details of suspected object and create an ImageObject if appropriate. 
     *
     * @param i x coordinate of region to be inspected.
     * @param j y coordinate of region to be inspected.
     */
    private void checkObject(int i, int j) {

       // working variables
       float c;                               // current centroid radius
       float flux;                            // flux inside the current centroid
       float requiredFlux;                    // minimum flux required to accept object
       int requiredPix;                       // minimum number of >threshold pixels
       PixelF cob = new PixelF(0, 0);         // centre of brightness correction
       Pixel toCob = new Pixel(i, j);         // centre of brightness
       int pCount;                            // number of pixels inside the centroid
       int tCount;                            // number of pixels > threshold inside the centroid
       float snr;                             // snr 
       float allFlux = 0;                     // flux in Full aperture
     
        
       // establish the minimum amount of net flux and thrshold pixels
       requiredFlux = minPix * (wStack.thold - wStack.bgrnd);
       requiredPix  = (int) Math.max(minPix * 0.5, 2.0);
       
       // get net flux in full sized aperture centred on (i,j) and test it is above minimum */
       flux = 0;
       for (PixelC p : ap.pixels) {
           float f = wStack.pixels[i + p.x][j + p.y];
           flux += f - wStack.bgrnd;
       }
       if (flux < requiredFlux) return;
        
       // Reduce aperture radius until it contains only the fwhm pixels. Adjust centre of brightness (COB) and 
       // check object properties at each step. Starting with the full aperture and working down in half-pixel steps.
        c = settings.aperture + 0.5f;
        while (true) {
                
            // get centre of brightness (COB) adjustment and reset position of aperture.
            cob.set(0.0f, 0.0f);
            for (PixelC p : ap.pixels) {
                if (p.distance > c) break;
                float f = (wStack.pixels[toCob.x + p.x][toCob.y + p.y] - wStack.bgrnd) / flux;
                cob.x += f * p.x;
                cob.y += f * p.y;
            }
            toCob.x += Math.round(cob.x);
            toCob.y +=Math.round(cob.y) ; 
            
            // if the COB has now drifted out of the aperture, quit */
            if ((ap.apRadius < Math.abs(toCob.x - i)) || (ap.apRadius < Math.abs(toCob.y - j))) return;
            
            // reduce aperture and re-establish flux and counts
            c -= 0.5f;            // current radius of aperture
            flux = 0;             // net flux in this aperture
            pCount = 0;           // number of pixels in this aperture
            tCount = 0;           // number of pix over threshold in this aperture
            for (PixelC p : ap.pixels) {
                if (p.distance > c) break;
                float f = wStack.pixels[toCob.x + p.x][toCob.y + p.y];
                if (f > wStack.thold) tCount++;
                f -= wStack.bgrnd;
                flux += f;
                pCount++;
            }
            
            if (c == settings.aperture) {allFlux = flux;}
            
            // quit if too few threshold pix
            if (tCount < requiredPix) return;
            
            // accept object if aperture full of threshold pixels (large object)
            if (tCount >= pCount) break;
            
            // accept object if required flux or thresholds found inside inside the fwhm. */
            if (pCount <= ap.fwhmCount) {
                if (flux > requiredFlux || tCount >= minPix) { break;} else {return;}
            }
        }

        // calculate object SNR as the flux inside the object over flux outside the object
        float outFlux = Math.max((allFlux - flux), wStack.stdev);
        snr = flux/outFlux;
        
        // create new object and add it to the object list
        ImageObject ob = new ImageObject();
        ob.flux = flux;
        ob.tCount = tCount;
        ob.obSize = pCount;
        ob.location.set(toCob.x, toCob.y);
        ob.objectSNR = snr;
        objects.add(ob);

        // set final aperture to backgroud so it will not trigger another object. */
        for (PixelC p : ap.pixels.subList(0, pCount)) {
            wStack.pixels[toCob.x + p.x][toCob.y + p.y] = wStack.bgrnd;
        }
}

    /**
     * Set the group reference time mid way between the start of the first exposure and the end of 
     * the last exposure and set the group elapse time from first to last FITSImage time stamps.
     */
    public void setRefTime() {
        int n = fitsImages.size();
        double d1 = fitsImages.get(0).JD.julian;
        double d2 = fitsImages.get(n - 1).JD.julian;
        groupElapse = (float) Math.max((d2 - d1) * 1440, exptime / 60);
        d2 += fitsImages.get(n - 1).exptime / 86400;
        groupRefTime.setDate((d1 + d2) / 2);
    }

    /**
     * Set stacks to re-draw next time a display image is required.
     */
    public void reDraw() {
        aStack.drawn = false;
        oStack.drawn = false;
    }

    /**
     * Supply information about the image size.
     *
     * @return The image size in pixels.
     */
    public Dimension getImageSize() {
        return new Dimension(naxis1, naxis2);
    }

}
