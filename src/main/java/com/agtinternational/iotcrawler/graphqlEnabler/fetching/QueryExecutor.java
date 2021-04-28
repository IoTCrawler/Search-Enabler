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

import com.agtinternational.iotcrawler.core.clients.IoTCrawlerRESTClient;
import com.agtinternational.iotcrawler.core.interfaces.IoTCrawlerClient;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.HierarchicalWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.agtinternational.iotcrawler.core.Constants.CUT_TYPE_URIS;
import static com.agtinternational.iotcrawler.core.Constants.IOTCRAWLER_ORCHESTRATOR_URL;
import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.GRAPHQL_ENDPOINT_URL;

public class QueryExecutor {

    static Logger LOGGER = LoggerFactory.getLogger(QueryExecutor.class);
    static IoTCrawlerClient iotCrawlerClient;
    static long totalQueryExectionTime = 0;
    static List<String> totalQueryExectionTimeList = new ArrayList<>();
    static long totalQueriesPerformed = 0;

//    public QueryResolver(){
//        executorService = Executors.newFixedThreadPool(64);
//    }

    private static ExecutorService executorService = Executors.newFixedThreadPool(64);

    public static IoTCrawlerClient getIoTCrawlerClient(){
        Boolean cutURIs = (System.getenv().containsKey(CUT_TYPE_URIS)?Boolean.parseBoolean(System.getenv(CUT_TYPE_URIS)):false);
        if(iotCrawlerClient==null) {
            //String orchestratorUrl = System.getenv(IOTCRAWLER_ORCHESTRATOR_URL);
            //String graphQL = System.getenv(GRAPHQL_ENDPOINT_URL);

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

    public static List<Object> getEntities(String typeURI, Map<String, Object> query, Map<String, Number> ranking, int offset, int limit){
        List ret = new ArrayList();

        if (query == null || query.size() == 0)
            try {
                long started = System.currentTimeMillis();
                List res = getIoTCrawlerClient().getEntities(typeURI, null, ranking, offset, limit);
                long took = (System.currentTimeMillis() - started);
                totalQueryExectionTime += took;
                totalQueryExectionTimeList.add(String.valueOf(took/1000.0));
                LOGGER.debug("Plus "+(System.currentTimeMillis() - started) +"ms for querying Type "+typeURI);
                totalQueriesPerformed++;
                return res;
            } catch (Exception e) {
                LOGGER.error("Failed to get entities of type {}: {}", typeURI, e.getLocalizedMessage());
                e.printStackTrace();
                return null;
            }
        else {
            List<Callable<Object>> tasks = new ArrayList();
            List<Map> uniqueQueries = new ArrayList<>();
            List<String> criteriaNames = new ArrayList();
            criteriaNames.addAll(query.keySet());
            int i=0;
            for (String primaryCriteriaName : criteriaNames){
                i++;
                Object primaryCriteriaValues = query.get(primaryCriteriaName);


                if (!(primaryCriteriaValues instanceof Iterable))
                    primaryCriteriaValues = Arrays.asList(new Object[]{primaryCriteriaValues});

                Iterator primaryCriteriaValuesIterator = ((Iterable) primaryCriteriaValues).iterator();
                while (primaryCriteriaValuesIterator.hasNext()) {
                    Object primaryCriteriaValue = primaryCriteriaValuesIterator.next();

                    if(criteriaNames.size()==1) { //for a single criteria filter
                        Map query2 = new HashMap();
                        query2.put(primaryCriteriaName, primaryCriteriaValue);
                        uniqueQueries.add(query2);
                        continue;
                    }

                    List<String> secondaryCriterias = criteriaNames.subList(i, criteriaNames.size());
                    for (String secondaryCriteriaName : secondaryCriterias){
                        Object iterableSecondaryCriteriaValue = query.get(secondaryCriteriaName);
                        if (!(iterableSecondaryCriteriaValue instanceof Iterable))
                            iterableSecondaryCriteriaValue = Arrays.asList(new Object[]{iterableSecondaryCriteriaValue});
                        Iterator secondaryCriteriaValuesIterator = ((Iterable) iterableSecondaryCriteriaValue).iterator();
                        while (secondaryCriteriaValuesIterator.hasNext()) {
                            Object secondaryCriteriaValue = secondaryCriteriaValuesIterator.next();
                            if(secondaryCriteriaValue instanceof Iterable){
                                //restCriteriaValue = Arrays.asList(new Object[]{criteriaValues});
                                Iterator iterator3 = ((Iterable) secondaryCriteriaValue).iterator();
                                while(iterator3.hasNext()){
                                    Map query2 = new HashMap();
                                    query2.put(primaryCriteriaName, primaryCriteriaValue);
                                    query2.put(secondaryCriteriaName, iterator3.next());
                                    uniqueQueries.add(query2);
                                }
                            }else {
                                Map query2 = new HashMap();
                                query2.put(primaryCriteriaName, primaryCriteriaValue);
                                query2.put(secondaryCriteriaName, secondaryCriteriaValue);
                                uniqueQueries.add(query2);
                            }
                        }

                    }

//                    if(uniqueValues.contains(String.valueOf(iValue)))
//                        continue;
                }
            }

            for(Map query2 : uniqueQueries){
                tasks.add(new Callable<Object>(){
                              @Override
                              public Object call(){
                                  try {
                                      List<EntityLD> res = getIoTCrawlerClient().getEntities(typeURI, query2, null, offset, limit);
                                      for(EntityLD entityLD: res)
                                          if(!ret.contains(entityLD))
                                            ret.add(entityLD);
                                          else {
                                              String test = "123";
                                          }
                                  } catch (Exception e) {
                                      LOGGER.error("Failed to get entities of type {}: {}", typeURI, e.getLocalizedMessage());
                                      //e.printStackTrace();
                                  }
                                  return null;
                              }
                          }
                );
            }

            if(tasks.size()>0)
                try {
                    executorService.invokeAll(tasks);
                    String test = "123";
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }

        //List augmented = augmentEntities(ret, concept);
        //return augmented;
        return ret;
    }

    public static List<Object> getEntityByIdQuery(List<String> ids, String concept){
        List ret = new ArrayList();

        String typeURI = null;
        try {
            typeURI = HierarchicalWiring.findURI(concept);
        }
        catch (Exception e){
            LOGGER.error("Failed to find URI for {}: {}", concept, e.getLocalizedMessage());
            return ret;
        }
        int count=0;
        final String typeURIfinal = typeURI;

        List<Callable<Object>> tasks = new ArrayList();
        Map<String, EntityLD> enitities = new HashMap<>();
        for(String id : ids) {
            tasks.add(new Callable<Object>(){
                          @Override
                          public Object call(){
                              EntityLD entity;
                              try {
                                  entity = getIoTCrawlerClient().getEntityById(id);
                                  //entity = new EntityLD(id, typeURIfinal);
                                  enitities.put(id, entity);
                              } catch (Exception e) {
                                  if(e.getCause() instanceof HttpClientErrorException.NotFound)
                                      LOGGER.debug("Entity {} not found", id);
                                  else {
                                      LOGGER.error("Failed to get entity {} of type {}: {}", id, concept, e.getLocalizedMessage());
                                      //e.printStackTrace();
                                  }

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
            for(String id : ids) {
                ret.add(enitities.get(id));
            }
        }
        catch (Exception e){
            LOGGER.error("Failed execute tasks via executor service", e.getLocalizedMessage());
            e.printStackTrace();
        }
        long took = (System.currentTimeMillis() - started);
        totalQueryExectionTime += took;
        totalQueryExectionTimeList.add(String.valueOf(took/1000.0));
        LOGGER.debug("Plus "+(System.currentTimeMillis() - started) +"ms for "+ tasks.size()+" queries of get entity By ID("+concept+")");
        totalQueriesPerformed+=tasks.size();

        if(ids.size()!=ret.size()) {
            int delta = ids.size() - ret.size();
            for (int i = 0; i < delta; i++) {
                ret.add(null);   //filling missing results
                LOGGER.warn("Failed to return exact amount of entnties({}). Adding null entity to the result", concept);
            }
        }
        return ret;
    }

    public static double getTotalQueryExectionTime() {
        return totalQueryExectionTime;
    }
    public static List<String> getTotalQueryExectionList() {
        return totalQueryExectionTimeList;
    }
    public static void resetTotalQueryExectionTime() {
        totalQueryExectionTime=0;
        totalQueryExectionTimeList.clear();
    }
    public static long getTotalQueriesPerformed() {
        return totalQueriesPerformed;
    }
    public static void resetTotalQueriesPerformed() {
        totalQueriesPerformed=0;
    }

}
