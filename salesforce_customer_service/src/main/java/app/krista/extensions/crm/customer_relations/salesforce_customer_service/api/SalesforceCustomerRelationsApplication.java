package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@Service
@ApplicationPath("/")
@ContractsProvided(Application.class)
public class SalesforceCustomerRelationsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(AuthenticationResource.class);
    }
}
