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
import com.agtinternational.iotcrawler.core.ontologies.IotStream;
import com.agtinternational.iotcrawler.core.ontologies.NGSI_LD;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.*;

import com.agtinternational.iotcrawler.graphqlEnabler.Context;
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
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.core.Constants.CUT_TYPE_URIS;
import static com.agtinternational.iotcrawler.core.Constants.IOTCRAWLER_ORCHESTRATOR_URL;
import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.GRAPHQL_ENDPOINT_URL;


@Component
public class GenericMDRWiring implements Wiring {

    static Logger LOGGER = LoggerFactory.getLogger(GenericMDRWiring.class);
    public static final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
    static IoTCrawlerClient iotCrawlerClient;
    static long totalQueryExectionTime = 0;
    static long totalQueriesPerformed = 0;
    private Map<String, String> schemas;
    private RuntimeWiring.Builder runtimeWiringBuilder;
    private static ExecutorService executorService;

    private static Map<String, String> bindingRegistry = new HashMap<>();
    private static Map<String, List<String>> topDownInheritance = new HashMap<>();
    private static Map<String, List<String>> bottomUpHierarchy = new HashMap<>();
    private static List<String> coreTypes = Arrays.asList(new String[]{ "IoTStream", "Sensor", "Platform", "ObservableProperty", "StreamObservation" });

    public GenericMDRWiring(){
         executorService = Executors.newFixedThreadPool(64);
    }

//    public void setCoreTypes(List<String> coreTypes) {
//        this.coreTypes = coreTypes;
//    }

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
            String orchestratorUrl = System.getenv(IOTCRAWLER_ORCHESTRATOR_URL);
            String graphQL = System.getenv(GRAPHQL_ENDPOINT_URL);

            iotCrawlerClient = new IoTCrawlerRESTClient(System.getenv(IOTCRAWLER_ORCHESTRATOR_URL), System.getenv(GRAPHQL_ENDPOINT_URL) , cutURIs);
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

    public static double getTotalQueryExectionTime() {
        return totalQueryExectionTime;
    }

    public static void resetTotalQueryExectionTime() {
        totalQueryExectionTime=0;
    }

    public static long getTotalQueriesPerformed() {
        return totalQueriesPerformed;
    }

    public static void resetTotalQueriesPerformed() {
        totalQueriesPerformed=0;
    }

