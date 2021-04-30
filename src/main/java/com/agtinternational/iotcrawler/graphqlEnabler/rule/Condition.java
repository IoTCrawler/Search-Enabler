package com.agtinternational.iotcrawler.graphqlEnabler.rule;

public class Condition {
    String definesField;
    String key;
    String value;

    public Condition(String definesField, String key, String value){
        this.definesField = definesField;
        this.key = key;
        this.value = value;
    }

    public boolean meets(String value){
        if(this.value.equals(value))
            return true;
        return false;
    }

    public String getDefinesField() {
        return definesField;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
