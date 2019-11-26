package AsterUtil; 

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

/**
 * 
 * Class SphCoordinate represents a 2D coordinate on the surface of a sphere (Spherical Coordinate). It may be Longitude
 * and Latitude, Ecliptic Longitude and Latitude or Equatorial RA and Dec.
 * 
 * Methods are provided to transform from ecliptic to equatorial coordinates and to calculate rise/set hour angles.
 * 
 * The coordinates are stored in radians but methods are provided to obtain degrees or hours for specific purposes
 * and to input from typical strings showing RA and Dec.
 * 
 * @author Tony Evans
 */
public class SphCoordinate implements Serializable  {
    
   /** The coordinate. */
    public double[] coord = {0.0,0.0};                          
   /** Radians to hours. */
    private static final double toHours = 12/Math.PI;             
   /** Obliquity of the ecliptic. */
    private static final double eps = 23.4373*(Math.PI/180);   
   /** Galactic pole. */
    private static final double 
        gpRA = 3.366,
        gpDec = 0.4734;    
   /** 2xPI */
    private static final double pi2 = 2 * Math.PI;
   /** Formatting */
    private static final DecimalFormat 
        dd = new DecimalFormat("00"),
        ddpd = new DecimalFormat("00.0"),
        ddpdd = new DecimalFormat("00.00");
    
   /** 
    * Constructor sets default x,y, coordinates to zero.
    */
    public SphCoordinate() {                        
        coord[0]=0.0;
        coord[1]=0.0;
    }

    /**
    * Constructor sets specified coordinates.
    * @param x X coordinate.
    * @param y Y coordinate.
    */
    public SphCoordinate(double x, double y) {                    // construct coordinate from ra, dec in adians
        coord[0]=x;
        coord[1]=y;
    }

    /**
     * Constructor sets specified coordinates from a string from the MPC.
     * @param s Coordinates from string hh mm ss.s dd mm ss.s.
     */
    public SphCoordinate(String s) {                           
         StringTokenizer st = new StringTokenizer(s," ");
         coord[0] = Util.s2d(st.nextToken(),0);
         coord[0]+= Util.s2d(st.nextToken(),0)/60;
         coord[0]+= Util.s2d(st.nextToken(),0)/3600;
         coord[0]=Math.toRadians(15*coord[0]);
         String deg = st.nextToken();
         coord[1] = Math.abs(Util.s2d(deg, 0));
         coord[1]+= Util.s2d(st.nextToken(),0)/60;
         coord[1]+= Util.s2d(st.nextToken(),0)/3600;
         coord[1]=Math.toRadians(coord[1]);
         if (deg.startsWith("-")) {coord[1]=-coord[1];}
    }
    
   /**
    * Calculate the angle between this coordinate and an object at coordinate c. Basic spherical geometry.
    * @param c Coordinates of another point.
    * @return Position angle in radians of point c from this point.
    */
    public double getAngle(SphCoordinate c) {
        return Math.acos(Math.sin(c.coord[1])*Math.sin(coord[1])
                + Math.cos(c.coord[1])*Math.cos(coord[1])*Math.cos(c.coord[0]-coord[0]));
    }
    
   /**
    * Gets the offset in hours from the Longitude or RA.
    * @return Offset expressed in hours. 
    */
    public double getHours() {
        return coord[0]*toHours;
    }
    
    /**
     * Set the coordinates explicitly in radians.
     * @param x X coordinate.
     * @param y Y coordinate.
     */
    public void setCoords(double x, double y) {
        coord[0] = x;
        coord[1] = y;
    }

    /**
     * Set the coordinates from a series of text tokens.
     * @param s Coordinates of the form hh mm ss dd mm ss.
     */
    public void setCoords(String s){
        StringTokenizer st = new StringTokenizer(s," ");
        float ra = Util.s2f(st.nextToken(),0);
        ra+=(Util.s2f(st.nextToken(),0))/60;
        ra+=(Util.s2f(st.nextToken(),0))/3600;
        float dec = Util.s2f(st.nextToken(),0);
        dec+=(Util.s2f(st.nextToken(),0))/60;
        dec+=(Util.s2f(st.nextToken(),0))/3600;
        setCoords(15*Math.toRadians(ra), Math.toRadians(dec));
    }
    
