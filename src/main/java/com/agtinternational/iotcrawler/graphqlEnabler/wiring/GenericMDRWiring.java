package com.agtinternational.iotcrawler.graphqlEnabler.wiring;

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


import com.agtinternational.iotcrawler.core.clients.IoTCrawlerRESTClient;
import com.agtinternational.iotcrawler.core.interfaces.IoTCrawlerClient;
import com.agtinternational.iotcrawler.core.models.*;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.*;

import graphql.execution.ExecutionTypeInfo;
import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.*;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.apache.commons.lang.NotImplementedException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.core.Constants.CUT_TYPE_URIS;
import static com.agtinternational.iotcrawler.core.Constants.IOTCRAWLER_ORCHESTRATOR_URL;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;


@Component
public class GenericMDRWiring implements Wiring {

    static Logger LOGGER = LoggerFactory.getLogger(GenericMDRWiring.class);
    public static final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
    static IoTCrawlerClient iotCrawlerClient;
    private String schemaString;
    private RuntimeWiring.Builder runtimeWiringBuilder;

    public static Map<String, String> bindingRegistry = new HashMap<>();

    public GenericMDRWiring(){
//        this.schemaString = schemaString;
//        this.runtimeWiringBuilder = runtimeWiringBuilder;
//        GenericMDRWiring.bindingRegistry = bindingRegistry;
//
//        for(String concept: dataLoaderConcepts)
//            dataLoaderRegistry.register(concept, new DataLoader(new GenericMDRWiring.GenericLoader(concept)));

    }

    public void setSchemaString(String schemaString) {
        this.schemaString = schemaString;
    }

    public void setRuntimeWiringBuilder(RuntimeWiring.Builder runtimeWiringBuilder) {
        this.runtimeWiringBuilder = runtimeWiringBuilder;
    }

    public void setBindingRegistry(Map<String, String> bindingRegistry) {
        GenericMDRWiring.bindingRegistry = bindingRegistry;
    }

    public void registerDataloaderConcept(String concept){
        dataLoaderRegistry.register(concept, new DataLoader(new GenericLoader(concept)));
    }

