package raff.stein.customer.service.aml.pipeline.step;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.bo.customer.Customer;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class AmlContext {

    private UUID amlCaseId;
    private String jurisdiction;
    private Customer customer;
    // shared data between steps
    private Map<String,Object> shared;


    public void addProperty(String key, Object value) {
        shared.put(key,value);
    }

    public Object getValue(String key){
        return shared.get(key);
    }
}
