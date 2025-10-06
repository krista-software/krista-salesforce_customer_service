package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.AccountDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.ContactDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CustomerDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.IndividualDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidationOrchestrator {

    @Inject
    public ValidationOrchestrator(CaseDaoImpl caseDao, AccountDaoImpl accountDao, ContactDaoImpl contactDao, CustomerDaoImpl customerDao, IndividualDaoImpl individualDao, SalesforceService salesforceService) {
        validators.put(Validator.ValidationResource.CASE_ID, new CaseIdValidator(caseDao));
        validators.put(Validator.ValidationResource.NUMBER_OF_POSTS, new NumberOfPostsValidator());
        validators.put(Validator.ValidationResource.UPDATE_MESSAGE, new UpdateMessageValidator());
        validators.put(Validator.ValidationResource.FILE, new FileValidator());
        validators.put(Validator.ValidationResource.OBJECT_ID, new ObjectIdValidator(accountDao, caseDao, contactDao, individualDao, customerDao));
        validators.put(Validator.ValidationResource.OBJECT_TYPE, new ObjectTypeValidator());
        validators.put(Validator.ValidationResource.CUSTOM_FIELDS, new CustomFieldsValidator(salesforceService));

    }

    public record ValidationResult(String confirmStepMessage, String fetchFieldName, String fetchStepMessage,
                                   String errMessage, String fieldType) {
    }

    private final Map<Validator.ValidationResource, Validator> validators = new HashMap<>();

    public List<ValidationResult> validate(Map<Validator.ValidationResource, String> resources) {
        List<ValidationResult> results = new ArrayList<>();
        for (Map.Entry<Validator.ValidationResource, String> entry : resources.entrySet()) {
            Validator validator = validators.get(entry.getKey());
            if (validator == null) {
                throw new IllegalStateException("Validator not found for resource: " + entry.getKey());
            }
            if (!validator.validate(entry.getValue(), resources)) {
                results.add(new ValidationResult(validator.getConfirmationStepMessage(entry.getValue(), resources),
                        validator.getFetchFieldName(), validator.getFetchStepMessage(),
                        validator.getErrMessage(entry.getValue()), validator.getFieldType()));
            }
        }
        return results;
    }
}





