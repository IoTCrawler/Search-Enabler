package com.agtinternational.iotcrawler.graphqlEnabler;

import graphql.Assert;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLScalarType;
import graphql.schema.PropertyDataFetcher;
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
