/***********************************************************************************************************************
 *                                            Class ImageDisplay
 **********************************************************************************************************************/
package AsterTracker;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Class ImageDisplay is the surface in which the image is displayed.
 *
 * @author Tony 
 */
public class ImageDisplay extends javax.swing.JPanel {
    private final AsterGUI gui;
    
   /**
    * Creates new form ImageDisplay and store reference back to the parent GUI 
     * @param gui The parent AsterGUI.
    */
    public ImageDisplay(AsterGUI gui) {
        initComponents();
        this.gui = gui;
    }
   

   /**
    * paintComponent() is overridden to pass the drawing buffer to the component for display while 
    * a new drawing is being prepared. The image in gBuffer is taken to be what is required to be displayed
    * regardless of size. 
    * 
    * @param g the graphics context for this object
    */
    @Override
    public void paintComponent(Graphics g){
            
        // if there is no display manager or no buffer just paint the default component
        if (gui.displayManager == null) {
            super.paintComponent(g);
                
        // else show the contents of imagebuffer belonging to the Displa Manager
            }  else {             
                if (gui.displayManager.gBuffer != null) {
                    BufferedImage image = gui.displayManager.gBuffer;
                    g.drawImage(image ,0,0,  null);
                } else {
                    super.paintComponent(g);
            }
        }
    }

    /**
     * Create the drawing surface. Do NOT modify this code as it is produced by Netbeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(0, 0, 0));
        setForeground(new java.awt.Color(255, 255, 255));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 683, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 503, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
