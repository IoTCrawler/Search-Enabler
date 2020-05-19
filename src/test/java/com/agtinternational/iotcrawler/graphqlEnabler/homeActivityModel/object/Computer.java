package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.object;

import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;
import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.Object;

public class Computer extends Object {
    public Computer(String uri) {
        super(uri, HomeActivity.NS + "Computer");

    }
}
