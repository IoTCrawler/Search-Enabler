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


import com.agtinternational.iotcrawler.graphqlEnabler.resolving.UniversalDataFetcher;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.HierarchicalWiring;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.MultipleSchemasWiring;
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
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions.newOptions;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.Arrays.asList;

@Component
public class GraphQLProvider {
    static Logger LOGGER = LoggerFactory.getLogger(GraphQLProvider.class);

    private GraphQL graphQL;

    private GraphQLSchema graphQLSchema;
    private DataLoaderRegistry dataLoaderRegistry;
    private HierarchicalWiring wiring;
    private Context context;

    TypeDefinitionRegistry typeRegistry;
    Map<String, String> bindingRegistry = new LinkedHashMap<>();
    Map<String, List<String>> topDownInheritance = new LinkedHashMap<>();
    Map<String, List<String>> bottomUpHierarchy = new LinkedHashMap<>();
    //List<String> coreTypes = new ArrayList<>();
    TypeRuntimeWiring.Builder wiringBuilder;

    //@Autowired
//    public GraphQLProvider(){
//        this.wiring = wiring;
//    }

    @Autowired
    public GraphQLProvider(){ //used for http app
        this(new MultipleSchemasWiring.Builder().build());

    }

    public GraphQLProvider(HierarchicalWiring wiring){  //used for outside tests
        this.wiring = wiring;
        this.dataLoaderRegistry = new DataLoaderRegistry();
    }


