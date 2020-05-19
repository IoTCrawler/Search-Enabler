package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.activity;

import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;

public class ReadingBookActivity extends ObjectBasedActivity {
    public ReadingBookActivity(String uri) {

        super(uri, HomeActivity.NS+"Reading_A_Book");
        label("Reading a book");
    }
}
