package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.*;

public final class StringHelper {
    public static boolean isStringAvailable(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * This method will append Order by clause to the given query string
     * @param size size of the query results per page
     * @param page page number of the query results
     * @param orderByField order by query clause field input
     * @param orderByOperand order by DESC or ASC
     * @param builder input query to be appended with order by clause
     */
    public static void appendOrderByClause(long size, long page, String orderByField, String orderByOperand, StringBuilder builder) {
        builder.append(Constants.ORDER_BY)
                .append(orderByField)
                .append(SPACE)
                .append(orderByOperand)
                .append(Constants.LIMIT)
                .append(size)
                .append(Constants.OFFSET)
                .append(Math.max((page - 1) * size, 0));
    }

    public static void setQueryEndIndex(StringBuilder builder) {
        if (builder.substring(builder.length() - WHERE.length()).equals(WHERE)) {
            builder.setLength(builder.lastIndexOf(WHERE));
        }
        if (builder.substring(builder.length() - SPACE_AND.length()).equals(SPACE_AND)) {
            builder.setLength(builder.lastIndexOf(SPACE_AND));
        }
    }

    private StringHelper() {
    }
}
