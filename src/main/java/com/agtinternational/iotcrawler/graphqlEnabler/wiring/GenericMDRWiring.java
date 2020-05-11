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

import com.agtinternational.iotcrawler.graphqlEnabler.Context;
import graphql.execution.ExecutionTypeInfo;
import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.*;
import org.apache.jena.vocabulary.RDFS;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.core.Constants.CUT_TYPE_URIS;
import static com.agtinternational.iotcrawler.core.Constants.IOTCRAWLER_ORCHESTRATOR_URL;
import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.ALT_TYPE;


@Component
public class GenericMDRWiring implements Wiring {

    static Logger LOGGER = LoggerFactory.getLogger(GenericMDRWiring.class);
    public static final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
    static IoTCrawlerClient iotCrawlerClient;
    private Map<String, String> schemas;
    private RuntimeWiring.Builder runtimeWiringBuilder;

    private static Map<String, String> bindingRegistry = new HashMap<>();
    private static Map<String, List<String>> topDownInheritance = new HashMap<>();
    private static Map<String, List<String>> bottomUpHierarchy = new HashMap<>();

    public GenericMDRWiring(){
    }

    public void setSchemaString(Map<String, String> schemas) {
        this.schemas = schemas;
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

    private static List<Object> serveQuery(String typeURI, Map<String, Object> query, int offset, int limit){
        List ret = new ArrayList();

        if (query == null || query.size() == 0)
            try {
                List res = getIoTCrawlerClient().getEntities(typeURI, null, null, offset, limit);
                return res;
            } catch (Exception e) {
                LOGGER.error("Failed to get entities of type {}", typeURI);
                e.printStackTrace();
                return null;
            }
        else
            for (String key : query.keySet()) {
                Object value = query.get(key);
                if (!(value instanceof Iterable))
                    value = Arrays.asList(new Object[]{value});

                Iterator iterator = ((Iterable) value).iterator();
                while (iterator.hasNext()) {
                    Map query2 = new HashMap();
                    query2.put(key, iterator.next());
                    try {
                        List<EntityLD> res = getIoTCrawlerClient().getEntities(typeURI, query2, null, offset, limit);
                        ret.addAll(res);
                    } catch (Exception e) {
                        LOGGER.error("Failed to get entities of type {}", typeURI);
                        e.printStackTrace();
                    }
                }
            }


        //List augmented = augmentEntities(ret, concept);
        //return augmented;
        return ret;
    }

    private static List<Object> getConceptsByIds(List<String> keys, String concept){
        List enitities = new ArrayList();
        String typeURI=null;
        try {
            typeURI = findURI(concept);
        }
        catch (Exception e){
            LOGGER.error("Failed to find URI for {}: {}", concept, e.getLocalizedMessage());
            return enitities;
        }
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


    private static Map resolveFilters(Map<String, Object> query, DataFetchingEnvironment environment, Map<String, Object> arguments){

        if(environment.getArgument("altType")!=null) {
            String altTypeName = environment.getArgument("altType");
            try {
                String typeURI = findURI(altTypeName);
                query.put(ALT_TYPE, "\"" + typeURI + "\"");
            } catch (Exception e) {
                LOGGER.warn("Failed to find URI for {}", altTypeName);
            }
        }
        //for(String argName: arguments.keySet()) {
        for(Field field: environment.getFields())
            for(Argument argument: field.getArguments()){

                String argName = argument.getName();


                Object argValue = arguments.get(argName);
                GraphQLType wrappedType = ((GraphQLModifiedType)environment.getFieldType()).getWrappedType();
                String typeName = wrappedType.getName();

                String propertyURI = null;
                try {
                    propertyURI = findURI(typeName, argName);
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



                    GraphQLList graphQLType = new GraphQLList(targetType);
                    GraphQLObjectType graphQLObjectType = (GraphQLObjectType) environment.getGraphQLSchema().getType(inputTypeName);
                    GraphQLFieldDefinition fieldDefinition = graphQLObjectType.getFieldDefinition(field1.getName());

                    LOGGER.debug("Preparing {} fetcher to resolve {}", targetType.getName(), typeName+"."+argName);

                    DataFetcher dataFetcher = genericDataFetcher(targetType.getName(), true, new ArrayList<>());

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

                    LOGGER.debug("Executing {} fetcher with args {}", targetType.getName(), argValue.toString());
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

                            LOGGER.debug("{} fetcher executed. {} entities returned", targetType.getName(), size);
                            query.put(propertyURI, entityIds);
                        } else {
                            LOGGER.debug("{} fetcher executed. One entity returned", targetType.getName());
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
    public static DataFetcher genericDataFetcher(String concept, boolean resolvingInput, List<String> resolvedConcepts) {

        return environment -> {
            Boolean topLevelQuery = (resolvedConcepts.size()==0?true: false);
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
                     List<Object> entities = getConceptsByIds(Arrays.asList(id), concept);
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

            Map<String, Object> argumentsToResolve = environment.getArguments();
            if(argumentsToResolve.containsKey("offset")){
                offset = environment.getArgument("offset");
                argumentsToResolve.remove("offset");
            }
            if(environment.getArgument("limit")!=null) {
                limit = environment.getArgument("limit");
                argumentsToResolve.remove("limit");
            }

            if(argumentsToResolve.size()>0){
                try {
                    query = resolveFilters(query, environment, argumentsToResolve);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            //environment.getGraphQLSchema().getType(concept)

            String typeURI = null;
            try {
                typeURI = findURI(concept);
            } catch (Exception e) {
                LOGGER.error("Failed to find URI for {}: {}", concept, e.getLocalizedMessage());
            }


            List entities = new ArrayList(serveQuery(typeURI, query, offset, limit));

            resolvedConcepts.add(concept);
            //Include all entities from child (TemperatureSenror) and to parent (SSNSystem) classes
            Map<String, Map<String, Object>> adjacentConcepts = new LinkedHashMap<>();

            //include all parentClasses with filtering by alt type(e.g. TemperatureSensor into Sensor)
            if(bottomUpHierarchy.containsKey(concept)) {
                Map extraQuery = new HashMap();
                //extraQuery.put("altType", "\""+typeURI+"\"");
                extraQuery.put("altType", concept);
                for (String parentClass : bottomUpHierarchy.get(concept))
                    if(!resolvedConcepts.contains(parentClass))
                        adjacentConcepts.put(parentClass, extraQuery);
            }
            //include all entities of a subclasses without any filtering (e.g. Sensor into SsnSystem)
            if(topDownInheritance.containsKey(concept))
                for (String childClass : topDownInheritance.get(concept))
                    if(!resolvedConcepts.contains(childClass))
                        adjacentConcepts.put(childClass, null);

            for(String adjacentConcept: adjacentConcepts.keySet()){
                Map adjacentTypeArguments = new HashMap(query);
                //removing already resolved
                if(adjacentTypeArguments.containsKey(ALT_TYPE))
                    adjacentTypeArguments.remove(ALT_TYPE);

                if(adjacentConcepts.get(adjacentConcept)!=null)
                    adjacentTypeArguments.putAll(adjacentConcepts.get(adjacentConcept));


                //List<EntityLD> adjacentEntities = serveQuery(typeURI, extendedQuery, offset, limit);

                DataFetcher dataFetcher = genericDataFetcher(adjacentConcept,false, resolvedConcepts);
                DataFetchingEnvironment environment2 = new DataFetchingEnvironmentImpl(
                        environment.getSource(),
                        (Map) adjacentTypeArguments,
                        environment.getContext(),
                        environment.getRoot(),

                        //fieldDefinition,
                        environment.getFieldDefinition(),
                        //fields,
                        environment.getFields(),
                        environment.getFieldType(),
                        //graphQLType,

                        environment.getParentType(),
                        environment.getGraphQLSchema(),
                        environment.getFragmentsByName(),
                        environment.getExecutionId(),
                        environment.getSelectionSet(),
                        environment.getFieldTypeInfo(),
                        //fieldTypeInfo,
                        environment.getExecutionContext()
                );
                CompletableFuture future = (CompletableFuture) dataFetcher.get(environment2);
                try {
                    LOGGER.debug("Executing adjacent {} fetcher with args {}", adjacentConcept, adjacentTypeArguments.toString());
                    List<EntityLD> adjacentEntities = (List<EntityLD>)future.get();
                    entities.addAll(adjacentEntities);
                }
                catch (Exception e){
                    LOGGER.error("Failed to execute adjacent fetcher for {}: {}", adjacentConcept, e.getLocalizedMessage());
                }
            }

            List<String> ids = new ArrayList<>();
            entities.stream().forEach(entity0->{
                EntityLD entity = ((EntityLD)entity0);
                loader.prime(entity.getId(), entity);
                ids.add(entity.getId());
            });

            if(topLevelQuery)
                resolvedConcepts.clear();

            return loader.loadMany(ids);

        };
    }


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




    public static String findURI(String type) throws Exception {
        if(bindingRegistry.containsKey(type))
            return bindingRegistry.get(type);
        throw new Exception("Type "+type+" not found in binding registry");

    }

    public static String findURI(String type, String property) throws Exception {
        if(bindingRegistry.containsKey(type+"."+property))
            return bindingRegistry.get(type+"."+property);
        throw new Exception("Type "+type+" not found in binding registry");
    }


    @Override
    public String getSchemaString() {
        return null;
    }

    //@Override
    public Map<String, String> getSchemas(){
        return schemas;
    }

    @Override
    public RuntimeWiring build() {
        CustomWiringFactory customWiringFactory = new CustomWiringFactory();
        runtimeWiringBuilder.wiringFactory(customWiringFactory);
        //runtimeWiringBuilder.directive("class", new DirectivesWiring());
        //runtimeWiringBuilder.directive("attribute", new DirectivesWiring());
        RuntimeWiring runtimeWiring = runtimeWiringBuilder.build();
        customWiringFactory.setRuntimeWiring(runtimeWiring);

        return runtimeWiring;
    }

    public void setInheritanceRegistry(Map<String, List<String>> topDownInheritance) {
        this.topDownInheritance = topDownInheritance;
    }

    public void setBottomUpHierarchy(Map<String, List<String>> bottomUpHierarchy) {
        this.bottomUpHierarchy = bottomUpHierarchy;
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
                    getConceptsByIds(list, concept));
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
