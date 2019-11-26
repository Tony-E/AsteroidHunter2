/*********************************************************************************************************************
 *                                               Class Tracklet
 ********************************************************************************************************************/
package AsterTracker;

/**
 * Class Tracklet is a connection between an object in one image and an object in another
 * image such that they may be one moving object with specified motion and position angle.
 *
 * @author Tony Evans
 */
public class Tracklet {

   /** Object at start of Tracklet. */
    public ImageObject obj1;

   /** Object at end of Tracklet. */
    public ImageObject obj2;

   /** Motion in arcseconds per minute. */
    public float motion;

   /** Position Angle in radians. */
    public float PA;

   /**
    * Constructor stores all the given attributes of the Tracklet.
    * @param o1 Object at start of Tracklet.
    * @param o2 Object at end of Tracklet.
    * @param m Motion in arcminutes per minute..
    * @param p Position angle in radians.
    */
    public Tracklet(ImageObject o1, ImageObject o2, float m, float p) {
        obj1 = o1;
        obj2 = o2;
        motion = m;
        PA = p;
    }
}
