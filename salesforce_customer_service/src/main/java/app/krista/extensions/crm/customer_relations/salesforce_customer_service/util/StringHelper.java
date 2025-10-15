package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.*;


/**
 * Utility class for common string-related operations and Salesforce query formatting helpers.
 * Contains static methods to assist with query construction, validation, and request body creation.
 */
public final class StringHelper {
    private StringHelper() {
    }


    /**
     * Checks if a string is non-null and not blank.
     *
     * @param str the input string
     * @return {@code true} if the string is non-null and not blank, {@code false} otherwise
     */
    public static boolean isStringAvailable(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * This method will append Order by clause to the given query string
     *
     * @param size           size of the query results per page
     * @param page           page number of the query results
     * @param orderByField   order by query clause field input
     * @param orderByOperand order by DESC or ASC
     * @param builder        input query to be appended with order by clause
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

    /**
     * Trims unnecessary {@code WHERE} or {@code AND} clauses at the end of a query.
     * This is useful when optional filters are appended conditionally.
     *
     * @param builder the {@link StringBuilder} representing the SOQL query
     */
    public static void setQueryEndIndex(StringBuilder builder) {
        if (builder.length() >= WHERE.length() &&
            builder.substring(builder.length() - WHERE.length()).equals(WHERE)) {
            builder.setLength(builder.lastIndexOf(WHERE));
        }
        if (builder.length() >= SPACE_AND.length() &&
            builder.substring(builder.length() - SPACE_AND.length()).equals(SPACE_AND)) {
            builder.setLength(builder.lastIndexOf(SPACE_AND));
        }
    }

    /**
     * Creates a JSON body string for posting a text message to a Salesforce case feed.
     *
     * @param caseId      the ID of the Salesforce case
     * @param postMessage the message text to post
     * @return a JSON-formatted string representing the feed post
     */
    public static String createFeedPostBody(String caseId, String postMessage) {
        return "{" +
                "\"body\": {" +
                "\"messageSegments\": [" +
                "{" +
                "\"type\": \"" + "Text" + "\"," +
                "\"text\": \"" + postMessage + "\"" +
                "}" +
                "]" +
                "}," +
                "\"feedElementType\": \"" + "FeedItem" + "\"," +
                "\"subjectId\": \"" + caseId + "\"" +
                "}";
    }

    /**
     * Creates a JSON body string for uploading a file attachment to Salesforce ContentVersion.
     *
     * @param base64EncodedString the Base64-encoded file content
     * @param fileName            the name of the file to be uploaded
     * @return a JSON-formatted string for the ContentVersion upload
     */
    public static String createFeedPostBodyForFile(String base64EncodedString, String fileName) {
        return String.format("{\"Title\":\"%s\",\"VersionData\":\"%s\",\"PathOnClient\":\"%s\"}", fileName, base64EncodedString, fileName);
    }

    /**
     * Creates a JSON body string for posting a text-based FeedItem to a case.
     *
     * @param body   the body text of the FeedItem
     * @param caseId the ID of the Salesforce case
     * @return a JSON-formatted string for the FeedItem post
     */
    public static String createFeedPostBodyForFeedItem(String body, String caseId) {
        return String.format("{\"Body\":\"%s\",\"ParentId\":\"%s\"}", body, caseId);
    }

    /**
     * Creates a JSON body string for linking an uploaded file to a Salesforce FeedItem.
     *
     * @param feedId    the FeedItem ID to associate the file with
     * @param recordId  the ContentVersion record ID
     * @param fileName  the name of the file
     * @return a JSON-formatted string for the FeedAttachment link
     */
    public static String createFeedPostBodyForFeedAttachment(String feedId, String recordId, String fileName) {
        String safeFeedId = feedId != null ? feedId.replace("\"", "") : "null";
        String safeRecordId = recordId != null ? recordId.replace("\"", "") : "null";
        String safeFileName = fileName != null ? fileName : "null";

        return String.format(
                "{\"FeedEntityId\":\"%s\",\"RecordId\":\"%s\",\"Title\":\"%s\",\"Type\":\"CONTENT\"}",
                safeFeedId,
                safeRecordId,
                safeFileName
        );
    }
}
