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

import com.agtinternational.iotcrawler.graphqlEnabler.wiring.GenericMDRWiring;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.language.*;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.apache.commons.lang3.NotImplementedException;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions.newOptions;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.Arrays.asList;

@Component
public class GraphQLProvider {
    private GraphQL graphQL;

    private GraphQLSchema graphQLSchema;
    private DataLoaderRegistry dataLoaderRegistry;
    private GenericMDRWiring wiring;
    private Context context;

    //@Autowired
//    public GraphQLProvider(){
//        this.wiring = wiring;
//    }

    @Autowired
    public GraphQLProvider(){ //used for http app
        this(new IoTCrawlerWiring.Builder().build());

    }

    public GraphQLProvider(GenericMDRWiring wiring){  //used for outside tests
        this.wiring = wiring;
        this.dataLoaderRegistry = new DataLoaderRegistry();
    }


    @PostConstruct
    public void init() throws IOException {

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(wiring.getSchemaString());

        TypeRuntimeWiring.Builder wiringBuilder = newTypeWiring("Query");

        Map<String, String> bindingRegistry = new LinkedHashMap<>();
        for (TypeDefinition typeDefinition: typeRegistry.types().values()){

            if(typeDefinition instanceof ObjectTypeDefinition){


                wiring.registerDataloaderConcept(typeDefinition.getName());
                //dataLoaderRegistry.register(typeDefinition.getName(), new DataLoader(new GenericMDRWiring.GenericLoader(typeDefinition.getName())));

                if(((ObjectTypeDefinition) typeDefinition).getDescription()!=null)
                    bindingRegistry.put(typeDefinition.getName(),((ObjectTypeDefinition) typeDefinition).getDescription().getContent());


                for(FieldDefinition fieldDefinition : ((ObjectTypeDefinition) typeDefinition).getFieldDefinitions()){
                    String name2=fieldDefinition.getName();

                    if(typeDefinition.getName().toLowerCase().equals("query")) {
                        Object type = fieldDefinition.getType();
                        String typeName = null;
                        if(type instanceof ListType)
                            type = ((ListType)type).getType();

                        if(type instanceof TypeName)
                            typeName = ((TypeName)type).getName();
                        else
                            throw new NotImplementedException(type.getClass().getCanonicalName());
                        wiringBuilder.dataFetcher(name2, GenericMDRWiring.genericDataFetcher(typeName, false));
                    }

                    if(fieldDefinition.getDescription()!=null)
                        bindingRegistry.put(typeDefinition.getName()+"."+name2,fieldDefinition.getDescription().getContent());
                }
            }
        }
        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring().type(wiringBuilder).scalar(ExtendedScalars.Object);

        wiring.setRuntimeWiringBuilder(runtimeWiringBuilder);
        wiring.setBindingRegistry(bindingRegistry);
        dataLoaderRegistry = wiring.getDataLoaderRegistry();

        context = new ContextProvider(dataLoaderRegistry).newContext();


        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = wiring.build();


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
