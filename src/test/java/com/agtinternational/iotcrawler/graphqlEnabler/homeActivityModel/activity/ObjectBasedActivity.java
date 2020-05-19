package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.activity;

import com.agtinternational.iotcrawler.core.models.RDFModel;
import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;

public class ObjectBasedActivity extends RDFModel {
    public ObjectBasedActivity(String uri, String typeUri) {
        super(uri, typeUri);
        namespaces.put(HomeActivity.Prefix, HomeActivity.NS);
    }

    public Object hasUse(){
        return getAttribute(HomeActivity.hasUse);
    }

    public void generatedBy(Object value){
        addProperty(HomeActivity.hasUse, value);
    }
}