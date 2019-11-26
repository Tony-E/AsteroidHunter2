package AsterUtil;

import java.io.Serializable;
import java.text.*;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Class DateTime contains a date and time and provides transforms between Julian Dates and Gregorian Dates. 
 *  
 * Note that the master date/time is held in Julian format, other forms are generated when needed.
 * 
 * A method is included to set the date from MPC packed format and to generate dates formated as 
 * in the MPC Further Observation? commentary. 
 * 
 * @author Tony Evans
 **/

public class DateTime implements Serializable {
    
   /** Master data is Julian date */ 
    public double julian = 2451545.0;              
    
   /** Gregorian date and time calculated when needed */
    private int jYear = 2000, jMonth = 1, jDay = 1, jHour = 12, jMinute = 0, jSecond = 0;
    
   /** Format names */
    public static final int HOUR = 1, HHMM = 2;
    
   /** Set true is Gregorian date has been calculated */ 
    private boolean isSet;                           // true if gregorian date has been calculated

   /** Julian date of J2000 */
    private final static double J2000 = 2451545.0;  
    
   /** Working values */
    private int jJyear = 2000, jJmonth; 
   
   /** Changeover date in Gregorian calendar */
    private static final int JGREG = 15 + 31 * (10 + 12 * 1582);
    
   /** Character list used in decoding the MPC compressed format dates */
    private static final String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcde"
            + "fghijklmnopqrstuvwxyz";
    
   /** Month names used to construct dates as formated in MPC ephemeris lists */ 
    private final String[] months ={"Jan.","Feb.","Mar.","Apr.","May","June","July","Aug.","Sept.","Oct.","Nov.","Dec."};
    
   /** Formats for output date styles */
    private static final DecimalFormat 
        jf = new DecimalFormat("#############0.0000"),
        f2 = new DecimalFormat("00"),
        f2a = new DecimalFormat("#0"),
        f4 = new DecimalFormat("#######0000"),
        f2d3 = new DecimalFormat("00.000"),
        f2d2 = new DecimalFormat("00.00");

   /**
    * Constructor creates a date with default J2000 epoch. Gregorian data is not set.
    */
    public DateTime() {
        isSet = false;
    }
    
   /**
    * Set the date according to Gregorian input. Default time is 0:0:0.
    * @param d Day
    * @param m Month
    * @param y Year
    */
    public void setDate(int d, int m, int y) {
        jDay = d;
        jMonth = m;
        jYear = y;      //load greg date as is
        jJyear = jYear;
        if (jJyear < 0) {
            jJyear++;
        }       // account for year zero is 1BC
        if (m > 2) {
            jJmonth = m + 1;
        } else {
            jJyear--;
            jJmonth = m + 13;
        }
        julian = (java.lang.Math.floor(365.25 * jJyear)
            + java.lang.Math.floor(30.6001 * jJmonth)
            + d + 1720995.0);
        if (d + 31 * (m + 12 * y) >= JGREG) {  // change over to Gregorian calendar
            int ja = (int) (0.01 * jJyear);
            julian += 2 - ja + (int) (0.25 * ja);
        }
        julian-=0.5; // set to zero hours UT on the specified day.
        isSet=false;
    }
    
   /**
    * Set the time of day from hh mm ss. 
    * @param h Hours.
    * @param m Minutes.
    * @param s Seconds.
    */
    public void setTime( int h, int m, int s) {
        julian = 0.5+((int) (julian-0.5));                             // set to time zero of current day
        julian+= (((double) h)/24) + (((double) m)/1440) + (((double) s)/86400);          // add the time
        isSet=false;
         
     }

    /**
     * Set the time from fraction of a day.
     * @param d Decimal fraction of a day.
     */
    public void setTime(double d) {
        julian = 0.5+((int) (julian-0.5)); 
        julian+=d;
        isSet=false;
     }
    
