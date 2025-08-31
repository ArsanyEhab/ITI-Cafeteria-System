package CrossCutting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for converting UTC timestamps to Cairo timezone (UTC+3)
 */
public class TimeZoneConverter {
    
    private static final String CAIRO_TIMEZONE = "Africa/Cairo";
    private static final String UTC_TIMEZONE = "UTC";
    
    /**
     * Convert a UTC Date to Cairo timezone and format it for display
     * @param utcDate The UTC date from database
     * @return Formatted string in Cairo timezone
     */
    public static String convertUTCToCairo(Date utcDate) {
        if (utcDate == null) {
            return "N/A";
        }
        
        try {
            // Create formatter for Cairo timezone
            SimpleDateFormat cairoFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            cairoFormatter.setTimeZone(TimeZone.getTimeZone(CAIRO_TIMEZONE));
            
            // Format the date in Cairo timezone
            return cairoFormatter.format(utcDate);
        } catch (Exception e) {
            // Fallback to original date if conversion fails
            return utcDate.toString();
        }
    }
    
    /**
     * Convert a UTC Date to Cairo timezone and return the Date object
     * @param utcDate The UTC date from database
     * @return Date object in Cairo timezone
     */
    public static Date convertUTCToCairoDate(Date utcDate) {
        if (utcDate == null) {
            return null;
        }
        
        try {
            // Get Cairo timezone
            TimeZone cairoTZ = TimeZone.getTimeZone(CAIRO_TIMEZONE);
            
            // Calculate the offset in milliseconds
            int offset = cairoTZ.getOffset(utcDate.getTime());
            
            // Create new date with Cairo timezone offset
            return new Date(utcDate.getTime() + offset);
        } catch (Exception e) {
            // Return original date if conversion fails
            return utcDate;
        }
    }
    
    /**
     * Get current time in Cairo timezone
     * @return Current date/time in Cairo timezone
     */
    public static Date getCurrentCairoTime() {
        try {
            TimeZone cairoTZ = TimeZone.getTimeZone(CAIRO_TIMEZONE);
            TimeZone utcTZ = TimeZone.getTimeZone(UTC_TIMEZONE);
            
            // Get current UTC time
            Date now = new Date();
            
            // Calculate offset
            int offset = cairoTZ.getOffset(now.getTime()) - utcTZ.getOffset(now.getTime());
            
            // Return Cairo time
            return new Date(now.getTime() + offset);
        } catch (Exception e) {
            return new Date();
        }
    }
    
    /**
     * Format a date in Cairo timezone with custom pattern
     * @param utcDate The UTC date from database
     * @param pattern The date pattern (e.g., "dd/MM/yyyy", "HH:mm:ss")
     * @return Formatted string in Cairo timezone
     */
    public static String formatInCairoTimeZone(Date utcDate, String pattern) {
        if (utcDate == null) {
            return "N/A";
        }
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            formatter.setTimeZone(TimeZone.getTimeZone(CAIRO_TIMEZONE));
            return formatter.format(utcDate);
        } catch (Exception e) {
            return utcDate.toString();
        }
    }
    
    /**
     * Get a human-readable timezone info string
     * @return String showing current Cairo timezone info
     */
    public static String getCairoTimeZoneInfo() {
        try {
            TimeZone cairoTZ = TimeZone.getTimeZone(CAIRO_TIMEZONE);
            int offset = cairoTZ.getOffset(System.currentTimeMillis());
            int hours = offset / (1000 * 60 * 60);
            int minutes = (offset % (1000 * 60 * 60)) / (1000 * 60);
            
            String sign = hours >= 0 ? "+" : "";
            return String.format("Cairo Time (UTC%s%d:%02d)", sign, hours, minutes);
        } catch (Exception e) {
            return "Cairo Time (UTC+3)";
        }
    }
}
