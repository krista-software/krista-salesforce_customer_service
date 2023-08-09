package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SFCaseHistory {
    public int totalSize;
    public boolean done;
    public List<Record> records;

    public Map<String, Object> toMap() {
        Map<String, Object> newRecord = new HashMap<>();
        newRecord.put(records.get(0).Field, records.get(0).NewValue);
        Long createdDate = records.get(0).CreatedDate.getTime();
        for (int i = 1; i < records.size(); i++) {
            if (createdDate > records.get(i).CreatedDate.getTime()) {
                return newRecord;
            } else if (createdDate == records.get(i).CreatedDate.getTime()) {
                newRecord.put(records.get(i).Field, records.get(i).NewValue);
            }
        }
        return newRecord;
    }


    public class Record {
        public Attributes attributes;
        public String Id;
        public boolean IsDeleted;
        public String CreatedById;
        public Date CreatedDate;
        public String Field;
        public String DataType;
        public String OldValue;
        public String NewValue;

        public class Attributes {
            public String type;
            public String url;
        }
    }
}

