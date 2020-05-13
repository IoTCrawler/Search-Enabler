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


import com.agtinternational.iotcrawler.core.ontologies.IotStream;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.GenericMDRWiring;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.language.*;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import graphql.schema.idl.errors.SchemaProblem;
import graphql.schema.idl.errors.SchemaRedefinitionError;
import org.apache.commons.lang3.NotImplementedException;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions.newOptions;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.Arrays.asList;

@Component
public class GraphQLProvider {
    static Logger LOGGER = LoggerFactory.getLogger(GraphQLProvider.class);

    private GraphQL graphQL;

    private GraphQLSchema graphQLSchema;
    private DataLoaderRegistry dataLoaderRegistry;
    private GenericMDRWiring wiring;
    private Context context;

    TypeDefinitionRegistry typeRegistry;
    Map<String, String> bindingRegistry = new LinkedHashMap<>();
    Map<String, List<String>> topDownInheritance = new LinkedHashMap<>();
    Map<String, List<String>> bottomUpHierarchy = new LinkedHashMap<>();
    List<String> coreTypes = new ArrayList<>();
    TypeRuntimeWiring.Builder wiringBuilder;

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
    public void init() throws Exception {

        bindingRegistry.put("altType", IotStream.alternativeType);
        typeRegistry = mergeSchemas(wiring.getSchemas());
        wiringBuilder = newTypeWiring("Query");

        processDirectives();

        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring().type(wiringBuilder).scalar(ExtendedScalars.Object);

        wiring.setRuntimeWiringBuilder(runtimeWiringBuilder);
        wiring.setBindingRegistry(bindingRegistry);
        wiring.setInheritanceRegistry(topDownInheritance);
        wiring.setBottomUpHierarchy(bottomUpHierarchy);
        //wiring.setCoreTypes(coreTypes);
        dataLoaderRegistry = wiring.getDataLoaderRegistry();

        context = new ContextProvider(dataLoaderRegistry).newContext();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = wiring.build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);


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

