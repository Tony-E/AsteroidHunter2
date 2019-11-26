package AsterUtil;
/**
 * This class contains general utility functions and constants that can be used across the projects.
 * @author Tony Evans
 */
public class Util {   
    
   /**
    * General purpose string to float converter.
    * @param s String value of a number to be converted.
    * @param dflt Default value if string does not contain a valid number.
    * @return The number.
    */
    public static float s2f(String s, float dflt) {
        if (s == null) { 
            return dflt;
        } else {
            try {
                return Float.valueOf(s);
            } catch (NumberFormatException e) {
                return dflt;
     }}}
     
   /**
    * General purpose string to double converter.
    * @param s String value of a number to be converted.
    * @param dflt Default value if string does not contain a valid number.     
    * @return The number.
    */
    public static double s2d(String s, double dflt) {
        if (s == null) { 
            return dflt;
        } else {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException e) {
                return dflt;
     }}}
     
   /**
    * General purpose string to integer converter.
    * @param s String value of a number to be converted.
    * @param dflt Default value if string does not contain a valid number.
    * @return The number.
    */
    public static int s2i(String s, int dflt) {
        if (s == null) {
            return dflt;
        } else {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return dflt;
            }
        }
     } 
}
