package com.agtinternational.iotcrawler.graphqlEnabler.rule;

public class ContextRule {

    String typeDefinitionName;
    String fieldToAffect;

    Condition conditionToMeet;
    Condition conditionToApply;



    String consequencePropertyName;


    public ContextRule(String typeDefinitionName, Condition ifCondition, Condition conditionToApply){
        this.typeDefinitionName = typeDefinitionName;
        this.conditionToMeet = ifCondition;
        this.conditionToApply = conditionToApply;
    }

    public Condition getConditionToMeet() {
        return conditionToMeet;
    }

    public Condition getConditionToApply() {
        return conditionToApply;
    }

    //    public ContextRule(
//            String typeDefinitionName,
//            String fieldToAffect,
//            String conditionFieldName,
//            String conditionValue,
//            String consequencePropertyName,
//            String consequenceFieldName,
//            String consequenceValue){
//        this.typeDefinitionName = typeDefinitionName;
//        this.fieldToAffect = fieldToAffect;
//        this.conditionFieldName = conditionFieldName;
//        this.conditionValue = conditionValue;
//        this.consequencePropertyName = consequencePropertyName;
//        this.consequenceFieldName = consequenceFieldName;
//        this.consequenceValue = consequenceValue;
//    }

    public String getTypeDefinitionName() {
        return typeDefinitionName;
    }

    public String getFieldToAffect() {
        return fieldToAffect;
    }

//    public String getConditionFieldName() {
//        return conditionFieldName;
//    }
//
//    public String getConditionValue() {
//        return conditionValue;
//    }


    public String getConsequencePropertyName() {
        return consequencePropertyName;
    }

//    public String getConsequenceFieldName() {
//        return consequenceFieldName;
//    }
//
//    public String getConsequenceValue() {
//        return consequenceValue;
//    }


}
