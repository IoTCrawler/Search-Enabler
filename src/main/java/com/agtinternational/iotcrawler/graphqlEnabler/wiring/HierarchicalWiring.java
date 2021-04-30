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


import com.agtinternational.iotcrawler.graphqlEnabler.*;

import com.agtinternational.iotcrawler.graphqlEnabler.fetching.QueryExecutor;
import com.agtinternational.iotcrawler.graphqlEnabler.rule.ContextRule;
import graphql.schema.idl.*;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;


@Component
public class HierarchicalWiring implements Wiring {

    static Logger LOGGER = LoggerFactory.getLogger(HierarchicalWiring.class);
    public static final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();


    private Map<String, String> schemas;
    private RuntimeWiring.Builder runtimeWiringBuilder;

    private static Map<String, String> bindingRegistry = new HashMap<>();
    private static Map<String, List<String>> topDownInheritance = new HashMap<>();
    private static Map<String, List<String>> bottomUpHierarchy = new HashMap<>();
    private static Map<String, List<ContextRule>> ifThenRulesRegistry =new HashMap<>();

    public void setSchemaString(Map<String, String> schemas) {
        this.schemas = schemas;
    }

    public void setRuntimeWiringBuilder(RuntimeWiring.Builder runtimeWiringBuilder) {
        this.runtimeWiringBuilder = runtimeWiringBuilder;
    }

    public void setBindingRegistry(Map<String, String> bindingRegistry) {
        HierarchicalWiring.bindingRegistry = bindingRegistry;
    }

    public void setDirectivesRegistry(Map<String, List<ContextRule>> ifThenRulesRegistry) {
        HierarchicalWiring.ifThenRulesRegistry = ifThenRulesRegistry;
    }

    public void registerDataloaderConcept(String concept){
        dataLoaderRegistry.register(concept, new DataLoader(new GenericLoader(concept)));
    }


    @Bean
    public DataLoaderRegistry getDataLoaderRegistry() {
        return dataLoaderRegistry;
    }

    public static  Map<String, List<ContextRule>> getIfThenRulesRegistry() {
        return ifThenRulesRegistry;
    }

    //    TypeResolver typesResolver = environment -> {
//        Object object = environment.getObject();
//
//        GraphQLObjectType ret = null;
//        if (object instanceof IoTStream) {
//            ret = (GraphQLObjectType) environment.getSchema().getType("IoTStream");
//        } else if(object instanceof Sensor) {
//            ret = (GraphQLObjectType) environment.getSchema().getType("Sensor");
//        }else
//            throw new NotImplementedException(object.getClass().getCanonicalName()+" not implemented");
//        return ret;
//    };




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

    public static Map<String, List<String>> getTopDownInheritance() {
        return topDownInheritance;
    }

    public void setBottomUpHierarchy(Map<String, List<String>> bottomUpHierarchy) {
        this.bottomUpHierarchy = bottomUpHierarchy;
    }

    public static Map<String, List<String>> getBottomUpHierarchy() {
        return bottomUpHierarchy;
    }

    public static class GenericLoader implements org.dataloader.BatchLoader {

        String concept;
        public GenericLoader(String concept){
            this.concept = concept;
        }

        @Override
        public CompletionStage<List> load(List list) {
            return CompletableFuture.supplyAsync(() ->
                    QueryExecutor.getEntityByIdQuery(list, concept));
        }
    }


}