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


import com.agtinternational.iotcrawler.graphqlEnabler.fetching.RecursiveDataFetcher;
import com.agtinternational.iotcrawler.graphqlEnabler.rule.Condition;
import com.agtinternational.iotcrawler.graphqlEnabler.rule.ContextRule;
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
import org.apache.commons.lang3.tuple.Pair;
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
    Map<String, List<ContextRule>> ifThenRulesRegistry = new LinkedHashMap<>();
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
        wiring.setDirectivesRegistry(ifThenRulesRegistry);
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

    private TypeDefinitionRegistry mergeSchemas(Map<String, String> schemas) throws Exception {
        LOGGER.debug("Merging schemas");
        TypeDefinitionRegistry mergedTypeRegistry = new TypeDefinitionRegistry();
        for (String schemaName:schemas.keySet()) {
            String schemaStr = schemas.get(schemaName);
            TypeDefinitionRegistry schemaTypeRegistry;
            try {
                schemaTypeRegistry = new SchemaParser().parse(schemaStr);
            } catch (Exception e) {
                LOGGER.error("Failed to parse schema {}: {}", schemaName, e.getLocalizedMessage());
                throw new Exception("Failed to parse schema "+ schemaName+": "+e.getLocalizedMessage());
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
                    if (mergedTypeRegistry.getType(name).isPresent()){
                        TypeDefinition alreadyPresetentTypeDefinition = mergedTypeRegistry.getType(name).get();
                        if (alreadyPresetentTypeDefinition instanceof ObjectTypeDefinition) {
                            List<FieldDefinition> mergedDefinitions = ((ObjectTypeDefinition) alreadyPresetentTypeDefinition).getFieldDefinitions();
                            mergedDefinitions.addAll(((ObjectTypeDefinition) newEntry).getFieldDefinitions());
                            mergedTypeRegistry.add(alreadyPresetentTypeDefinition);
                        } else if (alreadyPresetentTypeDefinition instanceof InputObjectTypeDefinition) {
                            List<InputValueDefinition> mergedDefinitions = ((InputObjectTypeDefinition) alreadyPresetentTypeDefinition).getInputValueDefinitions();
                            //mergedDefinitions.addAll(((InputObjectTypeDefinition) newEntry).getInputValueDefinitions());
                            //typeRegistry.add(alreadyPresetentTypeDefinition);
                        }else
                            throw new NotImplementedException(alreadyPresetentTypeDefinition.getClass().getCanonicalName());

                    } else
                        mergedTypeRegistry.add(newEntry);
                    mergedNames.add(name);
                } catch (Exception e) {
                    LOGGER.error("Failed to merge field {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });
            LOGGER.debug("Merged types: {}",String.join(",", mergedNames));
            mergedNames.clear();

            schemaTypeRegistry.getDirectiveDefinitions().values().forEach(newEntry -> {
                try {
                    mergedTypeRegistry.add(newEntry);
                    mergedNames.add(newEntry.getName());
                } catch (Exception e) {
                    LOGGER.error("Failed to merge directive {} into common schema: {}", newEntry.getName(), e.getLocalizedMessage());
                }
            });
            LOGGER.debug("Merged directives: {}",String.join(",", mergedNames));
            mergedNames.clear();

            schemaTypeRegistry.scalars().values().forEach(newEntry -> {
                try {
                    mergedTypeRegistry.add(newEntry);
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
        return mergedTypeRegistry;
    }

    private List<String> processTypeDirectives(String typeName, List<Directive> directives){
        List<String> topLevelTypes = new ArrayList<>();
        for (Directive directive : directives){
            if(directive.getName().equals("resource")) {
                if (directive.getArgument("subClassOf") != null) {
                    Value value = directive.getArgument("subClassOf").getValue();
                    List<String> parentTypeNames = Utils.extractValues(value);

                    for (String parentTypeName : parentTypeNames) {
                        List<String> childClasses = (topDownInheritance.containsKey(parentTypeName) ? topDownInheritance.get(parentTypeName) : new ArrayList<>());
                        childClasses.add(typeName);
                        topDownInheritance.put(parentTypeName, childClasses);

                        List<String> parentClasses = (bottomUpHierarchy.containsKey(typeName) ? topDownInheritance.get(typeName) : new ArrayList<>());
                        parentClasses.add(parentTypeName);
                        bottomUpHierarchy.put(typeName, parentClasses);
                    }
                } else topLevelTypes.add(typeName);
            }
        }
        return topLevelTypes;
    }

    private void buildHierarchyFromDirectives(){
        LOGGER.debug("Building hierarchy from directives");
        List<String> topLevelTypes = new ArrayList<>();
        for (TypeDefinition typeDefinition0 : typeRegistry.types().values()){
            if (typeDefinition0 instanceof ObjectTypeDefinition){

                ObjectTypeDefinition typeDefinition = ((ObjectTypeDefinition) typeDefinition0);
                String typeDefinitionName = typeDefinition.getName();
                wiring.registerDataloaderConcept(typeDefinitionName);
                //dataLoaderRegistry.register(typeDefinition.getName(), new DataLoader(new GenericMDRWiring.GenericLoader(typeDefinition.getName())));

                List<Directive> resourceDirectives = ((ObjectTypeDefinition) typeDefinition).getDirectives().stream().filter(directive -> directive.getName().equals("resource")).collect(Collectors.toList());
                List<String> appendToTopLevelTypes = processTypeDirectives(typeDefinitionName, resourceDirectives);
                appendToTopLevelTypes.forEach(type->{
                    if(!topLevelTypes.contains(type));
                    topLevelTypes.add(type);
                });

                List<Directive> contextDirectives = ((ObjectTypeDefinition) typeDefinition).getDirectives().stream().filter(directive -> directive.getName().equals("context")).collect(Collectors.toList());
                processContextDirectives(contextDirectives, typeDefinitionName);

                for (FieldDefinition fieldDefinition : ((ObjectTypeDefinition) typeDefinition).getFieldDefinitions()) {
                    String fieldDefinitionName = fieldDefinition.getName();

                    //extending parent type with child properties
//                        if (parentTypeDefinition != null && !parentTypeDefinition.getFieldDefinitions().contains(fieldDefinition))
//                            parentTypeDefinition.getFieldDefinitions().add(fieldDefinition);
                    List<Directive> fieldContextDirectives = fieldDefinition.getDirectives().stream().filter(directive -> directive.getName().equals("context")).collect(Collectors.toList());
                    processContextDirectives(fieldContextDirectives, typeDefinitionName+"."+fieldDefinitionName);

                    if (typeDefinitionName.toLowerCase().equals("query")) {
                        Object type = fieldDefinition.getType();
                        String fieldTypeName = null;
                        if (type instanceof ListType)
                            type = ((ListType) type).getType();

                        if (type instanceof TypeName)
                            fieldTypeName = ((TypeName) type).getName();
                        else
                            throw new NotImplementedException(type.getClass().getCanonicalName());
                        wiringBuilder.dataFetcher(fieldDefinitionName, RecursiveDataFetcher.get(fieldTypeName));
                    }

                }

            }else if(typeDefinition0 instanceof InputObjectTypeDefinition){
                InputObjectTypeDefinition inputObjectTypeDefinition = ((InputObjectTypeDefinition) typeDefinition0);
                List<Directive> directives = inputObjectTypeDefinition.getDirectives().stream().filter(directive -> directive.getName().equals("resource")).collect(Collectors.toList());
                List<String> appendToTopLevelTypes = processTypeDirectives(typeDefinition0.getName(), directives);
                appendToTopLevelTypes.forEach(type->{
                    if(!topLevelTypes.contains(type));
                    topLevelTypes.add(type);
                });


            }

        }
        for(String parentTypeName: topLevelTypes)
            addParentClassProperties(parentTypeName);
    }

    public void processContextDirectives(List<Directive> contextDirectives, String typeDefinitionName){
            Map<String, List<Pair<String[], String[]>>> ifThenRules = new HashMap<>();
            for(Directive contextRuleDirective: contextDirectives){
                Argument argument = contextRuleDirective.getArgument("rules");

                for(Value rule: ((ArrayValue)argument.getValue()).getValues()){
//                    String conditionFieldName = null;
//                    String conditionValue=null;
                    Condition conditionToBeMet=null;
                    String consequencePropertyName=null;
                    String consequenceFieldName=null;
                    Condition conditionToBeApplied= null;

                    String[] splitted0 = typeDefinitionName.replace(".", "#").split("#");
                    typeDefinitionName = splitted0[0];
                    String propertyName = splitted0[1];

                    for(ObjectField field: ((ObjectValue)rule).getObjectFields()){
                        String expressionString = ((StringValue)field.getValue()).getValue();
                        String[] keyAndValue = expressionString.split("=");

                        if(field.getName().equals("if")) {

                            String conditionFieldName = keyAndValue[0];
                            String conditionValue = keyAndValue[1];
                            if(conditionFieldName.contains("."))
                                conditionFieldName = conditionFieldName.replace(".","#").split("#")[1];

                            conditionToBeMet = new Condition(propertyName, conditionFieldName, conditionValue);
                        }else {
                            consequenceFieldName = keyAndValue[0];
                            if(consequenceFieldName.contains(".")){
                                List<String> splitted2 = Arrays.asList(consequenceFieldName.replace(".", "#").split("#"));
                                consequencePropertyName = splitted2.get(splitted2.size()-2);
                                //consequencePropertyName = String.join(".", splitted2.subList(0, splitted2.size()-1));// splitted2[splitted2.length-2];
                                if(consequencePropertyName.endsWith("*"))
                                    consequencePropertyName = consequencePropertyName.substring(0, consequencePropertyName.length()-1);
                                consequenceFieldName = splitted2.get(splitted2.size()-1);
                            }
                            String consequenceValue = keyAndValue[1];
                            conditionToBeApplied = new Condition(consequencePropertyName, consequenceFieldName, consequenceValue);
                        }
                    }
                    String key =  typeDefinitionName;
                    if(!key.contains(".")){ // when directive applied to type, when field name should be appended
                        key+="."+propertyName;
                    }

                    ContextRule contextRule = new ContextRule(typeDefinitionName, conditionToBeMet, conditionToBeApplied);
                    List<ContextRule> list = new ArrayList<>();

                    if(ifThenRulesRegistry.containsKey(key))
                        list = ifThenRulesRegistry.get(key);
                    list.add(contextRule);
                    ifThenRulesRegistry.put(key, list);
                }
            }



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