package com.agtinternational.iotcrawler.graphqlEnabler;

import com.agtinternational.iotcrawler.core.clients.GraphQLClient;
import com.google.common.io.Resources;

import java.nio.charset.Charset;

public class ExternalQueryRunner {

    public static void main(String[] args) throws Exception {
        GraphQLClient graphQLClient = new GraphQLClient("http://localhost:8081/graphql");
        int amountOfAttempts = 50;
        double total = 0;

        String query = String.join("\n", Resources.readLines(Resources.getResource("perfTests/4-types.gql"), Charset.defaultCharset()));
        for(int i=0; i<amountOfAttempts; i++) {
            long started = System.currentTimeMillis();
            graphQLClient.query(query);
            double took = (System.currentTimeMillis() - started) / 1000.0;
            total+=took;
            System.out.println(took);
        }
        System.out.println(total/amountOfAttempts);



    }
}
