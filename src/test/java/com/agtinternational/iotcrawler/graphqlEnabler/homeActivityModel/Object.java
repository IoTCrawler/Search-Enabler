package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel;

import com.agtinternational.iotcrawler.core.models.RDFModel;
import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;

public class Object extends RDFModel {
    public Object(String uri, String typeURI) {
        super(uri, typeURI);
        namespaces.put(HomeActivity.Prefix, HomeActivity.NS);
    }
}