package com.agtinternational.iotcrawler.graphqlEnabler.fetching;

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

import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.Context;
import com.agtinternational.iotcrawler.graphqlEnabler.resolution.BottomUpStrategy;
import com.agtinternational.iotcrawler.graphqlEnabler.resolution.TopDownStrategy;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.HierarchicalWiring;
import graphql.schema.*;
import org.dataloader.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.CORE_TYPES;

public class RecursiveDataFetcher {
    static Logger LOGGER = LoggerFactory.getLogger(RecursiveDataFetcher.class);

    public static DataFetcher get(String concept) {

        return environment -> {
//            Boolean topLevelQuery = (resolvedConcepts.size()==0?true: false);
//            if(topLevelQuery) {
//                String test1 = "123";
//            }
            LOGGER = LoggerFactory.getLogger(concept+"DataFetcher");
            Context ctx = environment.getContext();
            DataLoader<String, Object> loader = ctx.getLoader(concept);
            if(loader==null) {
                LOGGER.error("Loader for " + concept + " not found");
                return null;
            }
//            if(!calledRecursively)
//                loader.clearAll();

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

            List<String> requestedIds = new ArrayList<>();
            if(environment.getArgument("id")!=null){
                Object id = environment.getArgument("id");
                if(!(id instanceof Iterable))
                    requestedIds = Arrays.asList(new String[]{ requestedIds.toString() });
                else
                    requestedIds = (List<String>)id;
                argumentsToResolve.remove("id");
            }


            if(!requestedIds.isEmpty()){
                //if (calledRecursively){
                List<Object> entities = QueryExecutor.getEntityByIdQuery(requestedIds, concept);

                String type = null;
                try {
                    type = HierarchicalWiring.findURI(concept);
                } catch (Exception e) {
                    LOGGER.error("Failed to find URI for type {}", concept);
                    e.printStackTrace();
                }
//                final List<String> finalId = idsAsList;
//                final String finalType = type;

                    requestedIds.forEach(id->{
                        Optional<Object> firstElement = entities.stream().filter(e->e!=null && ((EntityLD)e).getId().equals(id)).findFirst();
                        Object entityLD = (firstElement.isPresent()? firstElement.get(): null);
                        loader.prime(id, entityLD);
                    });
//                    entities.stream().filter(e->e instanceof EntityLD).forEach(entity0 -> { //this might be null if non-existing id requested
//                        String id = ((EntityLD)entity0).getId();
//                        loader.prime(id, ((EntityLD)entity0).getId());
//                            ids.add(finalId);
//                    });
                    CompletableFuture future = loader.loadMany(requestedIds);
                     if(argumentsToResolve.size()==0)
                        return future;
                //}
            }


            Map<String,Number> ranking = null;
            if(argumentsToResolve.containsKey("ranking")){
                ranking = new HashMap<>();
                Iterator<Map<String,Object>> iterator = ((Iterable<Map<String,Object>>)argumentsToResolve.get("ranking")).iterator();
                while (iterator.hasNext()) {
                    Map<String, Object> criteria = iterator.next();
                    ranking.put(criteria.get("name").toString(), (Number)criteria.get("value"));
                }
                argumentsToResolve.remove("ranking");
            }

            Map<String,Object> query = new HashMap<>();
            if(argumentsToResolve.size()>0){
                try {
                    query = BottomUpStrategy.resolveFilters(environment, argumentsToResolve);
                    if(query==null)
                        return null;
                } catch (Exception e) {
                    LOGGER.error("Failed to resolve arguments bottom up: ", e.getLocalizedMessage());
                    e.printStackTrace();
                    return null;
                }
                //if bottom-up strategy returned nothing for a given agruments, when trying to apply them directly
                //required when we fetch entities from rules
                if(query.size()==0)
                    query.putAll(argumentsToResolve);
            }

            //environment.getGraphQLSchema().getType(concept)
            //String currentType = concept;


            String typeURI = null;
            try {
                typeURI = HierarchicalWiring.findURI(concept);
            } catch (Exception e) {
                LOGGER.error("Failed to find URI for {}: {}", concept, e.getLocalizedMessage());
            }

            List entities = new ArrayList();
            try {
                entities = new ArrayList(QueryExecutor.getEntities(typeURI, query, ranking, offset, limit));
            }catch (Exception e) {
                //LOGGER.error("Failed to get entities for query {}: {}", query, e.getLocalizedMessage());
            }

            if(!CORE_TYPES.contains(concept)) //if additional resolution might be required
            {
                List<String> childTypes = TopDownStrategy.getTopdownTypesAsList(concept);
                List<String> forBottomUpResolution = new ArrayList<>();
                forBottomUpResolution.add(concept);
                forBottomUpResolution.addAll(childTypes);
                Map<String, List<String>> typesWithFilters = BottomUpStrategy.resolveBottomUpType(forBottomUpResolution.toArray(new String[0]));

                List<EntityLD> adjacentEntities = fetchAdjacentConcepts(typesWithFilters, query, environment);
                entities.addAll(adjacentEntities);
                String abc = "123";

            }

            final List<String> finalIds = requestedIds;
            List<String> idsToReturn = new ArrayList<>();
            entities.stream().forEach(entity0->{
                EntityLD entity = ((EntityLD)entity0);

                if((!finalIds.isEmpty() && finalIds.contains(entity.getId())) || finalIds.isEmpty()){
                    loader.prime(entity.getId(), entity);
                    idsToReturn.add(entity.getId());
                }
            });

//            if(topLevelQuery)
//                resolvedConcepts.clear();

            return loader.loadMany(idsToReturn);

        };
    }



    static List<EntityLD> fetchAdjacentConcepts(Map<String, List<String>> adjacentConcepts, Map<String,Object> query, DataFetchingEnvironment environment){
        List<EntityLD> ret = new ArrayList<>();

        for(String adjacentConcept: adjacentConcepts.keySet()){
            for(String filter: adjacentConcepts.get(adjacentConcept)){
                Map arguments = new HashMap();
                arguments.putAll(query);

                if(filter!=null)
                    arguments.put("subClassOf",filter);

                List<EntityLD> entities = fetch(adjacentConcept, arguments, environment);
                ret.addAll(entities);
            }
        }
        return ret;
    }

    public static List<EntityLD> fetch(String concept, Map arguments, DataFetchingEnvironment environment){
        DataFetcher dataFetcher = RecursiveDataFetcher.get(concept);
        DataFetchingEnvironment environment2 = new DataFetchingEnvironmentImpl(
                environment.getSource(),
                (Map) arguments,
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
            LOGGER.debug("Executing {} fetcher with args {}", concept, arguments.toString());
            List<EntityLD> ret = (List<EntityLD>)future.get();
            return ret;
        }
        catch (Exception e){
            LOGGER.error("Failed to execute {} fetcher for {}: {}", concept, arguments.toString(), e.getLocalizedMessage());
        }
        return null;
    }
}
