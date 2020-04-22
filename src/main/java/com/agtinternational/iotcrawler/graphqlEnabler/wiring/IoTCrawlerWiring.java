package com.agtinternational.iotcrawler.graphqlEnabler.wiring;

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

import com.agtinternational.iotcrawler.core.clients.IoTCrawlerRESTClient;
import com.agtinternational.iotcrawler.core.interfaces.IotCrawlerClient;
import com.agtinternational.iotcrawler.core.models.*;
import com.agtinternational.iotcrawler.core.ontologies.IotStream;
import com.agtinternational.iotcrawler.core.ontologies.SOSA;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.*;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import graphql.GraphQL;
import graphql.execution.ExecutionTypeInfo;
import graphql.language.*;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.*;
import jena.cmd.Arg;
import net.minidev.json.JSONObject;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.vocabulary.RDFS;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.apache.commons.lang.NotImplementedException;

import javax.swing.text.html.parser.Entity;
import javax.xml.crypto.Data;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.core.Constants.CUT_TYPE_URIS;
import static com.agtinternational.iotcrawler.core.Constants.IOTCRAWLER_ORCHESTRATOR_URL;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;


@Component
public class IoTCrawlerWiring implements Wiring {

    static Logger LOGGER = LoggerFactory.getLogger(IoTCrawlerWiring.class);
    public static final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
    static IotCrawlerClient iotCrawlerClient;
    Boolean cutURIs;
    public static Map<String, String> bindingRegistry = new HashMap<>();

    public IoTCrawlerWiring() {
        cutURIs = (System.getenv().containsKey(CUT_TYPE_URIS)?Boolean.parseBoolean(System.getenv(CUT_TYPE_URIS)):false);
    }

