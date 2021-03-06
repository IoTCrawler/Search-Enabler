package com.agtinternational.iotcrawler.graphqlEnabler.resolution;

/*-
 * #%L
 * search-enabler
 * %%
 * Copyright (C) 2019 - 2021 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
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

import com.agtinternational.iotcrawler.core.ontologies.NGSI_LD;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.fetching.RecursiveDataFetcher;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.HierarchicalWiring;
import graphql.execution.ExecutionTypeInfo;
import graphql.language.*;
import graphql.schema.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.CORE_TYPES;

public class BottomUpStrategy {
    static Logger LOGGER = LoggerFactory.getLogger(BottomUpStrategy.class);

    public static Map resolveFilters(DataFetchingEnvironment environment, Map<String, Object> argumentsToResolve) throws Exception {
        Map<String, Object> query = new HashMap<>();
        LOGGER.debug("Amending query by resolving the filters "+argumentsToResolve.toString());
        //setting alternative type as a condition
        if(environment.getArgument("subClassOf")!=null) {
            String parentTypeName = environment.getArgument("subClassOf");
            try {
                String parentTypeURI = HierarchicalWiring.findURI(parentTypeName);
                query.put(NGSI_LD.alternativeType, "\"" + parentTypeURI + "\"");
                query.putAll(argumentsToResolve);
                query.remove("subClassOf");
                argumentsToResolve.clear();
            } catch (Exception e) {
                LOGGER.warn("Failed to find URI for {}", parentTypeName);
                return null;
            }
        }
        //for(String argName: arguments.keySet()) {
        for(Field field: environment.getFields())
            for(Argument argument: field.getArguments())
                if(argumentsToResolve.containsKey(argument.getName())){

                    String argName = argument.getName();
                    Object argValue = argumentsToResolve.get(argName);

                    GraphQLOutputType outputType = environment.getFieldType();
                    //GraphQLType wrappedType = ((GraphQLModifiedType)outputType).getWrappedType();
                    String wrappedTypeName = ((GraphQLModifiedType)outputType).getWrappedType().getName();

                    String propertyURI = null;
                    try {
                        propertyURI = HierarchicalWiring.findURI(wrappedTypeName, argName);
                    }
                    catch (Exception e){
                        //ignoring exception
                    }
//                if(wrappedType instanceof GraphQLObjectType)
//                    propertyURI = ((GraphQLObjectType)wrappedType).getDescription();
//                else throw new NotImplementedException(wrappedType.getClass().getCanonicalName()+" not implemented");

                    if(propertyURI==null) {
                        LOGGER.warn("URI not found for " + argName+". Excluding from resolution");
                        continue;
                    }

                    GraphQLArgument graphQLArgument = environment.getFieldTypeInfo().getFieldDefinition().getArgument(argName);
                    GraphQLType graphQLInputType = environment.getFieldTypeInfo().getFieldDefinition().getType();
                    if(graphQLArgument!=null)//getting type from argument (if possible)
                        graphQLInputType = graphQLArgument.getType();
                    else
                        throw new Exception(argName+" not found in arguments of field definition of the environment!");

                    if(graphQLInputType instanceof GraphQLModifiedType)
                        graphQLInputType = ((GraphQLModifiedType) graphQLInputType).getWrappedType();

                    // (!) Hardcoded stuff requiring Sensor & SensorInput types in schema
                    // Get target type out of input type (to resolve it with a separate fetcher)
                    //ToDO: make this via search by input types of arguments or via declarations
                    String targetInputTypeName = graphQLInputType.getName().replace("Input","");
                    //String inputTypeName = graphQLInputType.getName();

                    GraphQLType targetInputType = environment.getGraphQLSchema().getType(targetInputTypeName);
                    if(targetInputType==null)
                        throw new Exception("Type " + targetInputTypeName + " not found in schema");

                    //if target type is just a filterArgument
                    if(targetInputType instanceof GraphQLInputObjectType){ //handling input type filters
                        List<Field> fields = new ArrayList<>();
                        String propertyName = argument.getName();
                        List<ObjectField> filtersToApply = ((ObjectValue) argument.getValue()).getObjectFields();
                        //Field field1 = null;
                        List<Pair> pairs = new ArrayList<>();
                        for (ObjectField objectField : filtersToApply) {
                            String sign = null;
                            if(objectField.getName().equals("gt"))
                                sign = ">";
                            if(objectField.getName().equals("gte"))
                                sign = ">=";
                            if(objectField.getName().equals("lt"))
                                sign = "<";
                            if(objectField.getName().equals("lte"))
                                sign = "=<";
                            if(sign!=null)
                                pairs.add(Pair.of(sign, ((IntValue)objectField.getValue()).getValue().intValue()));
                        }

                        LOGGER.debug("Applying {} filter with values {}", propertyName, fields.toArray());
                        if(pairs.size()>0)
                            query.put(propertyURI, pairs.get(0));

                    }else if(targetInputType instanceof GraphQLObjectType) {

                        //treating id as scalar(not a relationship) and not requesting the reference object in case we have only id
                        if(argValue instanceof Map
                                && ((Map)argValue).size()==1
                                && ((Map)argValue).containsKey("id")
                        ){
                            Object value = (argValue instanceof Map? ((Map)argValue).get("id"): argValue);
                            query.put(propertyURI, (value instanceof String? "\""+value.toString()+"\"": value));
                            continue;
                        }

                        List<Field> fieldsForNewEnvironment = new ArrayList<>();
                        List<ObjectField> argumentValueObjectFields = ((ObjectValue) argument.getValue()).getObjectFields();
                        Field argumentValueObjectField = null;
                        for (ObjectField objectField : argumentValueObjectFields) {
                            List<Argument> arguments1 = new ArrayList<>();
                            arguments1.add(new Argument(objectField.getName(), objectField.getValue()));
                            String name = targetInputTypeName.toLowerCase()+"s";
                            //String name = objectField.getName();
                            argumentValueObjectField = new Field(name, arguments1);
                            fieldsForNewEnvironment.add(argumentValueObjectField);
                        }




                        LOGGER.debug("Preparing {} fetcher to resolve {}", targetInputType.getName(), wrappedTypeName+"."+argName);

                        GraphQLList typeForNewEnvironment = new GraphQLList(targetInputType);
                        //GraphQLObjectType graphQLObjectType = (GraphQLObjectType) environment.getGraphQLSchema().getType(targetInputTypeName);
                        //GraphQLObjectType graphQLObjectType = (GraphQLObjectType) environment.getGraphQLSchema().getType(targetInputTypeName.toLowerCase()+"s");
                        //GraphQLFieldDefinition fieldDefinitionForEnvironment = graphQLObjectType.getFieldDefinition(targetInputTypeName.toLowerCase()+"s");


//                        String fieldDefinitionName = targetInputTypeName.substring(0,1).toLowerCase()+targetInputTypeName.substring(1)+"s";
//                        if(targetInputTypeName.equals("IoTStream"))
//                            fieldDefinitionName = "streams";
//
//                        if(targetInputTypeName.equals("ObservableProperty"))
//                            fieldDefinitionName = "observableProperties";
                        List<GraphQLFieldDefinition> fieldDefinitionList = environment.getGraphQLSchema().getQueryType().getFieldDefinitions().stream().filter(def-> def.getType() instanceof GraphQLList && ((GraphQLList) def.getType()).getWrappedType().getName().equals(targetInputTypeName)).collect(Collectors.toList());
                        if(fieldDefinitionList.isEmpty())
                            throw new Exception("No field definition for "+targetInputTypeName);

                        GraphQLFieldDefinition fieldDefinitionForEnvironment = environment.getGraphQLSchema().getQueryType().getFieldDefinition(fieldDefinitionList.get(0).getName());

                        String fetcherName = targetInputType.getName();
                        DataFetcher dataFetcher = RecursiveDataFetcher.get(fetcherName);

                        ExecutionTypeInfo fieldTypeInfo = ExecutionTypeInfo.newTypeInfo()
                                .field(argumentValueObjectField)
                                .fieldDefinition(fieldDefinitionForEnvironment)
                                .type(typeForNewEnvironment)
                                .build();

                        DataFetchingEnvironment environment2 = new DataFetchingEnvironmentImpl(
                                environment.getSource(),
                                (Map) argValue,
                                environment.getContext(),
                                environment.getRoot(),

                                fieldDefinitionForEnvironment,
                                //environment.getFieldDefinition(),
                                fieldsForNewEnvironment,
                                //environment.getFields(),
                                //environment.getFieldType(),
                                typeForNewEnvironment,

                                environment.getParentType(),
                                environment.getGraphQLSchema(),
                                environment.getFragmentsByName(),
                                environment.getExecutionId(),
                                environment.getSelectionSet(),
                                //environment.getFieldTypeInfo(),
                                fieldTypeInfo,
                                environment.getExecutionContext()
                        );

                        LOGGER.debug("Executing {} fetcher with args {}", fetcherName, argValue.toString());
                        CompletableFuture future = (CompletableFuture) dataFetcher.get(environment2);
                        try {
                            Object entities = future.get();
                            if (entities instanceof Iterable) {

//                                if (!((Iterable) entities).iterator().hasNext())
//                                    return null;

                                List<String> entityIds = (List)((ArrayList) entities).stream().filter(entity -> entity!=null).map(entity -> ((EntityLD) entity).getId()).collect(Collectors.toList());
                                int size = ((List) entityIds).size();
                                //if (size == 0)
//                                    return null;

                                LOGGER.debug("{} fetcher executed. {} entities returned", fetcherName, size);
                                //String[] sorted = entityIds.toArray(new String[0]);
                                Collections.sort(entityIds);
                                query.put(propertyURI, entityIds);
                            } else {
                                LOGGER.debug("{} fetcher executed. One entity returned", fetcherName);
                                query.put(propertyURI, ((EntityLD) entities).getId());
                            }
                            //                                query.remove(key);
                        } catch (Exception e) {
                            LOGGER.error("Failed to execute {} fetcher: {}", fetcherName, e.getLocalizedMessage());
                            e.printStackTrace();
                            //return null;
                        }
                    }else if(targetInputType instanceof GraphQLScalarType){
                        query.put(propertyURI, (argValue instanceof String? "\""+argValue.toString()+"\"": argValue));
                    }else
                        throw new NotImplementedException(targetInputType+" not resolvable");
                }

        return query;
    }

    public static Map<String, List<String>> resolveBottomUpType(String[] concepts){
        Map<String, List<String>> ret = new HashMap<>();
        for(String concept: concepts){

            Map<String, String> typesToTry = new LinkedHashMap<>();

            typesToTry.put(concept, null);
            List<String> triedTypes = new ArrayList<>();
            while (typesToTry.keySet().size() > 0) {
                String typeToTry = typesToTry.keySet().iterator().next();
                String altType = typesToTry.get(typeToTry);

                triedTypes.add(typeToTry);

                String altType2 = (altType!=null?altType:typeToTry);
                if (CORE_TYPES.contains(typeToTry)) {
                    appendMapToList(ret, typeToTry, altType);
                }else if (HierarchicalWiring.getBottomUpHierarchy().containsKey(typeToTry)) { //adding more generic type
                    HierarchicalWiring.getBottomUpHierarchy().get(typeToTry).forEach(t2 -> {
                        if (!triedTypes.contains(t2)) {
                            if(CORE_TYPES.contains(t2)) {
                                appendMapToList(ret, t2, altType2);
//                                List<String> conditions = (ret.containsKey(t2)?ret.get(t2):new ArrayList<>());
//                                if(!conditions.contains(altType2)){
//                                    conditions.add(altType2);
//                                }
//                                ret.put(t2, conditions);
                            }else {
                                Map<String, List<String>> ret2 = resolveBottomUpType(new String[]{t2});
                                for(String resolvedType: ret2.keySet()) {
                                    //appending propagatedType
                                    appendMapToList(ret, resolvedType, altType2);
//                                    for (String condition : ret2.get(resolvedType))
//                                        appendMapToList(ret, resolvedType, altType2);
                                }

                            }

                        }
                    });
                }//else
                // throw new NotImplementedException("Unsupported type which cannot be resolved to any");

                //triedTypes.add(typeToTry);
                typesToTry.remove(typeToTry);
            }
        }
        return ret;
    }

    private static void appendMapToList(Map<String, List<String>> map, String key, String value){
        List<String> list = (map.containsKey(key)?map.get(key):new ArrayList<>());
        if(!list.contains(value)){
            list.add(value);
        }
        map.put(key, list);
    }
}