    private static List<Object> serveQuery(String typeURI, Map<String, Object> query, int offset, int limit){
        List ret = new ArrayList();

        if (query == null || query.size() == 0)
            try {
                long started = System.currentTimeMillis();
                List res = getIoTCrawlerClient().getEntities(typeURI, null, null, offset, limit);
                totalQueryExectionTime += (System.currentTimeMillis() - started);
                System.out.println("Plus "+(System.currentTimeMillis() - started) +" Type "+typeURI);
                totalQueriesPerformed++;
                return res;
            } catch (Exception e) {
                LOGGER.error("Failed to get entities of type {}: {}", typeURI, e.getLocalizedMessage());
                //e.printStackTrace();
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
                        //e.printStackTrace();
                    }
                }
            }


        //List augmented = augmentEntities(ret, concept);
        //return augmented;
        return ret;
    }

    private static List<Object> getConceptsByIds(List<String> ids, String concept){
        List enitities = new ArrayList();
        String typeURI = null;
        try {
            typeURI = findURI(concept);
        }
        catch (Exception e){
            LOGGER.error("Failed to find URI for {}: {}", concept, e.getLocalizedMessage());
            return enitities;
        }
        int count=0;
        final String typeURIfinal = typeURI;

        List<Callable<Object>> tasks = new ArrayList();
        for(String id : ids) {
            tasks.add(new Callable<Object>(){
                          @Override
                          public Object call(){
                              try {
                                  EntityLD entity = getIoTCrawlerClient().getEntityById(id);
                                  enitities.add(entity);
                              } catch (Exception e) {
                                  if(e.getCause() instanceof HttpClientErrorException.NotFound)
                                      LOGGER.debug("Entity {} not found", id);
                                  else {
                                      LOGGER.error("Failed to get entity {} of type {}: {}", id, concept, e.getLocalizedMessage());
                                      //e.printStackTrace();
                                  }
                                  enitities.add(new EntityLD(id, typeURIfinal));
                              }
                              return null;
                          }
                      }
            );
            count++;
        }
        long started = System.currentTimeMillis();
        try {
            executorService.invokeAll(tasks);
        }
        catch (Exception e){
            LOGGER.error("Failed execute tasks via executor service", e.getLocalizedMessage());
            e.printStackTrace();
        }
        totalQueryExectionTime += (System.currentTimeMillis() - started);
        System.out.println("Plus "+(System.currentTimeMillis() - started) +" - "+ tasks.size()+" queries of get entity By ID("+concept+")");
        totalQueriesPerformed+=tasks.size();

        if(ids.size()!=enitities.size()) {
            int delta = ids.size() - enitities.size();
            for (int i = 0; i < delta; i++) {
                enitities.add(null);   //filling missing results
                LOGGER.warn("Failed to return exact amount of entnties({}). Adding null entity to the result", concept);
            }
        }
        return enitities;
    }


    private static Map resolveFilters(Map<String, Object> query, DataFetchingEnvironment environment, Map<String, Object> argumentsToResolve) throws Exception {

        if(environment.getArgument("subClassOf")!=null) {
            String parentTypeName = environment.getArgument("subClassOf");
            try {
                String parentTypeURI = findURI(parentTypeName);
                query.put(NGSI_LD.alternativeType, "\"" + parentTypeURI + "\"");
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
                    if(targetType==null)
                        throw new Exception("Type " + inputTypeName + " not found in schema");

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

                        DataFetcher dataFetcher = genericDataFetcher(targetType.getName(), true);

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

//                                if (!((Iterable) entities).iterator().hasNext())
//                                    return null;

                                Object entityIds = ((ArrayList) entities).stream().map(entity -> ((EntityLD) entity).getId()).collect(Collectors.toList());
                                int size = ((List) entityIds).size();
                                //if (size == 0)
//                                    return null;

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
                            //return null;
                        }
                    }else if(targetType instanceof GraphQLScalarType){
                        query.put(propertyURI, (argValue instanceof String? "\""+argValue.toString()+"\"": argValue));
                    }else
                        throw new NotImplementedException();
                }



        return query;
    }



    //public static DataFetcher genericDataFetcher(Class targetClass, boolean resolvingInput) {
    public static DataFetcher genericDataFetcher(String concept, boolean calledRecursively) {

        return environment -> {
//            Boolean topLevelQuery = (resolvedConcepts.size()==0?true: false);
//            if(topLevelQuery) {
//                String test1 = "123";
//            }
            Context ctx = environment.getContext();
            DataLoader<String, Object> loader = ctx.getLoader(concept);
            if(loader==null) {
                LOGGER.error("Loader for " + concept + " not found");
                return null;
            }
            if(!calledRecursively)
                loader.clearAll();

            String id = environment.getArgument("id");
            String URI = environment.getArgument("URI");
            Object source = environment.getSource();


            if(id!=null) {
                if (calledRecursively){
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
                return loader.loadMany(Arrays.asList(new String[]{ id }));
                //return loader.load(id);
            }
//            if(URI!=null){
//                //Filtering query shoud
//                return loader.load(URI);
//            }

            Map<String,Object> query = new HashMap<>();

            int offset = 0;
            int limit = 500;

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
                    if(query==null)
                        return null;
                } catch (Exception e) {
                    LOGGER.error("Failed to resolve filters");
                    e.printStackTrace();
                    return null;
                }

            }

            //environment.getGraphQLSchema().getType(concept)
            //String currentType = concept;


            String typeURI = null;
            try {
                typeURI = findURI(concept);
            } catch (Exception e) {
                LOGGER.error("Failed to find URI for {}: {}", concept, e.getLocalizedMessage());
            }

            List entities = new ArrayList();
            try {
                entities = new ArrayList(serveQuery(typeURI, query, offset, limit));
            }catch (Exception e) {
                //LOGGER.error("Failed to get entities for query {}: {}", query, e.getLocalizedMessage());
            }
            if(!coreTypes.contains(concept)) //if additional resolution might be required
            {
                List<String> childTypes = getTopdownTypesAsList(concept);
                List<String> forBottomUpResolution = new ArrayList<>();
                forBottomUpResolution.add(concept);
                forBottomUpResolution.addAll(childTypes);
                Map<String, List<String>> typesWithFilters = resolveBottomUpType(forBottomUpResolution.toArray(new String[0]));

                List<EntityLD> resolvedEntities = serveResolvedEntities(typesWithFilters, environment);
                entities.addAll(resolvedEntities);
                String abc = "123";

            }

            List<String> ids = new ArrayList<>();
            entities.stream().forEach(entity0->{
                EntityLD entity = ((EntityLD)entity0);
                loader.prime(entity.getId(), entity);
                ids.add(entity.getId());
            });



//            if(topLevelQuery)
//                resolvedConcepts.clear();

            return loader.loadMany(ids);

        };
    }

    static List<String> getTopdownTypesAsList(String concept){
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

            if (topDownInheritance.containsKey(typeToTry))  //adding sensors/actuators/samples
                topDownInheritance.get(typeToTry).forEach(t2 -> {
                    if (!triedTypes.contains(t2))
                        ret.add(t2);
                    //typesToTry.add(t2);
                });
            triedTypes.add(typeToTry);
            typesToTry.remove(typeToTry);
        }
        return ret;
    }

    static Map<String, List<String>> resolveBottomUpType(String[] concepts){
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
                if (coreTypes.contains(typeToTry)) {
                    appendMapToList(ret, typeToTry, altType);
                }else if (bottomUpHierarchy.containsKey(typeToTry)) { //adding more generic type
                    bottomUpHierarchy.get(typeToTry).forEach(t2 -> {
                        if (!triedTypes.contains(t2)) {
                            if(coreTypes.contains(t2)) {
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

    static List<EntityLD> serveResolvedEntities(Map<String, List<String>> adjacentConcepts, DataFetchingEnvironment environment){
        List<EntityLD> ret = new ArrayList<>();

        for(String adjacentConcept: adjacentConcepts.keySet()){
            for(String filter: adjacentConcepts.get(adjacentConcept)){
                Map filterMap = new HashMap();
                if(filter!=null)
                    filterMap.put("subClassOf",filter);

                DataFetcher dataFetcher = genericDataFetcher(adjacentConcept,true);
                DataFetchingEnvironment environment2 = new DataFetchingEnvironmentImpl(
                        environment.getSource(),
                        (Map) filterMap,
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
                    LOGGER.debug("Executing adjacent {} fetcher with args {}", adjacentConcept, filterMap.toString());
                    List<EntityLD> ret2 = (List<EntityLD>)future.get();
                    ret.addAll(ret2);
                }
                catch (Exception e){
                    LOGGER.error("Failed to execute adjacent fetcher for {}: {}", adjacentConcept, e.getLocalizedMessage());
                }

            }
        }
        return ret;
    }



    TypeResolver typesResolver = environment -> {
        Object object = environment.getObject();

        GraphQLObjectType ret = null;
        if (object instanceof IoTStream) {
            ret = (GraphQLObjectType) environment.getSchema().getType("IoTStream");
        } else if(object instanceof Sensor) {
            ret = (GraphQLObjectType) environment.getSchema().getType("Sensor");
        }else
            throw new NotImplementedException(object.getClass().getCanonicalName()+" not implemented");
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
        throw new Exception("Type "+type+"."+property+" not found in binding registry");
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


}