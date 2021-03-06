package com.agtinternational.iotcrawler.graphqlEnabler.resolution;//package com.agtinternational.iotcrawler.graphqlEnabler.resolving;

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
//
import com.agtinternational.iotcrawler.core.ontologies.NGSI_LD;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.HierarchicalWiring;
import graphql.execution.ExecutionTypeInfo;
import graphql.language.*;
import graphql.schema.*;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TopDownStrategy {

    static Logger LOGGER = LoggerFactory.getLogger(TopDownStrategy.class);

    public static List<String> getTopdownTypesAsList(String concept){
        List<String> ret = new ArrayList<>();
        List<String> typesToTry  = new ArrayList();
        typesToTry.add(concept);
        List<String> triedTypes = new ArrayList<>();
        while (typesToTry.size()>0) {
            String typeToTry = typesToTry.iterator().next();

            //if(coreTypes.contains(typeToTry) && !ret.contains(typeToTry))
            if(!typeToTry.equals(concept))
                if(!ret.contains(typeToTry))
                    ret.add(typeToTry);

            if (HierarchicalWiring.getTopDownInheritance().containsKey(typeToTry))  //adding sensors/actuators/samples
                HierarchicalWiring.getTopDownInheritance().get(typeToTry).forEach(t2 -> {
                    if (!triedTypes.contains(t2))
                        ret.add(t2);
                    //typesToTry.add(t2);
                });
            triedTypes.add(typeToTry);
            typesToTry.remove(typeToTry);
        }
        return ret;
    }

//    public static Map resolveQuery(DataFetchingEnvironment environment, Map<String, Object> argumentsToResolve) throws Exception {
//        Map<String, Object> query = new HashMap<>();
//        LOGGER.debug("Amending query by resolving the filters "+argumentsToResolve.toString());
//        //setting alternative type as a condition
//        if(environment.getArgument("subClassOf")!=null) {
//            String parentTypeName = environment.getArgument("subClassOf");
//            try {
//                String parentTypeURI = HierarchicalWiring.findURI(parentTypeName);
//                query.put(NGSI_LD.alternativeType, "\"" + parentTypeURI + "\"");
//            } catch (Exception e) {
//                LOGGER.warn("Failed to find URI for {}", parentTypeName);
//                return null;
//            }
//        }
//        //for(String argName: arguments.keySet()) {
//        for(Field field: environment.getFields())
//            for(Argument argument: field.getArguments())
//                if(argumentsToResolve.containsKey(argument.getName())){
//
//                    String argName = argument.getName();
//
//                    Object argValue = argumentsToResolve.get(argName);
//                    GraphQLOutputType outputType = environment.getFieldType();
//                    //GraphQLType wrappedType = ((GraphQLModifiedType)outputType).getWrappedType();
//                    String wrappedTypeName = ((GraphQLModifiedType)outputType).getWrappedType().getName();
//
//                    String propertyURI = null;
//                    try {
//                        propertyURI = HierarchicalWiring.findURI(wrappedTypeName, argName);
//                    }
//                    catch (Exception e){
//                        //ignoring exception
//                    }
////                if(wrappedType instanceof GraphQLObjectType)
////                    propertyURI = ((GraphQLObjectType)wrappedType).getDescription();
////                else throw new NotImplementedException(wrappedType.getClass().getCanonicalName()+" not implemented");
//
//                    if(propertyURI==null) {
//                        LOGGER.warn("URI not found for " + argName+". Excluding from resolution");
//                        continue;
//                    }
//
//                    GraphQLArgument graphQLArgument = environment.getFieldTypeInfo().getFieldDefinition().getArgument(argName);
//                    GraphQLType graphQLInputType = environment.getFieldTypeInfo().getFieldDefinition().getType();
//                    if(graphQLArgument!=null)//getting type from argument (if possible)
//                        graphQLInputType = graphQLArgument.getType();
//                    else
//                        throw new Exception(argName+" not found in arguments of field definition of the environment!");
//
//                    if(graphQLInputType instanceof GraphQLModifiedType)
//                        graphQLInputType = ((GraphQLModifiedType) graphQLInputType).getWrappedType();
//
//                    // (!) Hardcoded stuff requiring Sensor & SensorInput types in schema
//                    // Get target type out of input type (to resolve it with a separate fetcher)
//                    String targetInputTypeName = graphQLInputType.getName().replace("Input","");
//                    //String inputTypeName = graphQLInputType.getName();
//
//                    GraphQLType targetInputType = environment.getGraphQLSchema().getType(targetInputTypeName);
//                    if(targetInputType==null)
//                        throw new Exception("Type " + targetInputTypeName + " not found in schema");
//
//                    //if target type is just a filterArgument
//                    if(targetInputType instanceof GraphQLInputObjectType){ //handling input type filters
//                        List<Field> fields = new ArrayList<>();
//                        String propertyName = argument.getName();
//                        List<ObjectField> filtersToApply = ((ObjectValue) argument.getValue()).getObjectFields();
//                        //Field field1 = null;
//                        List<Pair> pairs = new ArrayList<>();
//                        for (ObjectField objectField : filtersToApply) {
//                            String sign = null;
//                            if(objectField.getName().equals("gt"))
//                                sign = ">";
//                            if(objectField.getName().equals("gte"))
//                                sign = ">=";
//                            if(objectField.getName().equals("lt"))
//                                sign = "<";
//                            if(objectField.getName().equals("lte"))
//                                sign = "=<";
//                            if(sign!=null)
//                                pairs.add(Pair.of(sign, ((IntValue)objectField.getValue()).getValue().intValue()));
//                        }
//
//                        LOGGER.debug("Applying {} filter with values {}", propertyName, fields.toArray());
//                        if(pairs.size()>0)
//                            query.put(propertyURI, pairs.get(0));
//
//                    }else if(targetInputType instanceof GraphQLObjectType) {
//
//                        List<Field> fieldsForNewEnvironment = new ArrayList<>();
//                        List<ObjectField> argumentValueObjectFields = ((ObjectValue) argument.getValue()).getObjectFields();
//                        Field argumentValueObjectField = null;
//                        for (ObjectField objectField : argumentValueObjectFields) {
//                            List<Argument> arguments1 = new ArrayList<>();
//                            arguments1.add(new Argument(objectField.getName(), objectField.getValue()));
//                            String name = targetInputTypeName.toLowerCase()+"s";
//                            //String name = objectField.getName();
//                            argumentValueObjectField = new Field(name, arguments1);
//                            fieldsForNewEnvironment.add(argumentValueObjectField);
//                        }
//
//
//                        //treating id as scalar and not requesting the reference object in case we have only id
//                        if(fieldsForNewEnvironment.size()==1 && fieldsForNewEnvironment.get(0).getName().equals("id")){
//                            Object value = (argValue instanceof Map? ((Map)argValue).get("id"): argValue);
//                            query.put(propertyURI, (value instanceof String? "\""+value.toString()+"\"": value));
//                            continue;
//                        }
//
//                        LOGGER.debug("Preparing {} fetcher to resolve {}", targetInputType.getName(), wrappedTypeName+"."+argName);
//
//                        GraphQLList typeForNewEnvironment = new GraphQLList(targetInputType);
//                        //GraphQLObjectType graphQLObjectType = (GraphQLObjectType) environment.getGraphQLSchema().getType(targetInputTypeName);
//                        //GraphQLObjectType graphQLObjectType = (GraphQLObjectType) environment.getGraphQLSchema().getType(targetInputTypeName.toLowerCase()+"s");
//                        //GraphQLFieldDefinition fieldDefinitionForEnvironment = graphQLObjectType.getFieldDefinition(targetInputTypeName.toLowerCase()+"s");
//
//                        //(!) Another hard code here: get argument definitions of a corresponding type (e.g. sensor): sensors(arg1, arg2,)
//                        String fieldDefinitionName = targetInputTypeName.substring(0,1).toLowerCase()+targetInputTypeName.substring(1)+"s";
//                        GraphQLFieldDefinition fieldDefinitionForEnvironment = environment.getGraphQLSchema().getQueryType().getFieldDefinition(fieldDefinitionName);
//                        if(fieldDefinitionForEnvironment==null)
//                            throw new Exception("No field definition for "+fieldDefinitionName);
//                        DataFetcher dataFetcher = UniversalDataFetcher.get(targetInputType.getName(), true);
//
//                        ExecutionTypeInfo fieldTypeInfo = ExecutionTypeInfo.newTypeInfo()
//                                .field(argumentValueObjectField)
//                                .fieldDefinition(fieldDefinitionForEnvironment)
//                                .type(typeForNewEnvironment)
//                                .build();
//
//                        DataFetchingEnvironment environment2 = new DataFetchingEnvironmentImpl(
//                                environment.getSource(),
//                                (Map) argValue,
//                                environment.getContext(),
//                                environment.getRoot(),
//
//                                fieldDefinitionForEnvironment,
//                                //environment.getFieldDefinition(),
//                                fieldsForNewEnvironment,
//                                //environment.getFields(),
//                                //environment.getFieldType(),
//                                typeForNewEnvironment,
//
//                                environment.getParentType(),
//                                environment.getGraphQLSchema(),
//                                environment.getFragmentsByName(),
//                                environment.getExecutionId(),
//                                environment.getSelectionSet(),
//                                //environment.getFieldTypeInfo(),
//                                fieldTypeInfo,
//                                environment.getExecutionContext()
//                        );
//
//                        LOGGER.debug("Executing {} fetcher with args {}", targetInputType.getName(), argValue.toString());
//                        CompletableFuture future = (CompletableFuture) dataFetcher.get(environment2);
//                        try {
//                            Object entities = future.get();
//                            if (entities instanceof Iterable) {
//
////                                if (!((Iterable) entities).iterator().hasNext())
////                                    return null;
//
//                                Object entityIds = ((ArrayList) entities).stream().map(entity -> ((EntityLD) entity).getId()).collect(Collectors.toList());
//                                int size = ((List) entityIds).size();
//                                //if (size == 0)
////                                    return null;
//
//                                LOGGER.debug("{} fetcher executed. {} entities returned", targetInputType.getName(), size);
//                                query.put(propertyURI, entityIds);
//                            } else {
//                                LOGGER.debug("{} fetcher executed. One entity returned", targetInputType.getName());
//                                query.put(propertyURI, ((EntityLD) entities).getId());
//                            }
//                            //                                query.remove(key);
//                        } catch (Exception e) {
//                            LOGGER.error("Failed to resolve madeBySensor filter");
//                            e.printStackTrace();
//                            //return null;
//                        }
//                    }else if(targetInputType instanceof GraphQLScalarType){
//                        query.put(propertyURI, (argValue instanceof String? "\""+argValue.toString()+"\"": argValue));
//                    }else
//                        throw new NotImplementedException();
//                }
//
//        return query;
//    }

}
