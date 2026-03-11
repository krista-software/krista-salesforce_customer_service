/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for date formatting and parsing operations.
 * Provides helper methods to convert between timestamps and {@link String}
 * representations in the format {@code yyyy-MM-dd}.
 * <p>
 * This class is thread-safe and uses the modern java.time API.
 */
public final class DatePlus {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
        LocalDate localDate = Instant.ofEpochMilli(date)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate.format(FORMATTER);
    }

    /**
     * Parses a date string in {@code yyyy-MM-dd} format into a {@link LocalDate} object.
     *
     * @param date the date string in {@code yyyy-MM-dd} format
     * @return a {@link LocalDate} object representing the parsed date,
     *         or {@code null} if the string is not a valid date
     */
    public static LocalDate fromYYYYMMDD(String date) {
        try {
            return LocalDate.parse(date, FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parses a date string in {@code yyyy-MM-dd} format and converts it to epoch milliseconds.
     *
     * @param date the date string in {@code yyyy-MM-dd} format
     * @return the epoch milliseconds representing the parsed date at start of day in system timezone,
     *         or {@code null} if the string is not a valid date
     */
    public static Long fromYYYYMMDDToEpochMilli(String date) {
        LocalDate localDate = fromYYYYMMDD(date);
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
