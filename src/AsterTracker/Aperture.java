/**********************************************************************************************************************
 *                                                  Class Aperture
 *********************************************************************************************************************/
package AsterTracker;

import java.util.ArrayList;
import java.util.Collections;

/**
 * <p>This class creates a synthetic aperture of given radius. The aperture is defined in terms of an 
 * ArrayList of PixelC's representing the offsets from (0,0) in a synthetic aperture.</p>
 * 
 * <p>The aperture is an oblong of length and orientation matching the motion and PA of the track. 
 * The pixel list is sorted to be in ascending distance from the track so that a scan of the pixel
 * list will "spiral" out from the centre of the track.</p>
 *
 * @author Tony Evans
 */
public class Aperture {

   /** The set of pixels that defines the aperture. */
    public ArrayList<PixelC> pixels;  

   /** The radius of a circle (in pixels) that would fully enclose the aperture. */
    public int apRadius;
    
   /** The number of pixels in the region of half aperture. */ 
    public int fwhmCount;
    
   /* The locations of the ends of the track relative to (0,0). */
    private final PixelF 
        c1 = new PixelF(0,0),
        c2 = new PixelF(0,0);    
  
   /**
    * Constructor.
    */
    public Aperture() {          
       pixels = new ArrayList();
       fwhmCount = 0;
    }

   /**
    * Generate an aperture of given radius, track length and position angle.
    * 
    * @param a Radius of aperture in pixels.
    * @param trackPix Length of track in pixels.
    * @param pa Position angle of track in radians.
    */
    public void setCentroid(int a, float trackPix, float pa ) {
           
        // The FWHM is taken to be 40% of the full aperture
        int fwhmAp = (int) Math.round(0.4*a);
        fwhmCount = 0;
        
        // set the positions of the ends of the track C1 and C2
        float x = (float) (0.5f*trackPix*Math.sin(pa));
        float y = (float) (0.5f*trackPix*Math.cos(pa));
        c1.x = -x; c1.y = -y;
        c2.x = x;  c2.y = y;
        
        // clear any preexisting pixels 
        pixels.clear(); 
        
        // calc distance between track ends
        float CC  = c1.dist(c2);       
        
        // calculate the size of radius that could contain the entire aperture 
        int r = (int) (a+Math.ceil(CC/2.0f));
        
        // examine pixels inside r to see if they fall within given distance from the track 
        float dd;
        for (int j=-r; j<(r+1); j++) {
            for (int i=-r; i<(r+1); i++) {
                
               // if i and j are zero insert central pixel 
                if ((j==0) && (i==0)) {
                    pixels.add(new PixelC(0,0,0));
                    fwhmCount++;
               // else calculate distance from track
                } else {
                    // calculate distances of (i,j) to each track-end
                    double PC1 = c1.distSqr(i,j);
                    double PC2 = c2.distSqr(i,j);
               
                    // test for obtuse angle 
                    if (Math.abs(PC1-PC2)>(CC * CC)) {
                        // if obtuse, use distance to nearest trackend 
                        dd=(float) Math.sqrt(Math.min(PC1,PC2));
                    } else {
                        // else use Herons formula to get height of triangle.
                        // (double precision must be used else it does not work well)
                        PC1 = Math.sqrt(PC1);
                        PC2 = Math.sqrt(PC2);
                        double s  = (PC1+PC2+CC)/2.0f;
                        double A  = Math.sqrt(s*(s-PC1)*(s-PC2)*(s-CC));
                        dd =  (float) (2*A/CC);
                    }
                    
                    // if position (i,j) is inside aperture add a pixel to the list. 
                    if (dd <= a) {pixels.add(new PixelC(i,j,dd));}
                    // if position (i,j) is inside FWHM add to the fwhm count
                    if (dd <= fwhmAp) {fwhmCount++;}
                }
            }
        }
        
        // set apRadius to ensure the edge of the aperture cannot be off the image
        apRadius = r+1;
        
        // sort the pixels in order of distance from the track
        Collections.sort(pixels);
   }
}

