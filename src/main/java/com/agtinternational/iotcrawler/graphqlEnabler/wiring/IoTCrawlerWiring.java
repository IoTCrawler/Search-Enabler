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

import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.*;
import net.minidev.json.JSONObject;
import org.apache.jena.vocabulary.RDFS;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.swing.text.html.parser.Entity;
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

    private List<Object> getEntitiesViaHTTP(JSONObject query, int offset, int limit, String concept){
        List ret = new ArrayList();
        String typeURI = bindingRegistry.get(concept);
        try {
            ret = getIoTCrawlerClient().getEntities(typeURI, (query.size()>0?query.toString():null), null, offset, limit);
        }
        catch (Exception e){
            LOGGER.error("Failed to get {} entities", concept);
            e.printStackTrace();
        }

        //List augmented = augmentEntities(ret, concept);
        //return augmented;
        return ret;
    }

    private List<Object> getEntitiesViaHTTP(List<String> keys, String concept){
        List entitiesFromDB = new ArrayList();
        String typeURI = bindingRegistry.get(concept);
        int count=0;
        for(String key : keys) {
            try {
                List<EntityLD> entities = getIoTCrawlerClient().getEntityById(key);
                entitiesFromDB.addAll(entities);
            } catch (Exception e) {
                LOGGER.error("Failed to get entity {}", key, concept);
                //e.printStackTrace();
            }
            count++;
        }
        List augmented = entitiesFromDB;
        //List augmented = augmentEntities(entitiesFromDB, concept);
        if(keys.size()!=augmented.size()) {
            int delta = keys.size() - augmented.size();
            LOGGER.warn("Failed to return exact amount of entnties({}). Adding nulls entity to the result", concept);
            for (int i = 0; i < delta; i++) {
                augmented.add(null);   //filling missing results
            }
        }
        return augmented;
    }

    private List augmentEntities(List inputList, String concept){
        List augmented = new ArrayList();
        return inputList;
//        for(Object item: inputList)
//        try{
//            RDFModel rdfModel = RDFModel.fromEntity((EntityLD) item);
//            //Foo foo = (Foo) DebugProxy.newInstance(new FooImpl((EntityLD) item));
//            augmented.add(rdfModel);
//        }
//        catch (Exception e){
//            LOGGER.error(e.getLocalizedMessage());
//        }


//        if(concept=="IoTStream")
//            inputList.stream().forEach(item->{  augmented.add(new AugmentedIoTStream((IoTStream) item)); });
//
//        if(concept=="Sensor")
//            inputList.stream().forEach(item->{  augmented.add(new AugmentedSensor((Sensor) item)); });
//
//        if(concept=="Platform")
//            inputList.stream().forEach(item->{  augmented.add(new AugmentedPlatform((Platform) item)); });
//
//        if(concept=="ObservableProperty")
//            inputList.stream().forEach(item->{  augmented.add(new AugmentedObservableProperty((ObservableProperty) item)); });

        //return augmented;
    }

    private List serveQuery(JSONObject query, String concept, int offset, int limit){
        List entities = getEntitiesViaHTTP(query, offset,limit, concept);
        return entities;
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

                CompletableFuture future = (CompletableFuture) genericDataFetcher("Sensor", true).get(EnvironmentsBuilder.create(environment, arguments));
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

                CompletableFuture future = (CompletableFuture)genericDataFetcher("Platform", true).get(EnvironmentsBuilder.create(environment, arguments));

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
                CompletableFuture future = (CompletableFuture)genericDataFetcher("ObservableProperty", true).get(dataFetchingEnvironment);

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

            List augmentedEntities = serveQuery(query, concept, offset, limit);
            List<String> ids = new ArrayList<>();
            augmentedEntities.stream().forEach(entity0->{
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

    @Override
    public RuntimeWiring build() {



        bindingRegistry.put("IoTStream", IoTStream.getTypeUri(cutURIs));
        bindingRegistry.put("Sensor", Sensor.getTypeUri(cutURIs));
        bindingRegistry.put("Platform", Platform.getTypeUri(cutURIs));
        bindingRegistry.put("ObservableProperty", ObservableProperty.getTypeUri(cutURIs));

        bindingRegistry.put("label", RDFS.label.toString());

        bindingRegistry.put("isHostedBy", SOSA.isHostedBy);
        bindingRegistry.put("hosts", SOSA.hosts);
        bindingRegistry.put("madeBySensor", SOSA.madeBySensor);
        bindingRegistry.put("IotStream.observes", IotStream.observes);
        bindingRegistry.put("Sensor.observes", SOSA.observes);
        bindingRegistry.put("isObservedBy", SOSA.isObservedBy);


        bindingRegistry.put("generatedBy", IotStream.generatedBy);


        //RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.DateTime);
        String[] concepts = new String[]{ "IoTStream", "Sensor", "Platform", "ObservableProperty" };
        for(String concept: concepts)
            dataLoaderRegistry.register(concept, new DataLoader(new GenericLoader(concept)));
//        dataLoaderRegistry.register(IoTStream, new DataLoader(new GenericLoader("IoTStream")));
//        dataLoaderRegistry.register(Sensor.class.getSimpleName(), new DataLoader<>(new GenericLoader("Sensor")));
//        dataLoaderRegistry.register(Platform.class.getSimpleName(), new DataLoader<>(new GenericLoader("Platform")));
//        dataLoaderRegistry.register(ObservableProperty.class.getSimpleName(), new DataLoader<>(new GenericLoader("ObservableProperty")));


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
