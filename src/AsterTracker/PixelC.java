package AsterTracker;

/********************************************************************************************************************
 * Class PixelC represents the relative coordinates of a position in a Centroid together with its distance
 * from the track. It overrides comPareTo() to allow sorting into distance from track. 
 *
 * @author Tony Evans
 **/
public class PixelC implements Comparable<PixelC> {

    /** The x coordinate. */
    public int x;

    /** The y coordinate. */
    public int y;

    /** The distance of this pixel from the track of a potential object. */
    public float distance;
    
   /**
    * Constructor.
    * @param x x coordinate relative to centre of centroid.
    * @param y y coordinate relative to centre of centroid.
    * @param d d distance from track in pixels.
    */
   public PixelC(int x,int y,float d) {
       this.x = x;
       this.y = y;
       this.distance = d;
   }

   /**
    * Compare is overridden to sort on distance from track.
    * @param p
    * @return
    */
   @Override
   public int compareTo(PixelC p) {
       if (p.distance == distance) {return 0;}
       if (p.distance>distance) {return -1;} else {return 1;}
   }
}
