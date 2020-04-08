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
import com.agtinternational.iotcrawler.core.clients.IoTCrawlerRPCClient;
import com.agtinternational.iotcrawler.core.interfaces.IotCrawlerClient;
import com.agtinternational.iotcrawler.core.models.*;
import com.agtinternational.iotcrawler.core.ontologies.SOSA;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.Context;
import com.agtinternational.iotcrawler.graphqlEnabler.Wiring;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import net.minidev.json.JSONObject;
import org.apache.jena.vocabulary.RDFS;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.core.Constants.IOTCRAWLER_ORCHESTRATOR_URL;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class IoTCrawlerWiring implements Wiring {

    static Logger LOGGER = LoggerFactory.getLogger(IoTCrawlerWiring.class);
    public static final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
    static IotCrawlerClient iotCrawlerClient;

    public IoTCrawlerWiring() {

    }

    public static IotCrawlerClient getIoTCrawlerClient(){
        if(iotCrawlerClient==null) {
            iotCrawlerClient = new IoTCrawlerRESTClient(System.getenv().get(IOTCRAWLER_ORCHESTRATOR_URL));
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

    private static List<Object> getAugmentedEntitiesViaHTTP(JSONObject query, int offset, int limit, Class targetClass){
        List ret = new ArrayList();
        try {
            ret = getIoTCrawlerClient().getEntities(targetClass, (query.size()>0?query.toString():null), null, offset, limit);
        }
        catch (Exception e){
            LOGGER.error("Failed to get {} entities", targetClass.getCanonicalName());
            e.printStackTrace();
        }

        List augmented = augmentEntities(ret, targetClass);
        return augmented;
    }

    private static List<Object> getAugmentedEntitiesViaHTTP(List<String> keys, Class targetClass){
        List ret = new ArrayList();
        try {
            ret = getIoTCrawlerClient().getEntityById(keys.get(0), targetClass);
        }
        catch (Exception e){
            LOGGER.error("Failed to get {} entities", targetClass.getCanonicalName());
            e.printStackTrace();
        }

        //List augmented = ret;
        List augmented = augmentEntities(ret, targetClass);
        if(keys.size()!=augmented.size()) {
            LOGGER.warn("Failed to return exact amount of entnties({}). Adding nulls entity to the result", targetClass);
            for (int i = 0; i < keys.size() - augmented.size(); i++) {
                augmented.add(null);   //filling missing results
            }
        }
        return augmented;
    }

    private static List augmentEntities(List inputList, Class targetClass){
        List augmented = new ArrayList();
        if(targetClass==IoTStream.class)
            inputList.stream().forEach(item->{  augmented.add(new AugmentedIoTStream((IoTStream) item)); });

        if(targetClass==Sensor.class)
            inputList.stream().forEach(item->{  augmented.add(new AugmentedSensor((Sensor) item)); });

        if(targetClass==Platform.class)
            inputList.stream().forEach(item->{  augmented.add(new AugmentedPlatform((Platform) item)); });

        if(targetClass==ObservableProperty.class)
            inputList.stream().forEach(item->{  augmented.add(new AugmentedObservableProperty((ObservableProperty) item)); });

        return augmented;
    }

    private static List serveQuery(JSONObject query, Class targetClass, int offset, int limit){
        List entities = getAugmentedEntitiesViaHTTP(query, offset,limit, targetClass);
        return entities;
    }


    public static DataFetcher genericDataFetcher(Class targetClass, boolean resolvingInput) {
        return environment -> {
            Context ctx = environment.getContext();
            DataLoader<String, Object> loader = ctx.getLoader(targetClass.getSimpleName());
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
                     List<Object> entities = getAugmentedEntitiesViaHTTP(Arrays.asList(id), targetClass);
                     List<String> ids = new ArrayList<>();
                     entities.stream().forEach(entity0 -> {
                         RDFModel entity = ((RDFModel) entity0);
                         loader.prime(entity.getURI(), entity);
                         ids.add(entity.getURI());
                     });
                     //return loader.loadMany(ids);
                 }
                 return loader.load(id);
             }
//            if(URI!=null){
//                //Filtering query shoud
//                return loader.load(URI);
//            }

            JSONObject query = new JSONObject();
            query.putAll(environment.getArguments());

            if(query.containsKey("label")){
                query.put(RDFS.label.getURI(), query.get("label"));
                query.remove("label");
            }

            if(query.containsKey("offset"))
                query.remove("offset");

            if(query.containsKey("limit"))
                query.remove("limit");


            if(environment.getArgument("madeBySensor")!=null){  //sensor

                Map<String, Object> arguments = new HashMap<>();
                arguments.putAll(environment.getArgument("madeBySensor"));

                CompletableFuture future = (CompletableFuture) genericDataFetcher(Sensor.class, true).get(EnvironmentsBuilder.create(environment, arguments));
                try {
                    Object entities = future.get();
                    if(entities instanceof Iterable) {
                        if(!((Iterable)entities).iterator().hasNext())
                            return null;

                        Object entityIds = ((ArrayList)entities).stream().map(sensor -> ((RDFModel)sensor).getURI()).collect(Collectors.toList());
                        if (((List)entityIds).size()==0)
                            return null;
                        query.put(SOSA.madeBySensor, entityIds);
                    }else
                        query.put(SOSA.madeBySensor, ((RDFModel)entities).getURI());
                    query.remove("madeBySensor");
                }
                catch (Exception e){
                    LOGGER.error("Failed to resolve madeBySensor filter");
                    e.printStackTrace();
                    return null;
                }
            }

            if(environment.getArgument("isHostedBy")!=null){   //platform
                Map<String, Object> arguments = new HashMap<>();
                arguments.putAll(environment.getArgument("isHostedBy"));

                CompletableFuture future = (CompletableFuture)genericDataFetcher(Platform.class, true).get(EnvironmentsBuilder.create(environment, arguments));

                try {
                    Object entities = future.get();
                    if(entities instanceof Iterable) {
                        Object entityIds = ((ArrayList)entities).stream().map(sensor -> ((RDFModel)sensor).getURI()).collect(Collectors.toList());
                        if (((List)entityIds).size()==0)
                            return null;
                        query.put(SOSA.isHostedBy, entityIds);
                    }else
                        query.put(SOSA.isHostedBy, ((RDFModel)entities).getURI());
                    query.remove("isHostedBy");
                }
                catch (Exception e){
                    LOGGER.error("Failed to resolve isHostedBy filter");
                    e.printStackTrace();
                    return null;
                }

            }

            if(environment.getArgument("observes")!=null){  //observableProperty
                Map<String, Object> arguments = new HashMap<>();
                arguments.putAll(environment.getArgument("observes"));

                //CompletableFuture future = (CompletableFuture)genericDataFetcher(ObservableProperty.class).get();

                DataFetchingEnvironment dataFetchingEnvironment =  EnvironmentsBuilder.create(environment, arguments);
                CompletableFuture future = (CompletableFuture)genericDataFetcher(ObservableProperty.class, true).get(dataFetchingEnvironment);

                try {

                    Object entities = future.get();
                    if(entities instanceof Iterable) {
                        Object entityIds = ((ArrayList)entities).stream().map(sensor -> ((RDFModel)sensor).getURI()).collect(Collectors.toList());
                        if (((List)entityIds).size()==0)
                            return null;
                        query.put(SOSA.observes, entityIds);
                    }else
                        query.put(SOSA.observes, ((RDFModel)entities).getURI());
                    query.remove("observes");
                }
                catch (Exception e){
                    LOGGER.error("Failed to resolve observes filter");
                    e.printStackTrace();
                    return null;
                }
            }
//            if (environment.getArgument("query")!=null)
//                query = environment.getArgument("query");

            int offset = (environment.getArgument("offset")!=null?environment.getArgument("offset"):0);
            int limit = (environment.getArgument("limit")!=null?environment.getArgument("limit"):0);

            List entities = serveQuery(query, targetClass, offset, limit);
            List<String> ids = new ArrayList<>();
            entities.stream().forEach(entity0->{
                RDFModel entitiy = ((RDFModel)entity0);
                loader.prime(entitiy.getURI(), entitiy);
                ids.add(entitiy.getURI());
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
        }
        return ret;
    };


    @Override
    public String getSchemaString(){
        try {
            URL url = Resources.getResource("schema.graphqls");
            String sdl = Resources.toString(url, Charsets.UTF_8);
            return sdl;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RuntimeWiring build() {
        //RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.DateTime);

        dataLoaderRegistry.register(IoTStream.class.getSimpleName(), new DataLoader(new GenericLoader(IoTStream.class)));
        dataLoaderRegistry.register(Sensor.class.getSimpleName(), new DataLoader<>(new GenericLoader(Sensor.class)));
        dataLoaderRegistry.register(Platform.class.getSimpleName(), new DataLoader<>(new GenericLoader(Platform.class)));
        dataLoaderRegistry.register(ObservableProperty.class.getSimpleName(), new DataLoader<>(new GenericLoader(ObservableProperty.class)));

        return RuntimeWiring.newRuntimeWiring()

                .type(newTypeWiring("Query")
                        .dataFetcher("stream",  genericDataFetcher(IoTStream.class, false))
                        .dataFetcher("streams", genericDataFetcher(IoTStream.class, false))
                        .dataFetcher("sensor", genericDataFetcher(Sensor.class, false))
                        .dataFetcher("sensors", genericDataFetcher(Sensor.class, false))
                        .dataFetcher("platforms", genericDataFetcher(Platform.class, false))
                        .dataFetcher("platform", genericDataFetcher(Platform.class, false))
                        .dataFetcher("observableProperty", genericDataFetcher(ObservableProperty.class, false))
                        .dataFetcher("observableProperties", genericDataFetcher(ObservableProperty.class, false))
                        .dataFetcher("entities", genericDataFetcher(EntityLD.class, false))
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

                .scalar(ExtendedScalars.Object)
                //.scalar(new FilterScalarType())
                .build();
    }

    public class GenericLoader implements org.dataloader.BatchLoader {

        Class targetClass;
        public GenericLoader(Class targetClass){
            this.targetClass = targetClass;
        }

        @Override
        public CompletionStage<List> load(List list) {
            return CompletableFuture.supplyAsync(() -> getAugmentedEntitiesViaHTTP(list, targetClass));
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
