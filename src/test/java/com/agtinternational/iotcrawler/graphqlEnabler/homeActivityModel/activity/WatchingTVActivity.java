package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.activity;

import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;

public class WatchingTVActivity extends ObjectBasedActivity {
    public WatchingTVActivity(String uri) {
        super(uri, HomeActivity.NS+"Watching_TV");
        label("Watching TV");

    }
}

