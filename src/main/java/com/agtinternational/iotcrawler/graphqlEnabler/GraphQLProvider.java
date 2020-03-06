package com.agtinternational.iotcrawler.graphqlEnabler;

/*-
 * #%L
 * graphql-enabler
 * %%
 * Copyright (C) 2019 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
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

import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions.newOptions;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.Arrays.asList;

@Component
public class GraphQLProvider {
    private GraphQL graphQL;

    private GraphQLSchema graphQLSchema;
    private DataLoaderRegistry dataLoaderRegistry;
    private Wiring wiring;
    private Context context;

    @Autowired
    public GraphQLProvider() {

    }


    @PostConstruct
    public void init() throws IOException {

        wiring = new IoTCrawlerWiring();

        dataLoaderRegistry = wiring.getDataLoaderRegistry();
        context = new ContextProvider(dataLoaderRegistry).newContext();

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(wiring.getSchemaString());

        //SchemaParser.newParser().scalars(myUuidScalar)

        //typeRegistry.add(type);
        //RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.DateTime);

        RuntimeWiring runtimeWiring = wiring.build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);

        //GraphQLSchema graphQLSchema = buildSchema(wiring.getSchemaString());

        DataLoaderDispatcherInstrumentation dlInstrumentation =
                //new DataLoaderDispatcherInstrumentation(newOptions().includeStatistics(true));
                new DataLoaderDispatcherInstrumentation(dataLoaderRegistry, newOptions().includeStatistics(true));

        Instrumentation instrumentation = new ChainedInstrumentation(
                asList(new TracingInstrumentation(), dlInstrumentation)
        );


        this.graphQL = GraphQL.newGraphQL(graphQLSchema)
                              .instrumentation(instrumentation)
                              //.queryExecutionStrategy()
                              .build();
    }



    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    public Context getContext() {
        return context;
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

}
