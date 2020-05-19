package com.agtinternational.iotcrawler.graphqlEnabler;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class ManualTestsHomeActivity extends SchemasTests {
    protected static Logger LOGGER = LoggerFactory.getLogger(AutomatedTests.class);


    @Test
    public void getStreamByIdTest() throws Exception {

        executeQuery(Paths.get("queries","core","getStreamById"));
    }
}