    /**
     * Transform this coordinate from ecliptic to equatorial. From Fundamental Astronomy, 
     * Karttunen, etal 5th ed. 2003, Springer.
     * @return  A SpericalCoordinate containing this Ecliptic coordinate as an Equatorial coordinate.
     */
    public SphCoordinate getEquatorial() {
        SphCoordinate eq = new SphCoordinate();
        eq.coord[1]=Math.asin(Math.sin(coord[1])*Math.cos(eps) + Math.cos(coord[1])*Math.sin(eps)*Math.sin(coord[0]));    
        double sinRA = (Math.cos(coord[1])*Math.cos(eps)*Math.sin(coord[0])-Math.sin(coord[1])*Math.sin(eps))/Math.cos(eq.coord[1]);
        double cosRA = (Math.cos(coord[0])*Math.cos(coord[1]))/Math.cos(eq.coord[1]);
        eq.coord[0] = Math.atan2(sinRA,cosRA);
        if (eq.coord[0]<0) {eq.coord[0]+=pi2;}
        return eq;
    }
    
   /**
    * If this coordinate is the geographic position of an observatory, calculate time either side of 
    * meridian an object with equatorial coordinates p remains above a horizon of altitude alt degrees.
    * @param p Coordinates (RA, Dec) of target object.
    * @param alt Limiting altitude above horizon object must have.
    * @return Number of hours object at p remains above altitude alt.
    */
    public double riseTime(SphCoordinate p, double alt) {
        double zRad = Math.toRadians(alt);       
        double HARad = Math.acos((Math.sin(zRad)-Math.sin(p.coord[1])*Math.sin(coord[1]))/(Math.cos(p.coord[1])*Math.cos(coord[1])));
        return Math.toDegrees(HARad)/15;
    }
    
   /**
    * If this coordinate is the RA and Dec of a position then return the midpoint between it and point
    * p on the celestial sphere. From https://answers.yahoo.com/question/index?qid=20081211074044AA2G9aK
    * @param p The other point.
    * @return The SphericalCoordinate of the midpoint.
    */
    public SphCoordinate getMiddle(SphCoordinate p) {
        SphCoordinate m = new SphCoordinate();
        double Bx = Math.cos(p.coord[1]) * Math.cos(p.coord[0] - coord[0]); 
        double By = Math.cos(p.coord[1]) * Math.sin(p.coord[0] - coord[0]);
        m.coord[1] = Math.atan2(Math.sin(coord[1]) + Math.sin(p.coord[1]),
                                           Math.sqrt((Math.cos(coord[1]) + Bx)*(Math.cos(coord[1]) + Bx)+ (By*By))); 
        m.coord[0] = coord[0] + Math.atan2(By, Math.cos(coord[1]) + Bx);
        return m;
    }
    
   /**
    * If this coordinate is the RA and Dec of an object, return its galactic latitude in degrees.
    * @return Galactic latitude of this coordinate.
    */
    public double galLat(){
        double d = Math.sin(gpDec)*Math.sin(coord[1])+Math.cos(gpDec)*Math.cos(coord[1])*Math.cos(coord[0]-gpRA);
        return Math.toDegrees(Math.asin(d));
    }
    
   /**
    * Returns the RA coordinate in format required for MPC MPChecker.
    * @return This coordinate RA in the form of HHMMDD. 
    */
    public String getRA() {
        double ra = Math.toDegrees(coord[0]/15);
        int h = (int) ra;
        ra=(ra-h)*60;
        int m = (int) ra;
        double s = (ra-m)*60;
        return dd.format(h)+"+"+dd.format(m)+"+"+dd.format(s);
    }

    /**
     * Returns the coordinate Decl in format required for MPC checker.
     * @return Declination in form DDMMSS.
     */
    public String getDec() {
        String r;
        double dec = Math.abs(Math.toDegrees(coord[1]));
        int d = (int) dec;
        dec =(dec-d)*60;
        int m = (int) dec;
        int s = (int) (dec-m)*60;
        if (coord[1]<0) {r="-";} else {r="%2B";}
        r+= dd.format(d)+"+"+dd.format(m)+"+"+dd.format(s); 
        return r;
    }
    
   /**
    * Add coordinate.
    * @param a Coordinate to be added.
    */
    public void plus(SphCoordinate a) {
        coord[0]+=a.coord[0];
        coord[1]+=a.coord[1];
    }

   /**
    * Subtract coordinate.
    * @param a Coordinate to be subtracted.
    */
    public void minus (SphCoordinate a) {
        coord[0]-=a.coord[0];
        coord[1]-=a.coord[1];
    }

   /**
    * Copy coordinate.
    * @param a An new SphericalCoordinate containing the same data.
    */
    public void copy (SphCoordinate a) {
        coord[0]=a.coord[0];
        coord[1]=a.coord[1];
    }

   /**
    * Multiply by a scalar.
    * @param a Scalar value to multiply by.
    */
    public void mult (double a) {
        coord[0]*=a; coord[1]*=a;
    }
}
