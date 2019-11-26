package AsterTracker;

import java.util.ArrayList;

/**
 * Class Mover represents a possible moving object.
 * 
 * @author Tony Evans
 */
public class Mover implements Comparable<Mover>  {

    /** ImageObjects that lead to this detection. */
    public ArrayList<ImageObject> objects;          

    /** Calculated Motion "/m. */
    public float motion = 0;           

    /** Calculated PA in degrees. */
    public float PA = 0;                            

    /** Positional error of middle object compared to perfectly straight track. */
    public float errMid = 0;             

    /** User decision Ok or not. */
    public boolean status = false;  
    
    public Boolean deleted = false;

    /** Calculated score. */
    public float score = 10;                      

    /**
     * Constructor - creates an empty Mover.
     */
    public Mover() {
        objects = new ArrayList();
    }
    /**
     * Looks to see if the another Mover could be on the same track.
     * @param mov The Mover to be compared.
     * @param distance Distance less than this is considered a match.
     * @return True or false depending if this should be considered the same Mover.
     */
    public boolean isSameAs(Mover mov, float distance) {
        float d = this.objects.get(0).location.dist(mov.objects.get(0).location)
                + this.objects.get(1).location.dist(mov.objects.get(1).location);
        return d < distance;
    }
    
    /**
     * Compare is overridden to sort by score.
     * @param m
     * @return
     */
    @Override
    public int compareTo(Mover m) {
        return Float.compare(m.score, this.score); 
    }
}