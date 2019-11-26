/*********************************************************************************************************************
 *                                             Class PixelF
 *********************************************************************************************************************/
package AsterTracker;

/**
* Class PixelF contains the coordinates of a position in an image using Float (sub pixel precision)
* together with various arithmetic methods.

* @author Tony Evans
 **/
public class PixelF {

    /** X coordinate. */
    public float x;

    /** Y coordinate. */
    public float y;

    /**
     * Constructor.
     * @param x
     * @param y
     */
    public PixelF(float x, float y ) {this.x=x;this.y=y;}                   // create pixel from x and y values
    public PixelF(PixelF p) {this.x = p.x; this.y = p.y;}                   // create pixel from another pixel

    /**
     * The coordinates of this pixel are set to the x, y coordinates specified. 
     * @param a The x coordinate.
     * @param b The y coordinate.
     */
    public final void set(float a, float b) {x=a; y=b;}                     // explicitly set the values of x and y

    /**
     * The coordinates of a are added to the coordinates of this pixel.
     * @param a The pixel to be added to this pixel.
     */
    public final void add(PixelF a)  {x+=a.x; y+=a.y;}                      // add a float pixel to this pixel

    /**
     * The coordinates of a are added to the coordinates of this pixel.
     * @param a
     */
    public final void add(Pixel a)   {x+=a.x; y+=a.y;}                      // add an int pixel to this pixel

    /**
     * The coordinates (a,b) are added to the coordinates of this pixel.
     * @param a
     * @param b
     */
    public final void add(int a, int b) {x+=a; y+=b;}            

    /**
     * This pixel's coordinates are set to those of pixel a.
     * @param a
     */
    public final void copy(PixelF a) {x=a.x;y=a.y;}                  

    /**
     * This pixel's coordinates are multiplied by factor f.
     * @param f
     */
    public final void mult(Float f)  {x*=f; y*=f;}                 

    /**
     * Calculate the distance between this pixel and pixel p.
     * @param p The other pixel.
     * @return The distance from this pixel to p.
     */
    public final float dist(Pixel p) {                                   
       return (float) Math.sqrt( (p.x-x)*(p.x-x) + (p.y-y)*(p.y-y)); }

    /**
     * Calculate the distance between this pixel and pixel p.
     * @param p
     * @return
     */
    public final float dist(PixelF p) {                                 
       return (float) Math.sqrt((x-p.x)*(x-p.x) + (y-p.y)*(y-p.y));}

    /**
     * Calculate the distance between two other pixels.
     * @param a 
     * @param b
     * @return The distance between a and b.
     */
    public final float dist(int a, int b) {                         
       return (float) Math.sqrt((x-a)*(x-a) + (y-b)*(y-b));}

    /**
     * Calculate the distance between this pixel and anoth at (a,b).
     * @param a
     * @param b
     * @return The distance between a and b.
     */
    public final float dist(float a, float b) {                    
       return (float) Math.sqrt((x-a)*(x-a) + (y-b)*(y-b));}

    /**
     * Calculates the square of the distance between this pixel and point (a,b). 
     * @param a
     * @param b
     * @return The square of the distance between a and b.
     */
    public final float distSqr(int a, int b) {                      
       return (float) (x-a)*(x-a) + (y-b)*(y-b);}

    /**
     * Calculate the square of the distance between this pixel and (a,b).
     * @param a
     * @param b
     * @return Square of the distance from a to b.
     */
    float distSqr(float a, float b) {                             
        return (float) (x-a)*(x-a) + (y-b)*(y-b);
    }
    
    /**
     * Calculates the position angle of pixel a relative to this pixel.
     * @param a
     * @return The position angle of a (radians).
     */
    public final float pa(PixelF a) {                            
       return (float) Math.atan2(x-a.x, y-a.y);}

}
