/*********************************************************************************************************************
 *                                          Class Supergroup
 * *******************************************************************************************************************/
package AsterTracker;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JTextArea;

/**
 * Class SuperGroup owns the ImageGroups and performs processing at the cross-group level. Only one 
 * thread of Supergroup can be created.
 * <p>
 * This includes:</p>
 * <ul>
 * <li>Setting up the ImageGroups and loading them with FITSImages</li>
 * <li>Establishing cross-group reference data and settings.</li>
 * <li>Normalising pixel values across all the FITS Images.</li>
 * <li>Creating a stars-only Supergroup Stacked Image.</li>
 * <li>Creating a flat field Supergroup Stacked Image.</li>
 * <li>Creating Tracklets from Image Objects</li>
 * <li>Creating Movers from Tracklets.</li>
 * <li>Deciding which Mover should be shown to the user next.</li>
 * </ul>
 *
 * @author Tony Evans
 */
public class SuperGroup {

    /** Superstack image. */
    public StackedImage sStack;

    /** List of Movers so-far discovered. */
    public final ArrayList<Mover> movers;
    
    /** Count of Tracklets from groups 1 and 2. */
    public int trk0Count =0;      
    /** Count of Tracklets from groups 2 and 3. */
    public int trk1Count =0;   
    
    /* local properties */
    private final Settings settings;                     // pointer to Settings
    private final JTextArea comment;                     // pointer to area in which to place comments
    private ArrayList<ImageGroup> groups;                // the ImageGroups
    private final ArrayList<Tracklet> tracklets1;        // Tracklets formed from group 1 and 2 objects
    private final ArrayList<Tracklet> tracklets0;        // Tracklets formed from group 2 and 3 objects
    private int nextMover;                               // index to the next Mover to be displayed
    private final Rules rules;                           // the Rules that assign a score to a Mover

    private final DecimalFormat df1 = new DecimalFormat("##0.00");
    private final float pi2 = (float) Math.PI * 2;

    /**
     * Constructor initialises fields and pointers.
     *
     * @param s    Pointer to Settings.
     * @param txt  Pointer to text area for commentary.
     */
    public SuperGroup(Settings s, JTextArea txt) {
        settings = s;
        comment = txt;
        movers = new ArrayList();
        nextMover = -1;
        tracklets0 = new ArrayList<>();
        tracklets1 = new ArrayList<>();
        rules = new Rules(settings);
    }

    /**
     * Create a set of ImageGroups and populates them with FITSImages using a FITSLoader.
     * @return ArrayList containing a list of ImageGroups.
     */
    public ArrayList<ImageGroup> initGroups() {
        // create ImageGroup list
        groups = new ArrayList<>();

        // Load the Fits files into groups using a FitsLoader
        FitsLoader fl = new FitsLoader(settings);
        fl.selectFiles();
        fl.loadFiles(groups, comment);
        return groups;
    }

    /**
     * Creates a median Superstack from all the Group Stacks.
     */
    public void superStack() {

        // create a superStack if not already available
        if (sStack == null) {
            sStack = new StackedImage(settings);
        }

        // initialise stacking
        float[] pixList = new float[3];        
        int median = 1;                       

        // median stack the group average stacks into a superstack
        for (int j = 0; j < settings.naxis2; j++) {
            for (int i = 0; i < settings.naxis1; i++) {
                // collect one pixel from each image, sort by value, select the middle one
                Arrays.fill(pixList, 0.0f);
                for (int k = 0; k < 3; k++) {
                    pixList[k] = groups.get(k).aStack.pixels[i][j];
                }
                Arrays.sort(pixList);
                sStack.pixels[i][j] = pixList[median];
            }
        }

        // do a histogram to get background and stdev values
        sStack.doHist();

        // the Superstack threshold is set to detect the bright central regions of stars that should be masked
        sStack.thold = Math.min(1, sStack.bgrnd + sStack.stdev * settings.sigma2);
    }

