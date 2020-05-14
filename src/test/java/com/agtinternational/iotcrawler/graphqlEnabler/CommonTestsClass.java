package com.agtinternational.iotcrawler.graphqlEnabler;

import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
import graphql.GraphQL;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class CommonTestsClass {
    protected static Logger LOGGER = LoggerFactory.getLogger(AutomatedTests.class);
    private Path queryFilePath;

    GraphQLProvider graphQLProvider;
    GraphQL graphql;
    Context context;

    @Before
    public void init() throws Exception {
        EnvVariablesSetter.init();

        LOGGER.info("Initing graphql provider");

        graphQLProvider = new GraphQLProvider(new IoTCrawlerWiring.Builder().build());
        graphQLProvider.init();
        graphql = graphQLProvider.graphQL();

        context = graphQLProvider.getContext();

    }

}
