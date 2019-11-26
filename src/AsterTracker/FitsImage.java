/**********************************************************************************************************************
 *                                                 Class FitsImage
 *********************************************************************************************************************/
package AsterTracker;

import AsterUtil.DateTime;
import AsterUtil.SphCoordinate;
import java.util.Arrays;

/**
 * Class FITSImage represents a single FITS Image including pixel values, FITS Keywords 
 * and a range of processing options.
 * 
 * <p>The class is responsible to:</p>
 * <ul>
 *   <li>Provide a copy of itself on demand.
 *   <li>Calculate alignment offset parameters for stacking and synthetic tracking.</li>
 *   <li>Calculate background and noise profile data from a histogram.</li>
 *   <li>Run a de-lining algorithm on request.</li>
 *   <li>Run a Median 3x3 noise reduction filter on request.</li>
 *   <li>Run a star-subtraction algorithm on request.</li>
 *   <li>Run a flat field division algorithm on request.</li>
 *   <li>Run a stretch algorithm on request.</li>
 * </ul>
 * <p>FITSImages are created by the FITSLoader and each is assigned to an ImageGroup.</p> 
 *
 * @author Tony Evans
 */

public class FitsImage {
    /*  filter kernel */
    private final static double[] gaus = {0.062147, 0.124294, 0.254237};   // Gaussian


    /* Keyword values from the Fits Header */
    int         bitpix=16;         // BITPI keyword ( the rest of the proram assumes this is alwas 16)
    int         naxis1;            // NAXIS1 keyword (the x axis or width of the image)
    int         naxis2;            // NAXIS2 keyword (the y axis or height of the image)
    int         bzero=0;           // BZERO keyword
    double      bscale=1;          // BSCALE keyword
    float       exptime;           // EXPTIME keyword;
    DateTime    JD;                // JD keyword
    Double      cdelt1;            // CDELT1 keyword - scale in x direction in RA radians per pixel
    Double      cdelt2;            // CDELT2 keyword - scale in y direction in Dec radians per pixel
    Double      crota2;            // CROTA2 axis rotation (currently assumed to be zero)
    String      filter;            // FILTER keyword
    int         cblack;            // CBLACK keyword
    int         cwhite;            // CWHITE keyword
    int         pedestal;          // PEDESTAL keyword
    String      telescop;          // TELESCOP keyword

    /* calculated alignment parameters */
    SphCoordinate fitsRef    = new SphCoordinate(0.0,0.0); // reference coordinates (RA, Dec) in radians
    Pixel         fitsRefPix = new Pixel(0,0);             // reference pixel (x,y) from CRVAL1 & 2

    /** Offset of this image from the common alignment with no tracking. */
    public PixelF offset = new PixelF(0,0);       

    /** Offset of this image from the common alignment with current synthetic tracking values. */
    public Pixel  STOffset = new Pixel(0,0);               // offset for stack and track

    /** normalisation parameters */
    float bgrnd;                  // background
    float stdev;                  // noise profile sigma
    float fitBlack;               // nominal black level
    float fitWhite;               // nominal while level
    float mean;                   // mean value of pixels after fixed stars have been subtracted
    
    
    private final Settings settings;      // pointer to settings
    private boolean normalised;           // set true once pixel data has been nomalised to range 0 - 1.
    private float initialBgrnd;           // first calculation of backgroun

    /** The pixel array stored as float (values 0 - 1). */
    public float[][] pixels;

    /**
     * Constructor initialises the array of pixels, stores height, width and pointer to settings.
     * @param w width
     * @param h height
     * @param p settings
     */
    public FitsImage(int w, int h, Settings p) {
        pixels = new float[w][h];
        JD = new DateTime();
        naxis1=w;
        naxis2=h;
        settings = p;
        normalised = true;
    }

    /**
     * Simple copy of pixels from another FitsImage. The pixels in image f are copied to this image.
     * @param f the FitsImage from which to copy.
     */
    public void copy(FitsImage f) {
        for (int j = 0; j<naxis2; j++) {
            for (int i = 0; i<naxis1; i++) {
                pixels[i][j] = f.pixels[i][j];
            }
        }
    }
    
