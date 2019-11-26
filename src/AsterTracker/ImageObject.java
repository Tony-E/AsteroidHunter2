package AsterTracker;

/**
 * Class ImageObject represents a possible detection of an object in an single image.
 * 
 * @author Tony Evans
 **/
public class ImageObject {

    /** Location (coordinates) in image. */
    public PixelF location;                 

    /** Pixel count of smallest aperture used to identify object. */
    public float obSize = 0;               

    /** SNR (or similar indication of relative intensity) */
    public float objectSNR = 0;             

    /** Flux (count - background) inside object. */
    public float flux = 0;                  

    /** Count of pixels over threshold in the object. */
    public int   tCount = 0;    
        
    /**
     * Constructor creates empty object.
     */
    public ImageObject() {
       location = new PixelF(0,0);
    }
}