    /**
     * Set up cross-group reference data in the settings.
     */
    public void setReference() {

        // Number of groups
        int gSize = groups.size();

        // Reference coordinate (ra, dec) is mid way between first and last of all FITS 
        ImageGroup first = groups.get(0);
        ImageGroup last = groups.get(gSize - 1);
        int f = last.fitsImages.size() - 1;
        settings.refPoint = first.fitsImages.get(0).fitsRef.getMiddle(last.fitsImages.get(f).fitsRef);

        // Build array of time-differences between groups (for use in tracklet analysis)
        settings.dTime = new float[groups.size() - 1];
        for (int i = 0; i < (groups.size() - 1); i++) {
            settings.dTime[i] = (float) (1440.0 * (groups.get(i + 1).groupRefTime.julian - groups.get(i).groupRefTime.julian));
        }

        // Set common parameters in settings based on the first fits file
        ImageGroup g = groups.get(0);
        FitsImage fit = g.fitsImages.get(0);
        settings.expTime = fit.exptime;
        settings.naxis1 = fit.naxis1;
        settings.naxis2 = fit.naxis2;
        settings.crota2 = fit.crota2;

        // Initialise the synthetic tracking parameters
        settings.initSynthetic();
    }

    /**
     * Normalise the FITS Images. Adjusts ADU values of pixels so that the background value is the same in all the
     * images.
     */
    public void normalise() {

        //  Establish the average background level across all the FITS Stacks
        float mBgrnd = 0;
        for (ImageGroup g : groups) {
            for (FitsImage f : g.fitsImages) {
                mBgrnd += f.bgrnd;
            }
        }
        mBgrnd = mBgrnd / settings.fitsCount;

        //  Adjust pixel values to align background values
        for (ImageGroup g : groups) {
            for (FitsImage f : g.fitsImages) {
                float adjustment = f.bgrnd - mBgrnd;
                for (int j = 0; j < settings.naxis2; j++) {
                    for (int i = 0; i < settings.naxis1; i++) {
                        float v = f.pixels[i][j] - adjustment;
                        if (v < 0) v = 0f;
                        if (v > 1) v = 1f;
                        f.pixels[i][j] = v;
                    }
                }
                f.bgrnd = mBgrnd;
            }
        }
    }
    
    /**
     * Create a synthetic flat Superstack.
     */
    public void flatten() {

        // quit if the flat is not needed 
        if (!settings.flatten) return;

        // create a StackedImage for the superStack if not already available
        if (sStack == null) {
            sStack = new StackedImage(settings);
        }

        // Array to sort pixels for median stack
        float[] pixList = new float[settings.fitsCount];
        int k;

        // The median will come from the middle value 
        int median = Math.round(settings.fitsCount * 0.5f) - 1;

        // Median stack all the fits with no alignment
        for (int j = 0; j < settings.naxis2; j++) {
            for (int i = 0; i < settings.naxis1; i++) {
                Arrays.fill(pixList, 0.0f);
                k = 0;
                for (ImageGroup grp : groups) {
                    for (FitsImage fit : grp.fitsImages) {
                        pixList[k] = (fit.pixels[i][j] / fit.mean);
                        k++;
                    }
                }

                // sort the list of pixel values and select the median for the SuperStack
                Arrays.sort(pixList);
                sStack.pixels[i][j] = pixList[median];
            }
        }
    }
    
    /**
     * Scan the ImageObjects detected by the ImageGroups and try to build Tracklets from them.
     */
    public void findTracks() {

        // clear old Tracklets 
        tracklets0.clear();
        tracklets1.clear();

        // scan objects in each group against objects in the next group to create tracklets.
        for (int g = 0; g < 2; g++) {

            // get pointers to objects from the groups
            ArrayList<ImageObject> objects1 = groups.get(g).objects;
            ArrayList<ImageObject> objects2 = groups.get(g + 1).objects;
            
            // establish expected distance between objects and the tolerance
            float eDistance = settings.motion * settings.dTime[g];
            float dDistance = (0.5f * settings.motionStep * settings.dTime[g]) 
                    + (2.0f * settings.posErr * settings.pixToArcsecs);
            
            //establish tolerance in PA compared to that used for stacking
            float dPA = settings.PAStep/2.0f + 2.0f * settings.posErr * settings.pixToArcsecs / eDistance;
                    
            // scan for valid object pairs consistent with tolerances
            for (ImageObject ob1 : objects1) { 
                for (ImageObject ob2 : objects2) {
                    
                    // establish actual distance between objects and test it is within tolerance
                    float dist = ob1.location.dist(ob2.location) * settings.pixToArcsecs;
                    if (Math.abs(dist - eDistance) > dDistance) continue;
                    
                    // establish actual PA and check it is within tolerance
                    float myPA = ob1.location.pa(ob2.location);
                    if (myPA < 0.0f) myPA += pi2;
                    if (Math.abs(myPA - settings.PA) > dPA) continue;

                    // create a new Tracklet 
                    float myMotion = dist / settings.dTime[g];
                    Tracklet t = new Tracklet(ob1, ob2, myMotion, myPA);
                    if (g == 0) {tracklets0.add(t);} else {tracklets1.add(t);}
                }
            }
        }
    }

