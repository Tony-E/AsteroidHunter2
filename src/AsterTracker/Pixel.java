package AsterTracker;

/**
 * Class Pixel contains the coordinates of a position in an image using integer coordinates together 
 * with pixel arithmetic methods.
 * 
 * @author Tony Evans
 **/
public class Pixel {

    /** The x coordinate. */
    public int x;

    /** The y coordinate. */
    public int y;

    /**
     * Construct pixel from given coordinates.
     * @param x
     * @param y
     */
    public Pixel(int x, int y ) {this.x=x;this.y=y;}                   // create pixel from x and y values

    /**
     * Construct pixel from another pixel.
     * @param p
     */
    public Pixel(Pixel p) {this.x = p.x; this.y = p.y;}                // create as copy of existing pixel

    /**
     * Set this pixel to the sum of two other pixels.
     * @param a
     * @param b
     */
    public final void sum(Pixel a, Pixel b) {x=a.x+b.x;y=a.y+b.y;}     // set to sum of two pixels

    /**
     * Add the coordinates of another pixel.
     * @param a
     */
    public final void add(Pixel a) {x+=a.x; y+=a.y;}                   // add a pixel

    /**
     * Copy the coordinates from another pixel.
     * @param a
     */
    public final void copy(Pixel a) {x=a.x;y=a.y;}                     // set equal to another pixel

    /**
     * Calculate the distance from this pixel to another pixel.
     * @param p
     * @return The distance from this pixel to pixel p.
     */
    public final float dist(Pixel p) {
       return (float) Math.sqrt( (p.x-x)*(p.x-x) + (p.y-y)*(p.y-y));
   }

    /**
     * Set this pixel's coordinates to the minimum of its existing coordinates or
     * those of another pixel.
     * @param a
     */
    public void min(Pixel a) {
       x=Math.min(x, a.x);
       y=Math.min(y, a.y);
   }

    /**
     * Set this pixel's coordinates to the maximum of its existing coordinates or 
     * those of another pixel.
     * @param a
     */
    public void max(Pixel a) {
       x=Math.max(x, a.x);
       y=Math.max(y, a.y);
   }

    /**
     * Calculate the position angle from this pixel to another pixel.
     * @param a
     * @return The position angle of pixel a (radians).
     */
    public final float pa(Pixel a) {
       return (float) Math.atan2(x-a.x, y-a.y);}                      // position angle
}
