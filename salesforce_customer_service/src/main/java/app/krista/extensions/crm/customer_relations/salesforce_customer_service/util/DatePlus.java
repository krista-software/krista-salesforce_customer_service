package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for date formatting and parsing operations.
 * Provides helper methods to convert between {@link Date} and {@link String}
 * representations in the format {@code yyyy-MM-dd}.
 */
public final class DatePlus {

    private DatePlus() {
    }

    /**
     * Converts a long timestamp (milliseconds since epoch) into a date string
     * in {@code yyyy-MM-dd} format.
     *
     * @param date the timestamp in milliseconds since epoch
     * @return a formatted date string in {@code yyyy-MM-dd} format
     */
    public static String getYYYYMMDD(long date) {
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date(date));
    }

    /**
     * Parses a date string in {@code yyyy-MM-dd} format into a {@link Date} object.
     *
     * @param date the date string in {@code yyyy-MM-dd} format
     * @return a {@link Date} object representing the parsed date,
     *         or {@code null} if the string is not a valid date
     */
    public static Date fromYYYYMMDD(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