    @PostConstruct
    public void init() throws Exception {

        typeRegistry = mergeSchemas(wiring.getSchemas());
        if(typeRegistry.types().size()==0)
            throw new Exception("Empty schema. No types have been merged");

        wiringBuilder = newTypeWiring("Query");

        buildHierarchyFromDirectives();
        fillBindingRegistry();

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
//        typeRegistry.scalarTypeExtensions().put("Date",
//                ScalarTypeExtensionDefinition
//                .newScalarTypeExtensionDefinition()
//                        .name("Date").build());
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
        LOGGER.debug("Merging schemas");
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

            LOGGER.debug("Merging schema {}",schemaName);
            List<String> mergedNames = new ArrayList<>();
            schemaTypeRegistry.types().values().forEach(newEntry -> {
                String name = newEntry.getName();
                try {
                    if(schemaTypeRegistry.getType(name).get() instanceof ObjectTypeDefinition && schemaName.equals("iotcrawler.graphqls")) {
                        //if (!coreTypes.contains(name))
                         //   coreTypes.add(name);
                    }
                    //Merging types under Query type
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
                    mergedNames.add(name);
                } catch (Exception e) {
                    LOGGER.error("Failed to merge field {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });
            LOGGER.debug("Merged types: {}",String.join(",", mergedNames));
            mergedNames.clear();

            schemaTypeRegistry.getDirectiveDefinitions().values().forEach(newEntry -> {
                try {
                    typeRegistry.add(newEntry);
                    mergedNames.add(newEntry.getName());
                } catch (Exception e) {
                    LOGGER.error("Failed to merge directive {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });
            LOGGER.debug("Merged directives: {}",String.join(",", mergedNames));
            mergedNames.clear();

            schemaTypeRegistry.scalars().values().forEach(newEntry -> {
                try {
                    typeRegistry.add(newEntry);
                    mergedNames.add(newEntry.getName());
                } catch (Exception e) {
                    LOGGER.error("Failed to merge scalar {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });
            LOGGER.debug("Merged scalars: {}",String.join(",", mergedNames));
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

    private List<String> processTypeDirectives(String typeName, List<Directive> directives){
        List<String> topLevelTypes = new ArrayList<>();
        for (Directive directive : directives){
            if (directive.getArgument("subClassOf") != null){
                Value value = directive.getArgument("subClassOf").getValue();
                List<String> parentTypeNames = Utils.extractValues(value);

                for(String parentTypeName: parentTypeNames){
                    List<String> childClasses = (topDownInheritance.containsKey(parentTypeName) ? topDownInheritance.get(parentTypeName) : new ArrayList<>());
                    childClasses.add(typeName);
                    topDownInheritance.put(parentTypeName, childClasses);

                    List<String> parentClasses = (bottomUpHierarchy.containsKey(typeName) ? topDownInheritance.get(typeName) : new ArrayList<>());
                    parentClasses.add(parentTypeName);
                    bottomUpHierarchy.put(typeName, parentClasses);
                }
            }else topLevelTypes.add(typeName);
        }
        return topLevelTypes;
    }

    private void buildHierarchyFromDirectives(){
        LOGGER.debug("Building hierarchy from directives");
        List<String> topLevelTypes = new ArrayList<>();
        for (TypeDefinition typeDefinition0 : typeRegistry.types().values()){
            if (typeDefinition0 instanceof ObjectTypeDefinition){

                ObjectTypeDefinition typeDefinition = ((ObjectTypeDefinition) typeDefinition0);
                String typeName = typeDefinition.getName();
                wiring.registerDataloaderConcept(typeName);
                //dataLoaderRegistry.register(typeDefinition.getName(), new DataLoader(new GenericMDRWiring.GenericLoader(typeDefinition.getName())));

                List<Directive> directives = ((ObjectTypeDefinition) typeDefinition).getDirectives();
                List<String> appendToTopLevelTypes = processTypeDirectives(typeName, directives);
                appendToTopLevelTypes.forEach(type->{
                    if(!topLevelTypes.contains(type));
                    topLevelTypes.add(type);
                });

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
                        wiringBuilder.dataFetcher(name2, UniversalDataFetcher.get(fieldTypeName));
                    }

                }

            }else if(typeDefinition0 instanceof InputObjectTypeDefinition){
                InputObjectTypeDefinition inputObjectTypeDefinition = ((InputObjectTypeDefinition) typeDefinition0);
                List<Directive> directives = inputObjectTypeDefinition.getDirectives();
                List<String> appendToTopLevelTypes = processTypeDirectives(typeDefinition0.getName(), directives);
                appendToTopLevelTypes.forEach(type->{
                    if(!topLevelTypes.contains(type));
                    topLevelTypes.add(type);
                });


            }

        }
        for(String parentTypeName: topLevelTypes)
            addParentClassProperties(parentTypeName);

        String abc ="123";
    }


    public void addParentClassProperties(String parentTypeName){
        if(!topDownInheritance.containsKey(parentTypeName))
            return;
        for(String childTypeName: topDownInheritance.get(parentTypeName)){
            TypeDefinition typeDefinition = typeRegistry.getType(childTypeName).get();
            TypeDefinition parentTypeDefinition = typeRegistry.getType(parentTypeName).get();
            if(parentTypeDefinition==null)
                continue;

            List<FieldDefinition> parentTypeFieldDefinitions = new ArrayList<>();
            if (parentTypeDefinition instanceof ObjectTypeDefinition)
                parentTypeFieldDefinitions = ((ObjectTypeDefinition) parentTypeDefinition).getFieldDefinitions();
            else
                throw new NotImplementedException("");


            if(typeDefinition instanceof ObjectTypeDefinition) {
                List<FieldDefinition> childTypeDefinitions = ((ObjectTypeDefinition)typeDefinition).getFieldDefinitions();
                List<String> childTypeDefinitionNames = childTypeDefinitions.stream().map(type -> type.getName()).collect(Collectors.toList());
                parentTypeFieldDefinitions.forEach(typeDef -> {
                    if (!childTypeDefinitionNames.contains(typeDef.getName()))
                        childTypeDefinitions.add(typeDef);
                });
            }else if(typeDefinition instanceof InputObjectTypeDefinition){
                List<InputValueDefinition> childTypeDefinitions = ((InputObjectTypeDefinition)typeDefinition).getInputValueDefinitions();
                List<String> childTypeDefinitionNames = childTypeDefinitions.stream().map(type -> type.getName()).collect(Collectors.toList());
                parentTypeFieldDefinitions.forEach(typeDef -> {
                    if (!childTypeDefinitionNames.contains(typeDef.getName())){

                        //Type type = new TypeName(typeDef.getName()+"Input");
                        Type type = typeDef.getType();

                        if(type instanceof NonNullType){
                            type = ((NonNullType)type).getType();
                         }else if(!(type instanceof TypeName))
                            throw new NotImplementedException("");

                        if(type instanceof TypeName) {
                            String name = ((TypeName)(type)).getName();
                            TypeDefinition existingType = typeRegistry.getType(name).get();
                            if(existingType instanceof ObjectTypeDefinition) {
                                TypeName typeName = new TypeName(name+"Input");
                                InputValueDefinition inputValueDefinition = new InputValueDefinition(typeDef.getName(), typeName);
                                childTypeDefinitions.add(inputValueDefinition);
                            }

                        }else
                            throw new NotImplementedException("");

                    }
                        //childTypeDefinitions.add(typeDef);
                });
            }else
                throw new NotImplementedException("");




            addParentClassProperties(childTypeName);
        }
    }

    private void fillBindingRegistry(){
        LOGGER.debug("Filling binding registry");
        List<String> topLevelTypes = new ArrayList<>();
        for (TypeDefinition typeDefinition0 : typeRegistry.types().values()){
            if (typeDefinition0 instanceof ObjectTypeDefinition) {

                ObjectTypeDefinition typeDefinition = ((ObjectTypeDefinition) typeDefinition0);
                String typeName = typeDefinition.getName();

                for (Directive directive : ((ObjectTypeDefinition) typeDefinition).getDirectives()) {
                    if (directive.getArgument("class") != null && directive.getArgument("class").getValue() != null)
                        bindingRegistry.put(typeName, ((StringValue) directive.getArgument("class").getValue()).getValue());
                }

                for (FieldDefinition fieldDefinition : ((ObjectTypeDefinition) typeDefinition).getFieldDefinitions()) {
                    String name2 = fieldDefinition.getName();
                    for (Directive directive : fieldDefinition.getDirectives())
                        if (directive.getArgument("uri") != null && directive.getArgument("uri").getValue() != null)
                            bindingRegistry.put(typeName + "." + name2, ((StringValue) directive.getArgument("uri").getValue()).getValue());
                }

            }

        }
        for(String parentTypeName: topLevelTypes)
            addParentClassProperties(parentTypeName);

        String abc ="123";
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