    private TypeDefinitionRegistry mergeSchemas(Map<String, String> schemas){
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        for (String schemaName:schemas.keySet()) {
            String schemaStr = schemas.get(schemaName);
            TypeDefinitionRegistry schemaTypeRegistry;
            try {
                schemaTypeRegistry = new SchemaParser().parse(schemaStr);
            } catch (Exception e) {
                LOGGER.error("Failed to parse schema {}: {}", schemaName, e.getLocalizedMessage());
                continue;
            }

//            try {
//                schemaTypeRegistry.remove(schemaTypeRegistry.schemaDefinition().get());
            //typeRegistry.merge(schemaTypeRegistry);
//            } catch (Exception e) {
//                LOGGER.error("Failed to merge schema {} into common one: {}", key, e.getLocalizedMessage());
//                continue;
//            }

            schemaTypeRegistry.types().values().forEach(newEntry -> {
                String name = newEntry.getName();

                try {

                    if(schemaTypeRegistry.getType(name).get() instanceof ObjectTypeDefinition && schemaName.equals("iotcrawler.graphqls"))
                        if(!coreTypes.contains(name))
                            coreTypes.add(name);

                    if (typeRegistry.getType(name).isPresent()){
                        TypeDefinition alreadyPresetentTypeDefinition = typeRegistry.getType(name).get();
                        if (alreadyPresetentTypeDefinition instanceof ObjectTypeDefinition) {
                            List<FieldDefinition> mergedDefinitions = ((ObjectTypeDefinition) alreadyPresetentTypeDefinition).getFieldDefinitions();
                            mergedDefinitions.addAll(((ObjectTypeDefinition) newEntry).getFieldDefinitions());
                            typeRegistry.add(alreadyPresetentTypeDefinition);
                        } else
                            throw new NotImplementedException(alreadyPresetentTypeDefinition.getClass().getCanonicalName());

                    } else
                        typeRegistry.add(newEntry);
                } catch (Exception e) {
                    LOGGER.error("Failed to merge field {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });


            Map<String, DirectiveDefinition> tempDirectiveDefs = new LinkedHashMap<>();
            schemaTypeRegistry.getDirectiveDefinitions().values().forEach(newEntry -> {
                try {
                    typeRegistry.add(newEntry);
                } catch (Exception e) {
                    LOGGER.error("Failed to merge directive {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });

            Map<String, ScalarTypeDefinition> tempScalarTypes = new LinkedHashMap<>();
            schemaTypeRegistry.scalars().values().forEach(newEntry -> {
                try {
                    typeRegistry.add(newEntry);
                } catch (Exception e) {
                    LOGGER.error("Failed to merge scalar {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });

            //
//            // merge type extensions since they can be redefined by design
            schemaTypeRegistry.objectTypeExtensions().forEach((key, value) -> {
                throw new NotImplementedException("objectTypeExtensions");
//                List<ObjectTypeExtensionDefinition> currentList = this.objectTypeExtensions
//                        .computeIfAbsent(key, k -> new ArrayList<>());
//                currentList.addAll(value);
            });
            schemaTypeRegistry.interfaceTypeExtensions().forEach((key, value) -> {
                throw new NotImplementedException("interfaceTypeExtensions");
//                List<InterfaceTypeExtensionDefinition> currentList = this.interfaceTypeExtensions
//                        .computeIfAbsent(key, k -> new ArrayList<>());
//                currentList.addAll(value);
            });
            schemaTypeRegistry.unionTypeExtensions().forEach((key, value) -> {
                throw new NotImplementedException("unionTypeExtensions");
//                List<UnionTypeExtensionDefinition> currentList = this.unionTypeExtensions
//                        .computeIfAbsent(key, k -> new ArrayList<>());
//                currentList.addAll(value);
            });
            schemaTypeRegistry.enumTypeExtensions().forEach((key, value) -> {
                throw new NotImplementedException("enumTypeExtensions");
//                List<EnumTypeExtensionDefinition> currentList = this.enumTypeExtensions
//                        .computeIfAbsent(key, k -> new ArrayList<>());
//                currentList.addAll(value);
            });
            schemaTypeRegistry.scalarTypeExtensions().forEach((key, value) -> {
                throw new NotImplementedException("scalarTypeExtensions");
//                List<ScalarTypeExtensionDefinition> currentList = this.scalarTypeExtensions
//                        .computeIfAbsent(key, k -> new ArrayList<>());
//                currentList.addAll(value);
            });
            schemaTypeRegistry.inputObjectTypeExtensions().forEach((key, value) -> {
                throw new NotImplementedException("inputObjectTypeExtensions");
//                List<InputObjectTypeExtensionDefinition> currentList = this.inputObjectTypeExtensions
//                        .computeIfAbsent(key, k -> new ArrayList<>());
//                currentList.addAll(value);
            });

        }
        return typeRegistry;
    }

    private void processDirectives(){
        for (TypeDefinition typeDefinition0 : typeRegistry.types().values()){

            if (typeDefinition0 instanceof ObjectTypeDefinition) {

                ObjectTypeDefinition typeDefinition = ((ObjectTypeDefinition) typeDefinition0);
                String typeName = typeDefinition.getName();
                wiring.registerDataloaderConcept(typeName);
                //dataLoaderRegistry.register(typeDefinition.getName(), new DataLoader(new GenericMDRWiring.GenericLoader(typeDefinition.getName())));

                ObjectTypeDefinition parentTypeDefinition = null;
                for (Directive directive : ((ObjectTypeDefinition) typeDefinition).getDirectives()) {
                    if (directive.getArgument("class") != null && directive.getArgument("class").getValue() != null)
                        bindingRegistry.put(typeName, ((StringValue) directive.getArgument("class").getValue()).getValue());

                    if (directive.getArgument("subClassOf") != null){
                        Value value = directive.getArgument("subClassOf").getValue();
                        List<String> parentTypeNames = Utils.extractValues(value);

                        for(String parentTypeName: parentTypeNames) {
                            parentTypeDefinition = (ObjectTypeDefinition) typeRegistry.getType(parentTypeName).get();
                            List<String> childClasses = (topDownInheritance.containsKey(parentTypeName) ? topDownInheritance.get(parentTypeName) : new ArrayList<>());
                            childClasses.add(typeName);
                            topDownInheritance.put(parentTypeName, childClasses);

                            List<String> parentClasses = (bottomUpHierarchy.containsKey(typeName) ? topDownInheritance.get(typeName) : new ArrayList<>());
                            parentClasses.add(parentTypeName);
                            bottomUpHierarchy.put(typeName, parentClasses);
                            //Filling subclass with parent type properties
                            if (parentTypeDefinition != null)
                                typeDefinition.getFieldDefinitions().addAll(parentTypeDefinition.getFieldDefinitions());
                        }
                    }
                }

                for (FieldDefinition fieldDefinition : ((ObjectTypeDefinition) typeDefinition).getFieldDefinitions()) {
                    String name2 = fieldDefinition.getName();

                    //extending parent type with child properties
//                        if (parentTypeDefinition != null && !parentTypeDefinition.getFieldDefinitions().contains(fieldDefinition))
//                            parentTypeDefinition.getFieldDefinitions().add(fieldDefinition);

                    if (typeName.toLowerCase().equals("query")) {
                        Object type = fieldDefinition.getType();
                        String fieldTypeName = null;
                        if (type instanceof ListType)
                            type = ((ListType) type).getType();

                        if (type instanceof TypeName)
                            fieldTypeName = ((TypeName) type).getName();
                        else
                            throw new NotImplementedException(type.getClass().getCanonicalName());
                        wiringBuilder.dataFetcher(name2, GenericMDRWiring.genericDataFetcher(fieldTypeName, false, new ArrayList<>()));
                    }

                    for (Directive directive : fieldDefinition.getDirectives())
                        if (directive.getArgument("uri") != null && directive.getArgument("uri").getValue() != null)
                            bindingRegistry.put(typeName + "." + name2, ((StringValue) directive.getArgument("uri").getValue()).getValue());
                }

//                    SchemaDefinition schemaDefinition = typeDefinition.transform();
//                    typeRegistry.add(schemaDefinition);
            }

        }
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