    /**
     * Set date and time from a java Calendar object.
     * @param cal Calendar object.
     */
     public void setDateTime(Calendar cal)           {
        int year = cal.get(Calendar.YEAR);
        int month = 1+cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
                
        setDate(day, month,year);
        int hour =  cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);
        setTime(hour, min, sec);
        isSet=false;
    }
     
   /**
    * Set date according to Julian day number. This can contain time information.
    * @param d Julian Day.
    **/
    public void setDate(double d) {
        julian = d;
        isSet=false;
    }
    
   /**
    * Set date as J2000 or today or from character string of characters.
    * @param s Name of epoch "J2000" or "today" or date as a string of characters of form yyyy mm dd hh:mm:ss.
    **/
    public void setDate(String s) {
        if (s.equals("J2000")) {
            setDate(J2000);
            isSet=false;
        }
        if (s.equals("today")) {
            setNow();                 
            isSet=false;
        }
        if (s.length() == 21) {
            int yr = Util.s2i(s.substring(1,5), 1);
            int mo = Util.s2i(s.substring(6,8), 1);
            int dy = Util.s2i(s.substring(9,11), 1);
            int hr = Util.s2i(s.substring(12,14), 1);
            int mn = Util.s2i(s.substring(15,17), 1);
            int se = Util.s2i(s.substring(18,20), 1);
            setDate(dy,mo,yr);
            setTime(hr, mn, se);
            isSet=false;  
        }
    }
   /**
    * Sets the date according to a date in MPC text format.
    * @param s Date in yy MMM dd.d format
    */
    public void setMPCTextDate(String s) {
        int MPCy = Util.s2i(s.substring(0,4),1);
        int MPCm = 1;
        for (int i=0; i<12; i++) {if (s.contains(months[i])) {MPCm = i+1;break;}}
        int MPCd = Util.s2i(s.substring(10),1);
        setDate(MPCd, MPCm, MPCy);
    }
    
   /**
    * Get the date in Gregorian string format
    * @return Retyrns date in Gregorian string format yyyt-mm-dd.d 
    */
    public String getGdate() {
        if (!isSet) {setGregorian();}
        return f4.format(jYear) + "-" + f2.format(jMonth) + "-" + f2.format(jDay);
    }
    
   /**
    * Get the year of the date.
    * @return Year.
    */
    public String getYear() {
        if (!isSet) {setGregorian();}
        return f4.format(jYear);
    }

    /**
     * Get the month of the date
     * @return Month.
     */
    public String getMonth()  {
        if (!isSet) {setGregorian();}
        return f2.format(jMonth);
    }

   /**
    * Get the day of the date.
    * @return Day.
    */
    public String getDay() {
        if (!isSet) {setGregorian();}  
        float jDDay = (jDay + jHour/24.0f + jMinute/1440.0f + jSecond/86400.0f);
        return f2d2.format(jDDay);
    }
    
   /**
    * Get the time in one of several possible formats
    * @param format HOUR returns the hour in decimal format, HHMM returns in hours and minutes.
    * @return Date in selected format.
    */
    public String getTime(int format) {
        if (!isSet) {setGregorian();}
        if (format == HOUR) {
            return f2.format((int) (jHour/24 + jMinute/140 + jSecond/86400));
        }
        if (format == HHMM) {
            return f2.format(jHour)+":"+f2.format(jMinute);
        }
        return "fmt?";
    }
    
   /**
    * Gets the date in a format used by MPC queries. 
    * @return Date in form yyyy MMM dd 
    **/
    public String getMPCDate() {
        if (!isSet) {setGregorian();}
        String dy = f2a.format(Math.floor(jDay));
        return f4.format(jYear) + " " + months[jMonth-1] + " " + dy;
    }

   /**
    * Get date in alternative format used by the MPC.
    * @return Date in format yyyy MMM #d (i.e. single character day if less than 10).
    */
    public String getMPCDate1() {
        if (!isSet) {setGregorian();}
        String dy = f2a.format(Math.floor(jDay));
        if (1==dy.length()) {dy=" "+dy;}
        return f4.format(jYear) + " " + months[jMonth-1] + " " + dy;
    }
    
   /**
    * Get date as a julian string.
    * @return JD as a string. 
    */
    public String getJdate() {
        return jf.format(julian);
    }
    
   /**
    * Add a given number of days to a date.
    * @param d
    */
    public void add(double d) {
        julian += d;
        isSet=false;
    }
    
   /**
    * Returns true if the date is J2000.
    * @return True if date is J2000 else false. 
    *
    */
    public boolean isJ2000() {
        return julian == J2000;
    }
    
   /**
    * Set the date from the MPC compressed date format.
    * @param mpc A date in the MPC compressed format.
    */
    public void setMPCDate(String mpc) {
        // the century is encoded into the first char
        int yy=0;
        if ("I".equals(mpc.substring(0,1))) {yy=1800;}
        if ("J".equals(mpc.substring(0,1))) {yy=1900;}
        if ("K".equals(mpc.substring(0,1))) {yy=2000;}
        yy+=Integer.parseInt(mpc.substring(1,3));           // year is numeric in 2nd and 3rd chars
        int mm = charset.indexOf(mpc.substring(3,4));       // month is 4th char encoded
        int dd = charset.indexOf(mpc.substring(4,5));       // day is 5th char encoded
        setDate(dd,mm,yy);
        isSet=false;
    }
    
   /**
    * Set the Gregorian date and time from the julian date. Taken from Montenbruck & Pfledger p16.
    */
    private void setGregorian() {
        /* establish date */
        long  ja, jb, jc, jd, je, f;
        ja = (long) (julian+0.5);
        if (ja<2299161) {
            jb=0; jc=ja+1524;
        } else {
            jb=(long) ((ja-1867216.25)/36524.25);
            jc=ja+jb-(jb/4)+1525;                       
        }
        
        jd=(long) ((jc-122.1)/365.25);
        je=365*jd + jd/4; 
        f= (long) ((jc-je)/30.6001);
        jDay=(int) ((int) jc-je-(int) (30.6001*f));
        jMonth=(int) (f-1-12*(f/14));
        jYear=(int) (jd-4715 - ((7+jMonth)/10));
         
        /* establish time */
        double dd = ((julian%1)*24 +12)%24;                
        jHour = (int) dd;
        dd=(dd-jHour)*60;
        jMinute = (int) dd;
        dd=(dd-jMinute)*60;
        jSecond = (int) dd;
        isSet=true;
    }
    
   /**
    * Set the date and time to "now" U.T.C. (relies on the correct time zone being set in the OS).
    */
    public void setNow() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int year = now.get(Calendar.YEAR);
        int month = 1+now.get(Calendar.MONTH);
        int day = now.get(Calendar.DAY_OF_MONTH);      
        setDate(day, month,year);
        int hour =  now.get(Calendar.HOUR_OF_DAY);
        int min = now.get(Calendar.MINUTE);
        int sec = now.get(Calendar.SECOND);
        setTime(hour, min, sec);
        isSet=false;
    }
}
