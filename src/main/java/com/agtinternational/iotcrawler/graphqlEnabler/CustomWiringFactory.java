package com.agtinternational.iotcrawler.graphqlEnabler;

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


import com.agtinternational.iotcrawler.graphqlEnabler.fetching.CustomPropertyDataFetcher;
import graphql.Assert;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLScalarType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.*;

public class CustomWiringFactory implements WiringFactory {
    RuntimeWiring runtimeWiring;

    public CustomWiringFactory() {

    }

    public void setRuntimeWiring(RuntimeWiring runtimeWiring) {
        this.runtimeWiring = runtimeWiring;
    }

    public boolean providesScalar(ScalarWiringEnvironment environment) {
        return false;
    }

    public GraphQLScalarType getScalar(ScalarWiringEnvironment environment) {
        return (GraphQLScalarType) Assert.assertShouldNeverHappen();
    }

    public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
        return false;
    }

    public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
        return (TypeResolver)Assert.assertShouldNeverHappen();
    }

    public boolean providesTypeResolver(UnionWiringEnvironment environment) {

        return false;
    }

    public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        return (TypeResolver)Assert.assertShouldNeverHappen();
    }

    public boolean providesDataFetcher(FieldWiringEnvironment environment) {

//        dataFetcher = (DataFetcher)runtimeWiring.getDataFetcherForType(parentTypeName).get(fieldName);
//        if (dataFetcher == null) {
//            dataFetcher = runtimeWiring.getDefaultDataFetcherForType(parentTypeName);
//            if (dataFetcher == null) {
//                dataFetcher = wiringFactory.getDefaultDataFetcher(environment);
//                if (dataFetcher == null) {
//                    dataFetcher = this.dataFetcherOfLastResort(wiringEnvironment);
//                }
//            }
//        }

        return false;
    }

    public DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
        return null;
//        String fieldName = environment.getFieldDefinition().getName();
//
//        DataFetcher dataFetcher = (DataFetcher)runtimeWiring.getDataFetcherForType(parentTypeName).get(fieldName);
//        if (dataFetcher == null) {
//            dataFetcher = runtimeWiring.getDefaultDataFetcherForType(parentTypeName);
//            if (dataFetcher == null) {
//                dataFetcher = getDefaultDataFetcher(environment);
//                if (dataFetcher == null) {
//                    //dataFetcher = this.dataFetcherOfLastResort(wiringEnvironment);
//                    return new CustomPropertyDataFetcher(fieldName);
//                }
//            }
    }

    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
            String fieldName = environment.getFieldDefinition().getName();
            return new CustomPropertyDataFetcher(fieldName);
    }
}
