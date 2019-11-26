/*********************************************************************************************************************
 *                                          Class StackedImage
 * *******************************************************************************************************************/
package AsterTracker;

import java.util.Arrays;

/**
 * Class StackedImage contains the results of a stacking operation on a set of FIISImages 
 * or a Superstack from a stack of stacks.
 *
 * @author Tony Evans
 */

public class StackedImage {

    private final Settings settings;

   /** Image width. */
    public int width;
   /** Image height. */
    public int height;
   /** Image pixel values. */
    public float[][] pixels;  
    /** Black level for display. */
    public float black;                   
    /** White level for display. */
    public float white;              
    /** Background level. */
    public float bgrnd;     
    /** Mean value of pixels. */
    public float mean;                
    /** Threshold for object detection. */
    public float thold;                    
    /** Standard deviation of the noise (noise profile sigma). */
    public float stdev;                         
   /** When false, the buffered image of this stack needs to be redrawn. */
    public boolean drawn=false;                  

   /**
    * Constructor. Set up fields and pointers.
    * @param s pointer to Settings
    */
    public StackedImage(Settings s) {
        width=s.naxis1;
        height=s.naxis2;
        this.settings = s;
        pixels = new float[width][height];
    }

   /**
    * Copy the referenced stacked image into this stacked image.
     * @param img The image to be copied.
    */
    public void copy(StackedImage img) {
        black = img.black;
        white = img.white;
        bgrnd = img.bgrnd;
        mean =  img.mean;
        thold = img.thold;
        stdev = img.stdev;
        for (int i=0; i<width; i++) {
            System.arraycopy(img.pixels[i], 0, pixels[i], 0, height);
        }
    }

   /**
    * A histogram of this image is calculated and black, white, background and standard deviation
    * of the noise profile are calculated.
    */
    public void doHist() {

        // create the buckets for the histogram
        int factor = 1024;
        int[] hist = new int[factor+1];
        Arrays.fill(hist,0);

        // load the histogram buckets  */
        for (int j = 0; j<height; j++) {
            for (int i = 0; i<width; i++) {
                   hist[(int) (factor*pixels[i][j])]++;
            }
        }

        // get number of pixels to account for, excluding zero (black) and 1 (white) values
        double pixCount = width*height - (hist[0] + hist[factor]);

        // prepare to scan the histogram
        int e1=0; int e2=0;                    // working counters
        double med = pixCount * 0.5;           // count of pixels to the median
        double dev = pixCount * 0.8413;        // count pixels to 1 sigma above median

        // count up to median and median+sigma points in the histogram
        for (int i=1; i<factor; i++) {if (med<(e1+=hist[i])) {bgrnd = i; break;}}
        for (int i=1; i<factor; i++) {if (dev<(e2+=hist[i])) {stdev = i; break;}}

        //  set the results
        stdev = (float) ((stdev-bgrnd)/factor);
        bgrnd = (float) (bgrnd/factor);
        black = Math.max(0,bgrnd-stdev*settings.blackHist);
        white = Math.min(1,bgrnd+stdev*settings.whiteHist);
        thold = Math.min(1,bgrnd+stdev*settings.sigma1);
    }
}
