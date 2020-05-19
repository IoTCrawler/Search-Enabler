package com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.activity;

import com.agtinternational.iotcrawler.graphqlEnabler.homeActivityModel.HomeActivity;
import org.apache.jena.vocabulary.RDFS;

public class WorkingOnComputerActivity extends ObjectBasedActivity {
    public WorkingOnComputerActivity(String uri) {
        super(uri, HomeActivity.NS+"Working_on_computer");
        label("Working on computer");

    }
}
