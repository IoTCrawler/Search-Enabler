package com.agtinternational.iotcrawler.graphqlEnabler.resolving;

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

import com.agtinternational.iotcrawler.core.ontologies.NGSI_LD;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.Context;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.HierarchicalWiring;
import graphql.execution.ExecutionTypeInfo;
import graphql.language.*;
import graphql.schema.*;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.dataloader.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.CORE_TYPES;

public class UniversalDataFetcher {
    static Logger LOGGER = LoggerFactory.getLogger(UniversalDataFetcher.class);
    static List<String> coreTypes = Arrays.asList(CORE_TYPES);

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

            String id = null;
            if(environment.getArgument("id")!=null){
                id = environment.getArgument("id");
                argumentsToResolve.remove("id");
            }


            if(id!=null){
                //if (calledRecursively){
                    List<Object> entities = QueryResolver.serveGetEntityByIdQuery(Arrays.asList(id), concept);
                    List<String> ids = new ArrayList<>();
                    entities.stream().forEach(entity0 -> {
                        EntityLD entity = ((EntityLD) entity0);
                        if(entity!=null) {
                            loader.prime(entity.getId(), entity);
                            ids.add(entity.getId());
                        }
                    });
                    CompletableFuture future = loader.loadMany(ids);
                     if(argumentsToResolve.size()==0)
                        return future;
                //}
            }

            Map<String,Object> query = new HashMap<>();
            if(argumentsToResolve.size()>0){
                try {
                    query = BottomUpStrategy.resolveQuery(environment, argumentsToResolve);
                    if(query==null)
                        return null;
                } catch (Exception e) {
                    LOGGER.error("Failed to resolve arguments bottom up: ", e.getLocalizedMessage());
                    e.printStackTrace();
                    return null;
                }
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
                entities = new ArrayList(QueryResolver.serveGetEntitiesQuery(typeURI, query, offset, limit));
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

            final String finalId = id;
            List<String> ids = new ArrayList<>();
            entities.stream().forEach(entity0->{
                EntityLD entity = ((EntityLD)entity0);
                if((finalId!=null && entity.getId().equals(finalId)) || finalId==null){
                    loader.prime(entity.getId(), entity);
                    ids.add(entity.getId());
                }
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
                }else if (HierarchicalWiring.getBottomUpHierarchy().containsKey(typeToTry)) { //adding more generic type
                    HierarchicalWiring.getBottomUpHierarchy().get(typeToTry).forEach(t2 -> {
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

                DataFetcher dataFetcher = UniversalDataFetcher.get(adjacentConcept);
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
}
