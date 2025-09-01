package CrossCutting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for converting UTC timestamps to local timezone
 */
public class TimeZoneConverter {
    
    private static final String LOCAL_TIMEZONE = TimeZone.getDefault().getID();
    private static final String UTC_TIMEZONE = "UTC";
    
    /**
     * Convert a UTC Date to local timezone and format it for display
     * @param utcDate The UTC date from database
     * @return Formatted string in local timezone
     */
    public static String convertUTCToCairo(Date utcDate) {
        if (utcDate == null) {
            return "N/A";
        }
        
        try {
            // Create formatter for local timezone
            SimpleDateFormat localFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            localFormatter.setTimeZone(TimeZone.getDefault());
            
            // Format the date in local timezone
            return localFormatter.format(utcDate);
        } catch (Exception e) {
            // Fallback to original date if conversion fails
            return utcDate.toString();
        }
    }
    
    /**
     * Convert a UTC Date to local timezone and return the Date object
     * @param utcDate The UTC date from database
     * @return Date object in local timezone
     */
    public static Date convertUTCToCairoDate(Date utcDate) {
        if (utcDate == null) {
            return null;
        }
        
        try {
            // Get local timezone
            TimeZone localTZ = TimeZone.getDefault();
            
            // Calculate the offset in milliseconds
            int offset = localTZ.getOffset(utcDate.getTime());
            
            // Create new date with local timezone offset
            return new Date(utcDate.getTime() + offset);
        } catch (Exception e) {
            // Return original date if conversion fails
            return utcDate;
        }
    }
    
    /**
     * Get current time in local timezone
     * @return Current date/time in local timezone
     */
    public static Date getCurrentCairoTime() {
        try {
            TimeZone localTZ = TimeZone.getDefault();
            TimeZone utcTZ = TimeZone.getTimeZone(UTC_TIMEZONE);
            
            // Get current UTC time
            Date now = new Date();
            
            // Calculate offset
            int offset = localTZ.getOffset(now.getTime()) - utcTZ.getOffset(now.getTime());
            
            // Return local time
            return new Date(now.getTime() + offset);
        } catch (Exception e) {
            return new Date();
        }
    }
    
    /**
     * Format a date in local timezone with custom pattern
     * @param utcDate The UTC date from database
     * @param pattern The date pattern (e.g., "dd/MM/yyyy", "HH:mm:ss")
     * @return Formatted string in local timezone
     */
    public static String formatInCairoTimeZone(Date utcDate, String pattern) {
        if (utcDate == null) {
            return "N/A";
        }
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            formatter.setTimeZone(TimeZone.getDefault());
            return formatter.format(utcDate);
        } catch (Exception e) {
            return utcDate.toString();
        }
    }
    
    /**
     * Get a human-readable timezone info string
     * @return String showing current local timezone info
     */
    public static String getCairoTimeZoneInfo() {
        try {
            TimeZone localTZ = TimeZone.getDefault();
            int offset = localTZ.getOffset(System.currentTimeMillis());
            int hours = offset / (1000 * 60 * 60);
            int minutes = (offset % (1000 * 60 * 60)) / (1000 * 60);
            
            String sign = hours >= 0 ? "+" : "";
            return String.format("Local Time (UTC%s%d:%02d)", sign, hours, minutes);
        } catch (Exception e) {
            return "Local Time";
        }
    }
}
