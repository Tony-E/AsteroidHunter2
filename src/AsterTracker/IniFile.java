/**********************************************************************************************************************
 *                                                 Class Inifile
 *********************************************************************************************************************/
package AsterTracker;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Class IniFile provides an ".ini" file in which to save directory names and other persistent data.
 * 
 * @author Tony Evans
 */

public class IniFile {

     private static Properties ini;        
     private static String iniFileName;   

   /**
    * Constructor creates an ini filename in the user home directory.
    */
     public IniFile() {
        ini = new Properties();                       
        iniFileName = System.getProperty("user.home");
        iniFileName+="\\astertect.ini";            
    }
     
   /**
    * Get the value of a property from the ini file.
    * 
    * @param prop Name of the property.
    * @return The value of the property or empty string if not available.
    */
    public String getProperty(String prop) {
        String r ="";
            try (FileInputStream in = new FileInputStream(iniFileName)) {
                ini.load(in);
                r = ini.getProperty(prop);
            }
            catch (Exception e){}
        return r;
    }
    /**
     * Store the value of a property in the ini file. 
     * 
     * @param property Name of the property.
     * @param value Value of the property.
     */
    public void putProperty(String property, String value) {
        try (FileInputStream in = new FileInputStream(iniFileName)) {
            ini.load(in);
        }
        catch (Exception e) {
        }
        ini.setProperty(property, value);
        try (FileOutputStream out = new FileOutputStream(iniFileName)) {
            ini.store(out, "Asteroid Ini");
        }
        catch (Exception e) {
        }
    }
}