    public static IotCrawlerClient getIoTCrawlerClient(){
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

    private List<Object> getEntitiesViaHTTP(Map<String, Object> query, int offset, int limit, String concept){
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

    private List<Object> getEntitiesViaHTTP(List<String> keys, String concept){
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

        //List augmented = augmentEntities(entitiesFromDB, concept);
        if(keys.size()!=enitities.size()) {
            int delta = keys.size() - enitities.size();
            for (int i = 0; i < delta; i++) {
                enitities.add(null);   //filling missing results
                LOGGER.warn("Failed to return exact amount of entnties({}). Adding null entity to the result", concept);
            }
        }
        return enitities;
    }


    private List serveQuery(Map<String, Object> query, String concept, int offset, int limit){
        List entities = getEntitiesViaHTTP(query, offset,limit, concept);
        return entities;
    }

    private Map resolveInput(Map<String, Object> query, DataFetchingEnvironment environment, Map<String, Object> arguments) throws Exception {

        //for(String argName: arguments.keySet()) {
        for(Field field: environment.getFields())
            for(Argument argument: field.getArguments()){

                String argName = argument.getName();


            Object argValue = arguments.get(argName);
            String typeName = ((GraphQLModifiedType)environment.getFieldType()).getWrappedType().getName();


            String propertyURI = findURI(typeName, argName);

            if(propertyURI==null)
                throw new Exception("URI not found for "+argName);

            GraphQLArgument graphQLArgument = environment.getFieldTypeInfo().getFieldDefinition().getArgument(argName);
            GraphQLType graphQLInputType = environment.getFieldTypeInfo().getFieldDefinition().getType();
            if(graphQLArgument!=null)//getting from argument if possible
                graphQLInputType = graphQLArgument.getType();


            if(graphQLInputType instanceof GraphQLModifiedType)
                graphQLInputType = ((GraphQLModifiedType) graphQLInputType).getWrappedType();
            String inputTypeName = graphQLInputType.getName().replace("Input","");

            GraphQLType targetType = environment.getGraphQLSchema().getType(inputTypeName);

            if(targetType instanceof GraphQLObjectType) {

                LOGGER.info("Initializing {} fetcher to resolve {}", targetType.getName(), argName);
                DataFetcher dataFetcher = genericDataFetcher(targetType.getName(), true);

                //GraphQLOutputType graphQLType = environment.getFieldType();
                GraphQLList graphQLType = new GraphQLList(targetType);


                //GraphQLOutputType targetObjectType = environment.getFieldTypeInfo().getFieldDefinition().getType();

                //GraphQLFieldDefinition fieldDefinition = targetObjectType.getFieldDefinition(targetType.getName());
                List<Field> fields = new ArrayList<>();
                List<ObjectField> objectFields = ((ObjectValue) argument.getValue()).getObjectFields();
                Field field1 = null;
                for (ObjectField objectField : objectFields) {
                    List<Argument> arguments1 = new ArrayList<>();
                    arguments1.add(new Argument(objectField.getName(), objectField.getValue()));
                    field1 = new Field(objectField.getName(), arguments1);
                    fields.add(field1);
                }
//                GraphQLFieldDefinition fieldDefinition = ((GraphQLObjectType)environment.getParentType())
//                        .getFieldDefinition(inputTypeName.toLowerCase()+"s");

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
    public DataFetcher genericDataFetcher(String concept, boolean resolvingInput) {

        return environment -> {
            Context ctx = environment.getContext();
            DataLoader<String, Object> loader = ctx.getLoader(concept);
            String id = environment.getArgument("id");
            String URI = environment.getArgument("URI");
            Object source = environment.getSource();

//            if(source!=null){
//                String fieldName = environment.getField().getName();
//                if(fieldName.equals("observes"))
//                    ((Sensor)source).setObserves();
//            }

                if(id!=null) {
                 if (resolvingInput){
                     List<Object> entities = getEntitiesViaHTTP(Arrays.asList(id), concept);
                     List<String> ids = new ArrayList<>();
                     entities.stream().forEach(entity0 -> {
                         EntityLD entity = ((EntityLD) entity0);
                         loader.prime(entity.getId(), entity);
                         ids.add(entity.getId());
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

            if(environment.getArguments().size()>0){
                Map<String, Object> arguments = environment.getArguments();
                try {
                    query = resolveInput(query, environment, arguments);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            int offset = (query.containsKey("offset")?(int)query.get("offset"):0);
            int limit = (query.containsKey("limit")?(int)query.get("limit"):0);

            if(query.containsKey("offset"))
                query.remove("offset");

            if(query.containsKey("limit"))
                query.remove("limit");

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
        try {
            URL url = Resources.getResource("iotcrawler.graphqls");
            String sdl = Resources.toString(url, Charsets.UTF_8);
            return sdl;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public String findURI(String type, String property){
        if(bindingRegistry.containsKey(type+"."+property))
            return bindingRegistry.get(type+"."+property);

        return bindingRegistry.get(property);
    }

    @Override
    public RuntimeWiring build() {



        bindingRegistry.put("IoTStream", IoTStream.getTypeUri(cutURIs));
        bindingRegistry.put("Sensor", Sensor.getTypeUri(cutURIs));
        bindingRegistry.put("SensorInput", Sensor.getTypeUri(cutURIs));

        bindingRegistry.put("Platform", Platform.getTypeUri(cutURIs));
        bindingRegistry.put("PlatformInput", Platform.getTypeUri(cutURIs));

        bindingRegistry.put("ObservableProperty", ObservableProperty.getTypeUri(cutURIs));
        bindingRegistry.put("ObservablePropertyInput", ObservableProperty.getTypeUri(cutURIs));

        bindingRegistry.put("label", RDFS.label.toString());

        bindingRegistry.put("isHostedBy", SOSA.isHostedBy);
        bindingRegistry.put("hosts", SOSA.hosts);
        //bindingRegistry.put("madeBySensor", SOSA.madeBySensor);
        bindingRegistry.put("IotStream.observes", IotStream.observes);
        bindingRegistry.put("Sensor.observes", SOSA.observes);
        bindingRegistry.put("isObservedBy", SOSA.isObservedBy);


        bindingRegistry.put("generatedBy", IotStream.generatedBy);


        //RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.DateTime);
        String[] concepts = new String[]{ "IoTStream", "Sensor", "Platform", "ObservableProperty" };
        for(String concept: concepts)
            dataLoaderRegistry.register(concept, new DataLoader(new GenericLoader(concept)));

        dataLoaderRegistry.register("HomeState", new DataLoader<>(new GenericLoader("HomeState")));
        dataLoaderRegistry.register("Appliance", new DataLoader<>(new GenericLoader("Appliance")));
        dataLoaderRegistry.register("Activity", new DataLoader<>(new GenericLoader("Activity")));



        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("stream",  genericDataFetcher("IoTStream", false))
                        .dataFetcher("streams", genericDataFetcher("IoTStream", false))
                        .dataFetcher("sensor", genericDataFetcher("Sensor", false))
                        .dataFetcher("sensors", genericDataFetcher("Sensor", false))
                        .dataFetcher("platforms", genericDataFetcher("Platform", false))
                        .dataFetcher("platform", genericDataFetcher("Platform", false))
                        .dataFetcher("observableProperty", genericDataFetcher("ObservableProperty", false))
                        .dataFetcher("observableProperties", genericDataFetcher("ObservableProperty", false))
                        .dataFetcher("entities", genericDataFetcher("EntityLD", false))

                        .dataFetcher("homeState", genericDataFetcher("HomeState", false))
                        .dataFetcher("homeStates", genericDataFetcher("HomeState", false))
                        .dataFetcher("activity", genericDataFetcher("Activity", false))
                        .dataFetcher("activities", genericDataFetcher("Activity", false))

                )

//                .type(newTypeWiring("IoTStream")
//                        .dataFetcher("madeBySensor", genericDataFetcher(Sensor.class))
//                )
//
//                .type(newTypeWiring("Sensor")
//                        .dataFetcher("isHostedBy", genericDataFetcher(Platform.class))
//                        .dataFetcher("observes", genericDataFetcher(ObservableProperty.class))
//                )

                //.type(newTypeWiring("IoTStream").typeResolver(typesResolver))
                //.type(newTypeWiring("Sensor").typeResolver(typesResolver))

                .scalar(ExtendedScalars.Object);
                //.scalar(new FilterScalarType())

        CustomWiringFactory customWiringFactory = new CustomWiringFactory();
        runtimeWiringBuilder.wiringFactory(customWiringFactory);
        RuntimeWiring runtimeWiring = runtimeWiringBuilder.build();
        customWiringFactory.setRuntimeWiring(runtimeWiring);

        return runtimeWiring;
    }

    public class GenericLoader implements org.dataloader.BatchLoader {

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