     /**
     * Establish Black and White (stretch) levels using a histogram.
     * 
     * <p> The histogram is built using the full range of possible values then re-run after sigma clipping. Black,
     * and white levels are set according to user settings. </p>
     */
    public void doHist() {
       // there are 65536 histogram buckets initialised to zero
       int[] hist = new int[65536];
       Arrays.fill(hist,0);

       // load the histogram. Pixel value = bucket number 
       for (int j = 0; j<naxis2; j++) {
           for (int i = 0; i<naxis1; i++) {
                  hist[(int) pixels[i][j]]++;
           }
       }

       // establish number of non-zero pixels
       double pixCount = naxis1*naxis2 - hist[0];           
       

       // prepare to scan the histogram 
       int e1=0; int e2=0;                     // working counters
       double med = pixCount*0.5;              // count of pixels to the median
       double dev = med*0.0455;                // count of pixels to 2 sigma below median

       // establish first estimate median and median- 2-sigma points in the histogram
       for (int i=1; i<65535; i++) {if (med<(e1+=hist[i])) {bgrnd = i; break;}}
       for (int i=1; i<65535; i++) {if (dev<(e2+=hist[i])) {stdev = i; break;}}

       // first estimate of sigma
       stdev = (bgrnd-stdev)/2;

       // clear out pixels below median-3 sigmas and repeat as above for new estimate of background and sigma
       for (int i=0; i<(bgrnd-settings.blackFits*stdev); i++) {pixCount-=hist[i]; hist[i]=0;}
       med = pixCount*0.5;                             
       dev = med*0.0455;                              
       e1=0; e2=0;
       for (int i=0; i<65535; i++) {if (med<(e1+=hist[i])) {bgrnd = i; break;}}
       for (int i=0; i<65535; i++) {if (dev<(e2+=hist[i])) {stdev = i; break;}}
       stdev = (bgrnd-stdev)/2;

       /* set fitBlack, fitWhite as specified sigma multiples away from the background */
       if (!normalised) {initialBgrnd = bgrnd; normalised = true;}
       fitBlack = Math.max(0,bgrnd-stdev*settings.blackFits);
       fitWhite = Math.min(65535,bgrnd+stdev*settings.whiteFits);
    }

    /**
     * Stretch the pixel range to 0=black to 1=white at float precision.
     */
    public void doStretch() {
        for (int j = 0; j<naxis2; j++) {
            for (int i = 0; i<naxis1; i++) {
               // get pixel an set it within stretch range */
                float pix = pixels[i][j];
                if (pix>fitWhite) {pix=fitWhite;}
                if (pix<fitBlack) {pix=fitBlack;}
                pix = (pix-fitBlack)/(fitWhite - fitBlack);
                pixels[i][j] = pix;
            }
        }
       
        // reset stretched value of background and black white limits */
        bgrnd = (bgrnd - fitBlack)/(fitWhite - fitBlack);
        fitBlack = 0;
        fitWhite = 1;

    }

    /**
     * Apply a simple 3x3 Gaussian blur filter.
     */
    public void doBlur() {   
        if (settings.doBlur) {
            FitsImage fib = new FitsImage(naxis1,naxis2, settings);
            for (int j = 1; j<(naxis2-1); j++) {
                for (int i = 1; i<(naxis1-1); i++) {
                   /* Gaussian filter 3x3 */
                    fib.pixels[i][j] = (float) ((pixels[i-1][j-1]+pixels[i-1][j+1]+pixels[i+1][j-1]+pixels[i+1][j-1]) * gaus[0]);
                    fib.pixels[i][j] += (float) ((pixels[i-1][j]+pixels[i+1][j]+pixels[i][j+1]+pixels[i][j-1]) * gaus[1]);
                    fib.pixels[i][j] += (float) (pixels[i][j] * gaus[2]);
                }
            }
            copy(fib);
        }
    }
    
    /**
     * Apply a 3x3 median noise filter (not currently used)
     */
    public void doAltBlur() {
        if (!settings.doBlur) {return;}
        float[] pixList = new float[9]; 
        FitsImage fib = new FitsImage(naxis1,naxis2, settings);
        for (int j = 1; j<(naxis2-1); j++) {
            for (int i = 1; i<(naxis1-1); i++) {
                pixList[0] = pixels[i-1][j-1]; 
                pixList[1] = pixels[i-1][j];
                pixList[2] = pixels[i-1][j+1];
                pixList[3] = pixels[i][j-1];
                pixList[4] = pixels[i][j];
                pixList[5] = pixels[i][j+1];
                pixList[6] = pixels[i+1][j-1];
                pixList[7] = pixels[i+1][j];
                pixList[8] = pixels[i+1][j+1];
                Arrays.sort(pixList);
                fib.pixels[i][j] = pixList[4];
            }
        }
        copy(fib);
   }
    