    /**
     * Scan the Tracklets to see if they can be joined up to form Movers.
     */
    public void findMovers() {
        
        // note how many tracklets we are dealing with (for info) 
        trk0Count = tracklets0.size();
        trk1Count = tracklets1.size();
        
        Mover sm = new Mover();                  // similar mover already in the list
        PixelF mid = new PixelF(0,0);            // ideal position of middle object
        float errMid;                            // error in posiition of middle object
        
        // proportion of total elapse time between group 1 and 2
        float pTime = settings.dTime[0] / (settings.dTime[0] + settings.dTime[1]);  

        // scan tracklets and look for common central object
        for (Tracklet t1 : tracklets0) {
            for (Tracklet t2 : tracklets1) {
                if (t1.obj2.equals(t2.obj1)) {  

                    // calculate and check error in position of middle object
                    mid.copy(t1.obj1.location);
                    mid.x += (t2.obj2.location.x - mid.x) * pTime;
                    mid.y += (t2.obj2.location.y - mid.y) * pTime;
                    errMid = t1.obj2.location.dist(mid);
                    if (errMid > (2.0f * settings.posErr)) continue;
                    
                    
                    // create Mover
                    Mover mov = new Mover();
                    mov.objects.add(t1.obj1);
                    mov.objects.add(t1.obj2);
                    mov.objects.add(t2.obj2);
                    mov.motion = (t1.motion + t2.motion) / 2.0f;
                    mov.PA = t1.obj1.location.pa(t2.obj2.location);
                    if (mov.PA < 0.0f) mov.PA += pi2;
                    mov.errMid = errMid;
                    
                    // calculate Mover's score from rules 
                    rules.testMover(mov);

                    // check if this mover was already found in an earlier scan. set action code depending on which has
                    // the better score: 0=add new mover, 1=keep existing mover, 2= replace existing mover with new
                    int action = 0;
                    for (Mover m : movers) {
                        if (m.isSameAs(mov, 3*settings.aperture)) {
                            if (m.score > mov.score) {
                                action = 1;
                            } else {
                                action = 2; 
                                sm = m; 
                            }
                            break;
                        }
                    }
                
                    // if action is 1 ignore the new Mover
                    if (action == 1) {continue;}
                    
                    // if action 2 remove the existing mover
                    if (action == 2) {movers.remove(sm);}            
                    
                    // add the new Mover and publish details 
                    movers.add(mov);
                    String txt = "Mover x=" + (int) t1.obj1.location.x + " y=" + (int) t1.obj1.location.y;
                    double rot = settings.crota2;
                    txt += " PA=" + df1.format(Math.toDegrees(t1.PA + rot));
                    txt += " Motion=" + df1.format(t1.motion);
                    txt += " Score=" + df1.format(mov.score) + "\n";
                    comment.append(txt);
                    settings.newMover = true;
                    
                }
            }
        }
    }

    /**
     * Select the next/previous Mover to be blinked
     *
     * @param b true = next, false = previous.
     */
    public void nextMover(boolean b) {

        // if no Movers yet, clear target mover in settings and give message
        if (movers.isEmpty()) {
            settings.targetMover = null;
            comment.append("No movers to show.");
            return;
        }

        // check for end of list. if OK change Mover else issue message 
        if (b) {
            if ((nextMover + 1) >= movers.size()) {
                comment.append("Reached end of mover list. \n");
                return;
            } else {
                nextMover += 1;
            }
        } else {
            if ((nextMover - 1) < 0) {
                comment.append("Reached start of mover list. \n");
                return;
            } else {
                nextMover -= 1;
            }
        }

        // set up next mover for display
        settings.targetMover = movers.get(nextMover);
    }

    /**
     * Sort the Movers into score sequence.
     */
    public void sortMovers() {
        Collections.sort(movers);
        comment.append("Movers sorted by score. \n");
    }   
}
