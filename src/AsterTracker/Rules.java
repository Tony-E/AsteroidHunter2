
/**********************************************************************************************************************
 *                                               Class Rules 
 *********************************************************************************************************************/
package AsterTracker;

/**
 * Class Rules assigns a score to a Mover. 
 *
 * @author Tony Evans
 */
public class Rules {

    private final Settings s;
    
    public Rules(Settings settings) {
        s = settings;
    }
    
    /**
     * Calculate score for mover. 
     * @param m The Mover to be tested.
     */
    public void testMover (Mover m) {
        double mean = 0;
        double sdev= 0;
        double rdev;
        // get mean snr */
        for (ImageObject ob : m.objects) {mean+= ob.objectSNR;}
        mean/=3.0;
        
        // get stdev snr */
        for (ImageObject ob : m.objects) {
            double d = ob.objectSNR;
            sdev+= (d - mean)*(d - mean);
        }
        sdev = Math.sqrt(sdev/2.0f);
        
        // get relative stdev
        rdev = sdev/mean;
        
        // score is mean/relative std dev divided by middle error
        m.score = (float) (mean/rdev)/m.errMid;
        
      
    }
}