   /**
    * Set the stacking offsets for zero tracking motion based on RA/Dec offset and rotation angle.
    */
    public void setStaticOffset() {
        double cosr = Math.cos(crota2);
        double sinr = Math.sin(crota2);
         /* RA/DEC alignment offset in radians */
          SphCoordinate temp = new SphCoordinate(0.0,0.0);
          temp.copy(fitsRef);
          temp.minus(settings.refPoint);
         /* RA/DEC alignment offset in pixels */
          temp.coord[0]/=cdelt1;
          temp.coord[1]/=cdelt2;
         /* convert to x,y offset in stacked image */
          offset.x =  (float) (temp.coord[0] * cosr - temp.coord[1] * sinr);
          offset.y =  (float) (temp.coord[0] * sinr + temp.coord[1] * cosr);
    }

   /**
    * Set the stacking offsets for stack and track.
    * @param groupDate The date-time of the middle of the stacking group.
    * @param showMover Indicates whether to use the track of a known Mover or use the track set by synthetic tracking.
    */
    public void setSTOffset(DateTime groupDate, boolean showMover, float[] offsets) {
       /* calculate distance to be tracked in pixels */
        float dTime = (float) ((JD.julian - groupDate.julian) * 1440);
        float dist = (float) (dTime * settings.getMotion(showMover)/settings.pixToArcsecs );

       /* calculate pixel offset (x,y) based on distance and PA + original stacking offset */
        STOffset.x = (int) Math.round(offset.x +(dist * Math.sin(settings.getPA(showMover))));
        STOffset.y = (int) Math.round(offset.y +(dist * Math.cos(settings.getPA(showMover))));
        
       /* maintain list of max and min offsets */ 
        offsets[0] = Math.max(offsets[0], STOffset.x);
        offsets[1] = Math.min(offsets[1], STOffset.x);
        offsets[2] = Math.max(offsets[2], STOffset.y);
        offsets[3] = Math.min(offsets[3], STOffset.y);
    }

   /**
    * Subtract an image (remove fixed stars). 
    * 
    * <p>The superStack contains fixed stars. It is aligned to this image and is subtracted 
    * from this image pixel by pixel except that, where the superStack pixel is above the superStack threshold the
    * corresponding pixel in this image is set to this image´s background.</p>
    * 
    * <p> The effect is that the bright central regions of stars in the superStack are totally removed from this image 
    * while their halos are just reduced. </p> 
    * 
    * @param superStack The StackedImage to be subtracted.
    */
    public void subtract(StackedImage superStack) {
        mean = 0.0f;
        for (int j = 0; j<naxis2; j++) {
            for (int i =0; i<naxis1; i++) {
                int x = i - Math.round(offset.x);
                int y = j - Math.round(offset.y);
                if ((x>0) && (x<naxis1) && (y>0) && (y<naxis2)) {
                    float p = pixels[x][y];
                    float q = superStack.pixels[i][j];
                    if (q > superStack.thold) {
                        p = this.bgrnd;
                    } else {
                        p-=(q - superStack.bgrnd);
                        if (p>1) {p=1;}
                        if (p<0) {p=0;}
                    }
                    pixels[x][y] = p;
                    mean+=p;
                }
            }
        }
       /* note that the mean is used when calculating the superstack used as a flat field */
        mean = mean/(naxis1*naxis2);
    }
    
    /**
     * Reduce vertical lines. 
     * 
     * <p>Each column of the image is adjusted so that its background value is the same as the
     * overall background. This only works if the lines run all the way from top to bottom and bright objects do not
     * occupy more than half the pixels. Note this function relies on the histogram already having been run.</p>
     * 
     */
    public void doLines() {
        if (!settings.deLine) {return;}
        float colMedian;
        float[] pixList = new float[naxis2];
        int median = (int) naxis2/2;
        for (int i=0; i<naxis1; i++) {
            for (int j=0; j<naxis2; j++) {
                pixList[j] = pixels[i][j]/bgrnd;
            }
            Arrays.sort(pixList);
            colMedian = pixList[median];
            for (int j=0; j<naxis2; j++) {
                pixels[i][j]/=colMedian;
            }
        }
    }

    /**
     * Divide by an image (apply flat field).
     * 
     * <p> The superStack contains a flat field image. It is divided into this image pixel by pixel as long
     * as this does not cause a pixel value to go outside the range 0 - 1. </p>
     * 
     * @param superStack The flat field image.
     */
    public void divide(StackedImage superStack) {
        if (!settings.flatten) return;
        for (int j = 0; j<naxis2; j++) {
            for (int i =0; i<naxis1; i++) {
                float p = pixels[i][j];
                float q = superStack.pixels[i][j];
                if (q>0) {p=p/q;}
                if (p>1) {p=1;}
                if (p<0) {p=0;}
                pixels[i][j] = p;
            }
        }
    }
}
