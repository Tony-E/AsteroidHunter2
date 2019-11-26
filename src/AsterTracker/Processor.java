/**********************************************************************************************************************
 *                                                Class Processor
 *********************************************************************************************************************/
package AsterTracker;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class Processor is responsible to drive the parallel processing of tasks. The variable pType determines
 * which class of tasks it should perform. A Cyclic Barrier is used to synchronise activities across the parallel 
 * running threads. This Class is Runnable and the execution threads are started by AsterGUI as soon as all the
 * FITS have been loaded.
 *
 * @author Tony Evans
 */
public class Processor implements Runnable {
    
   /**
    * pType determines the processes to be performed:
    *  1 - ImageGroup functions (3 processors),
    *  2 - Supergroup functions (1 processor).
    */
    private final int pType;

    /** grpNo specifies which group to work on. */
    private final int grpNo;

    /** groups is a pointer to the set of ImageGroups. */
    private final ArrayList<ImageGroup> groups;

    /** superGroup is a pointer to the SuperGroup object. */
    private final SuperGroup superGroup;

    /** settings is a pointer to the Settings. Not that settings used by a Processor should be Volatile */
    Settings settings;
    
    /** Synchronisation lock.  */
    private final CyclicBarrier synchLock;

   /**
    * phase determines what processing is to be done
    * 1 - image preparation activities,
    * 2 - synthetic tracking and object searching.
    */
    private static int phase;

    /**
     * Constructor sets up pointers.
     * @param s Pointer to Settings.
     * @param pt Process type - 1 work with ImageGroup, 2 work with Supergroup.
     * @param grp Which group to be worked on 0-2.
     * @param grps The set of ImageGroups.
     * @param sup  The SuperGroup.
     * @param lock CyclicBarrier for synchronisation.
     */
    public Processor(Settings s, int pt, int grp, ArrayList grps, SuperGroup sup, CyclicBarrier lock, int ph) {
        settings = s;
        pType = pt;
        groups = grps;
        superGroup = sup;
        phase = ph;
        grpNo = grp;
        synchLock = lock;
    }

    /**
     * Run is the running thread. The threads will run until Settings.Finished becomes true. If Settings.paused
     * is true the threads will sleep until paused becomes false. The threads wait on the cyclic barrier at each
     * point where they need to be synchronised.
     */
    @Override
    public void run() {
    while(true) {
     
        switch (pType) {
            
           /* pType 1 is a Processor that handles one of the three ImageGroups. During phase 1 this involves preparing
            * the FITS Images, performing the initial stacking and removing stars from FITS. During phase 2 it is
            * repeatedly re-stacking with different motions and PA and looking for objects. 
            */ 
            case 1:
                if (phase == 1) {
                    // Phase 1 is image preparation
                      
                    // do histograms, filters and stretches for all the FITS in the groups
                    for (FitsImage f : groups.get(grpNo).fitsImages){
                        f.doHist();
                        f.doLines();
                        f.doStretch();
                        f.doBlur();
                    }   
                        
                    // Calculate static (no motion) stacking offsets for all FITS
                    for (FitsImage f : groups.get(grpNo).fitsImages) {
                        f.setStaticOffset();
                    }   
                        
                    // Median stack the group FITS
                    groups.get(grpNo).stack();
                       
                    // Wait here until all groups have finished stacking
                    waitFor(synchLock);
                       
                    // Wait here until the stars-only Superstack is ready then subtract it from the FITS
                    waitFor(synchLock);
                    for (FitsImage f : groups.get(grpNo).fitsImages) {
                        f.subtract(superGroup.sStack);
                    }  
                        
                    // Wait here untill all groups have finished subtracting
                    waitFor(synchLock);
                     
                    // Wait here until the pseudo-flat is ready, then divide it into all the FITS
                    waitFor(synchLock);
                    for (FitsImage f : groups.get(grpNo).fitsImages) {
                        f.divide(superGroup.sStack);
                    }   
                        
                    // Wait here until all groups reach this point then go to phase 2
                    waitFor(synchLock);
                    phase = 2;
                   
                } else {       
                    
                    // phase 2 is synthetic tracking and object search.
                    
                    // Re-stack with synthetic tracking parameters then look for objects in the stack
                    groups.get(grpNo).reStack(false);
                    groups.get(grpNo).findObjects();
                        
                    // Wait here until all groups have found their objects
                    if (!waitFor(synchLock)) return;
                  
                    // Wait here for the Supergoup to convert objects into tracklets, then loop
                    if (!waitFor(synchLock)) return;
                }
                break;
                
            /* pType 2 is a Processor that manages what the SuperGroup is doing. In phase 1 this is to normalise 
             * images and construct the "Superstack" used to remove stars from the FITS. Also to prepare an (optional) 
             * synthetic flat-field image. In phase 2 the action is to combine ImageObjects into Tracklets and
             * Tracklets into Movers.
             */     
            case 2:
                // phase 1 is image preparation
                
                if (phase == 1) {
                    
                    // wait here unil the group stacks are ready, then normalise and create the superstack
                    waitFor(synchLock);
                    superGroup.normalise();
                    superGroup.superStack();
                    
                    // now we can start to blink
                    settings.blink = true;
                    
                    // wait here when Superstack is ready
                    waitFor(synchLock);
                    
                    // wait here until groups have subtracted superstack, then produce the pseudo flat
                    waitFor(synchLock);
                    superGroup.flatten();
                    
                    /* wait here when flat is ready */
                    waitFor(synchLock);
                    
                    /* wait here for groups to apply flat then go to phase 2 */
                    waitFor(synchLock);
                    phase = 2;
                    
                    
                // phase 2 is converting Objects into Movers
                } else {
                    
                    // wait here for groups to produce object lists 
                    if (!waitFor(synchLock)) return;
                    
                    // create tracklets from objects. */ 
                    superGroup.findTracks();
                    
                    // advance synthetic tracking step. */ 
                    boolean end = settings.nextStep(); 
                    
                    // wait here for groups ready to start next iteration
                    waitFor(synchLock);
                    
                    /* Note that Tracklet formation is not overlapped with group object searching because the * 
                     * list of objects would get overwritten. But forming Movers from Tracklets is overlapped *
                     * with the Groups re-stacking with the next synthetic tracking step.                     */
                    
                    // create movers from tracklets
                    superGroup.findMovers();
                    
                    // If the end of synthetic tracking, sort the movers and quit (kills thread). */
                    if (end) {
                        superGroup.sortMovers();
                        return;                        
                    }
                    
                    // if paused then wait here for user to un-pause */
                    while (settings.paused) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } 
                break;
            }   
        }
    }
    
    /* Subroutine to execute a wait - ( I should do something more with the exceptions).
     * I probably should do something to ensure the other threads die when the supergroup thread quits.*/
    private boolean waitFor(CyclicBarrier b) {
        
        // if finished synthetic tracking quit (kills the thread cleanly) */
        if (settings.finished) return false;
        // wait until someone issues a notfy() that causes the exception, then everything continues.
        try {
            b.await();
        } catch (InterruptedException | BrokenBarrierException ex) {
            return true;
        }
        return true;
    }
}