    public static IoTCrawlerClient getIoTCrawlerClient(){
        Boolean cutURIs = (System.getenv().containsKey(CUT_TYPE_URIS)?Boolean.parseBoolean(System.getenv(CUT_TYPE_URIS)):false);
        if(iotCrawlerClient==null) {
            iotCrawlerClient = new IoTCrawlerRESTClient(System.getenv(IOTCRAWLER_ORCHESTRATOR_URL), cutURIs);
            //iotCrawlerClient = new OrchestratorRESTClient();
            try {
                iotCrawlerClient.init();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return iotCrawlerClient;
    }

    @Bean
    public DataLoaderRegistry getDataLoaderRegistry() {
        return dataLoaderRegistry;
    }

    private static List<Object> getEntitiesViaHTTP(Map<String, Object> query, int offset, int limit, String concept){
        List ret = new ArrayList();
        String typeURI = bindingRegistry.get(concept);
        if(query.size()==0)
            try {
                List res = getIoTCrawlerClient().getEntities(typeURI, null, null, offset, limit);
                return res;
            } catch (Exception e) {
                LOGGER.error("Failed to get {} entities", concept);
                e.printStackTrace();
                return null;
            }

        for(String key: query.keySet()){
            Object value = query.get(key);
            if(!(value instanceof Iterable))
                value = Arrays.asList(new Object[]{ value });

            Iterator iterator = ((Iterable) value).iterator();
            while (iterator.hasNext()) {
                Map query2 = new HashMap();
                query2.put(key, iterator.next());
                try {
                    List<EntityLD> res = getIoTCrawlerClient().getEntities(typeURI, query2, null, offset, limit);
                    ret.addAll(res);
                } catch (Exception e) {
                    LOGGER.error("Failed to get {} entities", concept);
                    e.printStackTrace();
                }
            }
        }

        //List augmented = augmentEntities(ret, concept);
        //return augmented;
        return ret;
    }

    private static List<Object> getEntitiesViaHTTP(List<String> keys, String concept){
        List enitities = new ArrayList();
        String typeURI = bindingRegistry.get(concept);
        int count=0;
        for(String key : keys) {
            try {
                List<EntityLD> entities = getIoTCrawlerClient().getEntityById(key);
                enitities.addAll(entities);
            } catch (Exception e) {
                LOGGER.error("Failed to get entity {}", key, concept);
                //e.printStackTrace();
            }
            count++;
        }

        if(keys.size()!=enitities.size()) {
            int delta = keys.size() - enitities.size();
            for (int i = 0; i < delta; i++) {
                enitities.add(null);   //filling missing results
                LOGGER.warn("Failed to return exact amount of entnties({}). Adding null entity to the result", concept);
            }
        }
        return enitities;
    }


    private static List serveQuery(Map<String, Object> query, String concept, int offset, int limit){
        List entities = getEntitiesViaHTTP(query, offset,limit, concept);
        return entities;
    }

    private static Map resolveInput(Map<String, Object> query, DataFetchingEnvironment environment, Map<String, Object> arguments) throws Exception {

        //for(String argName: arguments.keySet()) {
        for(Field field: environment.getFields())
            for(Argument argument: field.getArguments()){

                String argName = argument.getName();


                Object argValue = arguments.get(argName);
                GraphQLType wrappedType = ((GraphQLModifiedType)environment.getFieldType()).getWrappedType();
                String typeName = wrappedType.getName();

                String propertyURI = findURI(typeName, argName);
//                if(wrappedType instanceof GraphQLObjectType)
//                    propertyURI = ((GraphQLObjectType)wrappedType).getDescription();
//                else throw new NotImplementedException(wrappedType.getClass().getCanonicalName()+" not implemented");

                if(propertyURI==null) {
                    LOGGER.warn("URI not found for " + argName+". Excluding from resolution");
                    continue;
                }

                GraphQLArgument graphQLArgument = environment.getFieldTypeInfo().getFieldDefinition().getArgument(argName);
                GraphQLType graphQLInputType = environment.getFieldTypeInfo().getFieldDefinition().getType();
                if(graphQLArgument!=null)//getting from argument if possible
                    graphQLInputType = graphQLArgument.getType();


                if(graphQLInputType instanceof GraphQLModifiedType)
                    graphQLInputType = ((GraphQLModifiedType) graphQLInputType).getWrappedType();
                String inputTypeName = graphQLInputType.getName().replace("Input","");

                GraphQLType targetType = environment.getGraphQLSchema().getType(inputTypeName);

                if(targetType instanceof GraphQLObjectType) {

                    List<Field> fields = new ArrayList<>();
                    List<ObjectField> objectFields = ((ObjectValue) argument.getValue()).getObjectFields();
                    Field field1 = null;
                    for (ObjectField objectField : objectFields) {
                        List<Argument> arguments1 = new ArrayList<>();
                        arguments1.add(new Argument(objectField.getName(), objectField.getValue()));
                        field1 = new Field(objectField.getName(), arguments1);
                        fields.add(field1);
                    }

                    //treating id as scalar(not requesting the reference object)
                    if(fields.size()==1 && fields.get(0).getName().equals("id")){
                        Object value = (argValue instanceof Map? ((Map)argValue).get("id"): argValue);
                        query.put(propertyURI, (value instanceof String? "\""+value.toString()+"\"": value));
                        continue;
                    }

                    LOGGER.info("Initializing {} fetcher to resolve {}", targetType.getName(), typeName+"."+argName);
                    DataFetcher dataFetcher = genericDataFetcher(targetType.getName(), true);

                    GraphQLList graphQLType = new GraphQLList(targetType);
                    GraphQLObjectType graphQLObjectType = (GraphQLObjectType) environment.getGraphQLSchema().getType(inputTypeName);
                    GraphQLFieldDefinition fieldDefinition = graphQLObjectType.getFieldDefinition(field1.getName());

                    ExecutionTypeInfo fieldTypeInfo = ExecutionTypeInfo.newTypeInfo()
                            .field(field1)
                            .fieldDefinition(fieldDefinition)
                            .type(graphQLType)
                            .build();

                    DataFetchingEnvironment environment2 = new DataFetchingEnvironmentImpl(
                            environment.getSource(),
                            (Map) argValue,
                            environment.getContext(),
                            environment.getRoot(),

                            fieldDefinition,
                            //environment.getFieldDefinition(),
                            fields,
                            //environment.getFields(),
                            //environment.getFieldType(),
                            graphQLType,

                            environment.getParentType(),
                            environment.getGraphQLSchema(),
                            environment.getFragmentsByName(),
                            environment.getExecutionId(),
                            environment.getSelectionSet(),
                            //environment.getFieldTypeInfo(),
                            fieldTypeInfo,
                            environment.getExecutionContext()
                    );

                    LOGGER.info("Executing {} fetcher", targetType.getName());
                    CompletableFuture future = (CompletableFuture) dataFetcher.get(environment2);
                    try {
                        Object entities = future.get();
                        if (entities instanceof Iterable) {

                            if (!((Iterable) entities).iterator().hasNext())
                                return null;

                            Object entityIds = ((ArrayList) entities).stream().map(entity -> ((EntityLD) entity).getId()).collect(Collectors.toList());
                            int size = ((List) entityIds).size();
                            if (size == 0)
                                return null;

                            LOGGER.info("{} fetcher executed. {} entities returned", targetType.getName(), size);
                            query.put(propertyURI, entityIds);
                        } else {
                            LOGGER.info("{} fetcher executed. One entity returned", targetType.getName());
                            query.put(propertyURI, ((EntityLD) entities).getId());
                        }
                        //                                query.remove(key);
                    } catch (Exception e) {
                        LOGGER.error("Failed to resolve madeBySensor filter");
                        e.printStackTrace();
                        return null;
                    }
                }else if(targetType instanceof GraphQLScalarType){
                    query.put(propertyURI, (argValue instanceof String? "\""+argValue.toString()+"\"": argValue));
                }else
                    throw new NotImplementedException();

        }

        return query;
    }

    //public static DataFetcher genericDataFetcher(Class targetClass, boolean resolvingInput) {
    public static DataFetcher genericDataFetcher(String concept, boolean resolvingInput) {

        return environment -> {
            Context ctx = environment.getContext();
            DataLoader<String, Object> loader = ctx.getLoader(concept);
            if(loader==null) {
                LOGGER.error("Loader for " + concept + " not found");
                return null;
            }
            String id = environment.getArgument("id");
            String URI = environment.getArgument("URI");
            Object source = environment.getSource();


                if(id!=null) {
                 if (resolvingInput){
                     List<Object> entities = getEntitiesViaHTTP(Arrays.asList(id), concept);
                     List<String> ids = new ArrayList<>();
                     entities.stream().forEach(entity0 -> {
                         EntityLD entity = ((EntityLD) entity0);
                         if(entity!=null) {
                             loader.prime(entity.getId(), entity);
                             ids.add(entity.getId());
                         }
                     });
                     //return loader.loadMany(ids);
                 }
                 return loader.load(id);
             }
//            if(URI!=null){
//                //Filtering query shoud
//                return loader.load(URI);
//            }

            Map<String,Object> query = new HashMap<>();

            int offset = 0;
            int limit = 0;
            if(environment.getArguments().size()>0){
                Map<String, Object> arguments = environment.getArguments();
                try {
                    query = resolveInput(query, environment, arguments);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(environment.getArgument("offset")!=null)
                    offset = environment.getArgument("offset");
                if(environment.getArgument("limit")!=null)
                    limit = environment.getArgument("limit");
            }


            List entities = serveQuery(query, concept, offset, limit);
            List<String> ids = new ArrayList<>();
            entities.stream().forEach(entity0->{
                EntityLD entity = ((EntityLD)entity0);
                loader.prime(entity.getId(), entity);
                ids.add(entity.getId());
            });

//            if(resolvingInput)
//                return entities;

            return loader.loadMany(ids);

        };
    }


//    public static DataFetcher entitiesDataFetcher() {
//        return environment -> {
//            String type = environment.getArgument("type");
//            String query = environment.getArgument("query");
//            int offset = environment.getArgument("offset");
//            int limit = environment.getArgument("limit");
//            Object ret=null;
//
//            //JsonObject jsonObject = (JsonObject) jsonParser.parse(query);
//
//            try {
//                ret = getIoTCrawlerClient().getEntities(type, query, null, offset, limit);
//            } catch (Exception e) {
//                LOGGER.error("Failed to get entities");
//                e.printStackTrace();
//            }
////            Context ctx = environment.getContext();
////            DataLoader<String, Object> loader = ctx.getLoader("sensors");
////            CompletableFuture ret = loader.load(id);
//            return ret;
//        };
//    }





    TypeResolver typesResolver = environment -> {
        Object object = environment.getObject();

        GraphQLObjectType ret = null;
        if (object instanceof IoTStream) {
            ret = (GraphQLObjectType) environment.getSchema().getType("IoTStream");
        } else if(object instanceof Sensor) {
            ret = (GraphQLObjectType) environment.getSchema().getType("Sensor");
        }else
            throw new NotImplementedException();
        return ret;
    };


    @Override
    public String getSchemaString(){
        return schemaString;
    }

    public static String findURI(String type){
        if(bindingRegistry.containsKey(type))
            return bindingRegistry.get(type);
        LOGGER.warn("Type {} not found in binding registry", type);
        return null;
    }

    public static String findURI(String type, String property){
        if(bindingRegistry.containsKey(type+"."+property))
            return bindingRegistry.get(type+"."+property);
        LOGGER.warn("Type {} not found in binding registry", type+"."+property);
        return null;
    }

    @Override
    public RuntimeWiring build() {
        CustomWiringFactory customWiringFactory = new CustomWiringFactory();
        runtimeWiringBuilder.wiringFactory(customWiringFactory);
        RuntimeWiring runtimeWiring = runtimeWiringBuilder.build();
        customWiringFactory.setRuntimeWiring(runtimeWiring);

        return runtimeWiring;
    }

    public static class GenericLoader implements org.dataloader.BatchLoader {

        String concept;
        public GenericLoader(String concept){
            this.concept = concept;
        }

        @Override
        public CompletionStage<List> load(List list) {
            String test = "123";
            return CompletableFuture.supplyAsync(() ->
                    getEntitiesViaHTTP(list, concept));
        }
    }


    public static class EnvironmentsBuilder{
        public static DataFetchingEnvironment create(DataFetchingEnvironment environment, Map<String, Object> arguments){
           return new DataFetchingEnvironmentImpl(
                    environment.getSource(),
                    arguments,
                    environment.getContext(),
                    environment.getRoot(),
                    environment.getFieldDefinition(),
                    environment.getFields(),
                    environment.getFieldType(),
                    environment.getParentType(),
                    environment.getGraphQLSchema(),
                    environment.getFragmentsByName(),
                    environment.getExecutionId(),
                    environment.getSelectionSet(),
                    environment.getFieldTypeInfo(),
                    environment.getExecutionContext()
            );
        }
    }
}
