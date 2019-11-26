/*********************************************************************************************************************
 *                                           Class DisplayManager
 *********************************************************************************************************************/
package AsterTracker;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

/**
 * <p>The DisplayManager is responsible to decide what should be presented in the display window 
 * and to construct and paint the images.</p>
 *
 * @author Tony Evans
 */
public class DisplayManager {
    
    /** Index to the image group from which an image is currently being displayed. */
    public int groupIndex;
    
    /** Graphics buffer in which to place the image to be displayed. */
    public BufferedImage gBuffer;

    /* The size of the display window available */
    private final Dimension displaySize;
    /* The size of the images coming from the FITs files */
    private final Dimension fitsSize;
    /* The scroll pane in which the image should be shown */
    private final JScrollPane scroller;
    /* The display window */
    private final JPanel display;
    /* The graphics context for the buffer */
    private final Graphics2D bg;
    /* The set of ImageGroups from which the images will be supplied */
    private final ArrayList<ImageGroup> groups;
    /* Current zoom factor (Zoom is not currently implemented */
    private int zoom;

    /**
     * Constructor stores pointers and initialises variables.
     *
     * @param scroll  The scroll pane available to paint in.
     * @param display The display window.
     * @param groups  The set of ImageGroups that will provide the images.
     */
    public DisplayManager(JScrollPane scroll, JPanel display, ArrayList<ImageGroup> groups) {
        this.display = display;
        displaySize = display.getSize();
        this.groups = groups;
        fitsSize = groups.get(0).getImageSize();
        groupIndex = 0;
        zoom = 1;
        gBuffer = new BufferedImage(fitsSize.width, fitsSize.height, BufferedImage.TYPE_INT_RGB);
        scroller = scroll;
        display.setPreferredSize(fitsSize);
        bg = (Graphics2D) gBuffer.getGraphics();
    }

    /**
     * Set zoom magnifier and recreate buffer for image.
     *
     * @param z Zoom factor required.
     */
    public void zoom(int z) {
        if (z == 0) {
            zoom = 1;
        }
        if (z > 0) {
            zoom += 1;
        }
        if (z < 0 && zoom > 1) {
            zoom -= 1;
        }
        gBuffer = new BufferedImage(displaySize.width * zoom, displaySize.height * zoom, BufferedImage.TYPE_INT_RGB);

    }

    /**
     * Construct and show the next image.
     *
     * @param m A Mover that should be displayed. If m is NULL, the default Group Stacks are shown.
     */
    public void showNext(Mover m) {

        // get the imagegroup that should supply the image and get the image from it
        ImageGroup showGroup = groups.get(groupIndex);
        BufferedImage img = showGroup.draw();

        // if a Mover is to be shown, draw it centred in the viewport otherwise just show whats in the buffer
        scroller.revalidate();
        if ((m != null)) {
            JViewport port = scroller.getViewport();
            Dimension pd = port.getExtentSize();
            int vx = (int) pd.width / 4;
            int vy = (int) pd.height / 4;
            PixelF loc = m.objects.get(1).location;
            int mx1 = (int) loc.x - vx;
            int my1 = (int) loc.y - vy;
            int mx2 = (int) loc.x + vx;
            int my2 = (int) loc.y + vy;
            bg.clearRect(0, 0, fitsSize.width, fitsSize.height);
            bg.drawImage(img, 0, 0, pd.width, pd.height, mx1, my1, mx2, my2, null);
            port.setViewPosition(new Point(0, 0));
        } else {
            bg.drawImage(img, 0, 0, null);
        }
        display.repaint();

        // cycle to next image group
        groupIndex++;
        if (groupIndex > groups.size() - 1) {
            groupIndex = 0;
        }
    }

}
