package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DatePlus {

    private DatePlus() {
    }

    public static String getYYYYMMDD(long date) {
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date(date));
    }

    public static Date fromYYYYMMDD(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
