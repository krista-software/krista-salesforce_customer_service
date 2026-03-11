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





