package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.object;

import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;
import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.Object;

public class TV extends Object {
    public TV(String uri) {
        super(uri, HomeActivity.NS + "TV");

    }
}
