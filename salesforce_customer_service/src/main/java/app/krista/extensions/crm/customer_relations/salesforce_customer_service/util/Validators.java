package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

public class Validators {
    public static String getValidatedString(Object value) {
        return value != null ? value.toString() : "";
    }
    public static Double getValidatedDouble(Object value) {
        return value != null ? (Double) value : 0;
    }
    private Validators() {
    }
}

