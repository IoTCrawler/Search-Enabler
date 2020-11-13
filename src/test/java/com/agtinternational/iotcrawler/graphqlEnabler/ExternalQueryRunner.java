package com.agtinternational.iotcrawler.graphqlEnabler;

/*-
 * #%L
 * search-enabler
 * %%
 * Copyright (C) 2019 - 2020 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.agtinternational.iotcrawler.core.clients.GraphQLClient;
import com.google.common.io.Resources;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ExternalQueryRunner {

    public static void main(String[] args) throws Exception {
        GraphQLClient graphQLClient = new GraphQLClient("http://localhost:8081/graphql");
        int amountOfAttempts = 5;
        double total = 0;

        String query = String.join("\n", Resources.readLines(Resources.getResource("perfTests/4-types.gql"), Charset.defaultCharset()));
        for(int i=0; i<amountOfAttempts; i++) {
            long started = System.currentTimeMillis();
            graphQLClient.query(query);
            double took = (System.currentTimeMillis() - started) / 1000.0;
            total+=took;
            System.out.println(took);
        }

        List<String> lines = Files.readAllLines(Paths.get("times.csv"));
        lines = lines.subList(lines.size()-amountOfAttempts, lines.size());
        double totalRequestsTime = 0.0;
        double totalExecutionTime = 0.0;
        int requestsPerformed = 0;
        for(String line : lines){
            String[] splitted = line.split(";");
            requestsPerformed = Integer.parseInt(splitted[0]);
            totalRequestsTime+=Double.parseDouble(splitted[1]);
            totalExecutionTime+=Double.parseDouble(splitted[2]);
        }

        //System.out.println("Client resolution time: "+ total/amountOfAttempts);
        System.out.println("Server resolution time: "+totalExecutionTime/amountOfAttempts);
        System.out.println("Requests ("+requestsPerformed+") execution time: "+totalRequestsTime/amountOfAttempts);
        //System.out.println("Queries times "+ String.join("+ ", GenericMDRWiring.getTotalQueryExectionList()));



    }
}
