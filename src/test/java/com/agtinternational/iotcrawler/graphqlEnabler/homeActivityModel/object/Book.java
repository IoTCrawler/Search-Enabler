package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.object;

import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;
import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.Object;

public class Book extends Object {
    public Book(String uri) {
        super(uri, HomeActivity.NS + "Book");

    }
}