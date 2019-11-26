package AsterUtil;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Class AsterIni is a basic ".ini" file that can be used to store data that needs to persist from session to session.
 * This might include directory names last used for open and save operations. There can only be one Ini object.
 * 
 * @author Tony Evans
 */
public class AsterIni {

    private static Properties ini;     // properties object
    private static String iniFile;     // path and name of ini file

   /**
    * Constructor creates a properties object and constructs an .ini file name in the user directory.
    */
    public AsterIni() {
        ini = new Properties();                       // create properties list
        iniFile = System.getProperty("user.dir");     // get place to put ini file
        iniFile+="\\aster.ini";                       // construct ini file name
    }
    
   /**
    * Retrieves the value of the specified property.
    * @param prop  Name of the property to be retrieved from the .ini file.
    * @return Value of the property retrieved from the .ini file.
    */
    public String getProperty(String prop) {
        String r ="";
        try (FileInputStream in = new FileInputStream(iniFile)) {
            ini.load(in);
            r = ini.getProperty(prop);
        } catch (Exception e){}
        return r;
    }

   /**
    * Places a property and its value in the .ini file.
    * @param property Name of the property to be recorded. 
    * @param value Value of the property to be recorded.
    */
    public void putProperty(String property, String value) {
        try (FileInputStream in = new FileInputStream(iniFile)) {
            ini.load(in);
            ini.setProperty(property, value);
        } catch (Exception e) {}

        try (FileOutputStream out = new FileOutputStream(iniFile)) {
            ini.store(out, "AsterTracker ini file");
        } catch (Exception e) {}
    }
}
