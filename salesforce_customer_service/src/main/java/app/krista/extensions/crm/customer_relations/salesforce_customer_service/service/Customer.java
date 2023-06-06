package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

public interface Customer {
    /**
     * This function will return the id of the Customer entity
     *
     * @return id of the customer entity
     */
    String getId();

    /**
     * This function will return individual entity id for the customer instance
     *
     * @return id of the individual
     */
    String getPartyId();

    /**
     * This function will return the name of the customer entity
     *
     * @return name of the customer
     */
    String getName();

    /**
     * This function will return the status of the customer entity
     *
     * @return status of the customer
     */
    String getCustomerStatusType();

    /**
     * This function will return the lifetime value of the customer entity
     *
     * @return total lifetime of the customer
     */
    Double getTotalLifeTimeValue();
}
