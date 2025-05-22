package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import app.krista.extension.executor.SearchCondition;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.AND;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.WHERE;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper.isStringAvailable;


/**
 * Utility class for building dynamic SOQL queries for Salesforce Case objects based on search conditions.
 * <p>
 * This class is stateless and cannot be instantiated.
 */
public final class CaseQueryBuilder {

    private static final String QUERY_FIELDS = "Contact.Name, ContactId, ContactEmail, ContactPhone, Account.Name, AccountId, Subject, Priority, Description, IsClosed, ClosedDate, OwnerId, CreatedById, CaseNumber, Status, Type, CreatedDate, LastModifiedDate, Id, Origin";

    private CaseQueryBuilder() {
    }

    /**
     * Constructs a SOQL query for the Case object based on provided search conditions and pagination/sorting details.
     *
     * @param conditions       List of {@link SearchCondition} containing filters to be applied.
     * @param size             Number of records per page.
     * @param page             Page number to retrieve.
     * @param orderByField     Field to sort the results by.
     * @param orderByOperand   Sorting order (ASC or DESC).
     * @return                 A valid SOQL query string.
     * @throws IllegalArgumentException if the conditions list is null or empty.
     */
    public static String getQuery(List<SearchCondition> conditions, long size, long page, String orderByField, String orderByOperand) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException(Constants.CONDITIONS_NOT_FOUND);
        }
        String id = getConditionValue(conditions, Constants.ID, Constants.STRING, Constants.EQUALS);
        String contactId = getConditionValue(conditions, Constants.CONTACT_ID, Constants.STRING, Constants.EQUALS);
        String accountId = getConditionValue(conditions, Constants.ACCOUNT_ID, Constants.STRING, Constants.EQUALS);
        String contactName = getConditionValue(conditions, Constants.CONTACT_NAME, Constants.STRING, Constants.EQUALS);
        String subject = getConditionValue(conditions, Constants.SUBJECT, Constants.STRING, Constants.EQUALS);
        String ownerId = getConditionValue(conditions, Constants.OWNER_ID, Constants.STRING, Constants.EQUALS);
        String accountName = getConditionValue(conditions, Constants.ACCOUNT_NAME, Constants.STRING, Constants.EQUALS);
        String caseNumber = getConditionValue(conditions, Constants.CASE_NUMBER, Constants.DOUBLE, Constants.EQUALS);
        String contactEmail = getConditionValue(conditions, Constants.CONTACT_EMAIL, Constants.STRING, Constants.EQUALS);
        String contactPhone = getConditionValue(conditions, Constants.CONTACT_PHONE, Constants.STRING, Constants.EQUALS);
        String status = getConditionValue(conditions, Constants.STATUS, Constants.STRING, Constants.EQUALS);
        String origin = getConditionValue(conditions, Constants.ORIGIN, Constants.STRING, Constants.EQUALS);
        String createdDate = getConditionValue(conditions, Constants.CREATED_DATE, Constants.LONG, Constants.EQUALS);
        String createdDateGreaterThan = getConditionValue(conditions, Constants.CREATED_DATE, Constants.LONG, Constants.GT);
        String createdDateLessThan = getConditionValue(conditions, Constants.CREATED_DATE, Constants.LONG, Constants.LT);
        String modifiedDate = getConditionValue(conditions, Constants.MODIFIED_DATE, Constants.LONG, Constants.EQUALS);
        String modifiedDateGreaterThan = getConditionValue(conditions, Constants.MODIFIED_DATE, Constants.LONG, Constants.GT);
        String modifiedDateLessThan = getConditionValue(conditions, Constants.MODIFIED_DATE, Constants.LONG, Constants.LT);
        return createQuery(id, contactId, accountId, contactName, subject, ownerId, accountName, caseNumber, contactEmail, contactPhone, status, origin, createdDate, createdDateLessThan, createdDateGreaterThan, modifiedDate, modifiedDateLessThan, modifiedDateGreaterThan, size, page, orderByField, orderByOperand);
    }

    /**
     * Extracts the operand value from the search condition list based on the field, data type, and operator.
     *
     * @param conditions     List of conditions to search.
     * @param fieldName      Field name to match.
     * @param datatype       Data type (String, Double, Long).
     * @param queryOperand   Operator (e.g., "=", "<", ">").
     * @return               The formatted operand value, or an empty string if not found.
     */
    private static String getConditionValue(List<SearchCondition> conditions, String fieldName, String datatype, String queryOperand) {
        for (SearchCondition con : conditions) {
            if (fieldName.equalsIgnoreCase(con.getFieldName()) && con.getOperator().toString().equalsIgnoreCase(queryOperand)) {
                if (con.getOperand() == null) {
                    return "";
                } else if (datatype.equalsIgnoreCase(Constants.STRING)) {
                    return String.valueOf(con.getOperand()).trim();
                } else if (datatype.equalsIgnoreCase("Double")) {
                    return BigDecimal.valueOf((Double) con.getOperand()).toPlainString().trim();
                } else if (datatype.equalsIgnoreCase("Long")) {
                    String dateString = String.valueOf(con.getOperand()).trim().replace("E", "").replace(".", "");
                    long epoch = Long.parseLong(dateString) * 1000;
                    Date date = new Date(dateString.length() == 9 ? epoch * 10 : epoch);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000+0000");
                    return dateFormat.format(date);
                }
            }
        }
        return "";
    }

    /**
     * Constructs the final SOQL query string using provided filter and pagination/sorting parameters.
     *
     * @return A SOQL query string for Salesforce Case object.
     */
    private static String createQuery(String id, String contactId, String accountId, String contactName, String subject, String ownerId, String accountName, String caseNumber, String contactEmail, String contactPhone, String status, String origin, String createdDate, String createdDateLessThan, String createdDateGreaterThan, String modifiedDate,
                                      String modifiedDateLessThan, String modifiedDateGreaterThan, long size, long page, String orderByField, String orderByOperand) {
        StringBuilder builder = new StringBuilder();
        builder.append("Select " + QUERY_FIELDS + " from Case ")
                .append(WHERE)
                .append(isStringAvailable(id) ? " Id = '" + id + "' " + AND : "")
                .append(isStringAvailable(contactId) ? " ContactId = '" + contactId + "' " + AND : "")
                .append(isStringAvailable(accountId) ? " AccountId = '" + accountId + "' " + AND : "")
                .append((isStringAvailable(contactName) ? " Contact.Name LIKE '%" + contactName + "%' " + AND : ""))
                .append((isStringAvailable(subject) ? " Subject LIKE '%" + subject + "%' " + AND : ""))
                .append(isStringAvailable(ownerId) ? " OwnerId = '" + ownerId + "' " + AND : "")
                .append(isStringAvailable(accountName) ? " Account.Name = '" + accountName + "' " + AND : "")
                .append((isStringAvailable(caseNumber) ? " CaseNumber LIKE '%" + caseNumber + "%' " + AND : ""))
                .append((isStringAvailable(contactEmail) ? " ContactEmail LIKE '%" + contactEmail + "%' " + AND : ""))
                .append((isStringAvailable(contactPhone) ? " ContactPhone LIKE '%" + contactPhone + "%' " + AND : ""))
                .append(isStringAvailable(status) ? " Status = '" + status + "' " + AND : "")
                .append(isStringAvailable(origin) ? " Origin = '" + origin + "' " + AND : "")
                .append(isStringAvailable(createdDate) ? " createdDate = " + createdDate + " " + AND : "")
                .append(isStringAvailable(createdDateLessThan) ? " createdDate < " + createdDateLessThan + " " + AND : "")
                .append(isStringAvailable(createdDateGreaterThan) ? " createdDate > " + createdDateLessThan + " " + AND : "")
                .append(isStringAvailable(modifiedDate) ? " modifiedDate = " + modifiedDate + " " + AND : "")
                .append(isStringAvailable(modifiedDateLessThan) ? " modifiedDate < " + modifiedDateLessThan + " " + AND : "")
                .append(isStringAvailable(modifiedDateGreaterThan) ? " modifiedDate > " + modifiedDateGreaterThan + " " + AND : "");
        StringHelper.setQueryEndIndex(builder);
        StringHelper.appendOrderByClause(size, page, orderByField, orderByOperand, builder);
        return builder.toString();
    }
}
