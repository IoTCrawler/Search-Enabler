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


import graphql.GraphQLException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;
import graphql.schema.PropertyDataFetcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import graphql.Assert;
import graphql.PublicApi;


import java.util.function.Function;


@PublicApi
public class CustomPropertyDataFetcher<T> implements DataFetcher<T> {

    private final String propertyName;
    private final Function<Object, Object> function;

    /**
     * This constructor will use the property name and examine the {@link DataFetchingEnvironment#getSource()}
     * object for a getter method or field with that name.
     *
     * @param propertyName the name of the property to retrieve
     */
    public CustomPropertyDataFetcher(String propertyName) {
        this.propertyName = Assert.assertNotNull(propertyName);
        this.function = null;
    }

    @SuppressWarnings("unchecked")
    private <O> CustomPropertyDataFetcher(Function<O, T> function) {
        this.function = (Function<Object, Object>) Assert.assertNotNull(function);
        this.propertyName = null;
    }

    /**
     * Returns a data fetcher that will use the property name to examine the {@link DataFetchingEnvironment#getSource()} object
     * for a getter method or field with that name, or if its a map, it will look up a value using
     * property name as a key.
     * <p>
     * For example :
     * <pre>
     * {@code
     *
     *      DataFetcher functionDataFetcher = fetching("pojoPropertyName");
     *
     * }
     * </pre>
     *
     * @param propertyName the name of the property to retrieve
     * @param <T>          the type of result
     * @return a new PropertyDataFetcher using the provided function as its source of values
     */
    public static <T> CustomPropertyDataFetcher<T> fetching(String propertyName) {
        return new CustomPropertyDataFetcher<>(propertyName);
    }

    /**
     * Returns a data fetcher that will present the {@link DataFetchingEnvironment#getSource()} object to the supplied
     * function to obtain a value, which allows you to use Java 8 method references say obtain values in a
     * more type safe way.
     * <p>
     * For example :
     * <pre>
     * {@code
     *
     *      DataFetcher functionDataFetcher = fetching(Thing::getId);
     *
     * }
     * </pre>
     *
     * @param function the function to use to obtain a value from the source object
     * @param <O>      the type of the source object
     * @param <T>      the type of result
     * @return a new PropertyDataFetcher using the provided function as its source of values
     */
    public static <T, O> CustomPropertyDataFetcher<T> fetching(Function<O, T> function) {
        return new CustomPropertyDataFetcher<>(function);
    }

    /**
     * @return the property that this is fetching for
     */
    public String getPropertyName() {
        return propertyName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(DataFetchingEnvironment environment) {
        Object source = environment.getSource();
        if (source == null) {
            return null;
        }

        if (function != null) {
            return (T) function.apply(source);
        }

        try {
            Object ret = CustomPropertyDataFetcherHelper.getPropertyValue(propertyName, source, environment.getFieldType(), environment);
            //Object ret = CustomPropertyDataFetcherHelper.getPropertyValue(propertyName, source, environment.getParentType(), environment);
            return (T)ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * PropertyDataFetcher caches the methods and fields that map from a class to a property for runtime performance reasons
     * as well as negative misses.
     * <p>
     * However during development you might be using an assistance tool like JRebel to allow you to tweak your code base and this
     * caching may interfere with this.  So you can call this method to clear the cache.  A JRebel plugin could
     * be developed to do just that.
     */
    @SuppressWarnings("unused")
    public static void clearReflectionCache() {
        CustomPropertyDataFetcherHelper.clearReflectionCache();
    }

    /**
     * This can be used to control whether PropertyDataFetcher will use {@link java.lang.reflect.Method#setAccessible(boolean)} to gain access to property
     * values.  By default it PropertyDataFetcher WILL use setAccessible.
     *
     * @param flag whether to use setAccessible
     * @return the previous value of the flag
     */
    public static boolean setUseSetAccessible(boolean flag) {
        return CustomPropertyDataFetcherHelper.setUseSetAccessible(flag);
    }

    /**
     * This can be used to control whether PropertyDataFetcher will cache negative lookups for a property for performance reasons.  By default it PropertyDataFetcher WILL cache misses.
     *
     * @param flag whether to cache misses
     * @return the previous value of the flag
     */
    public static boolean setUseNegativeCache(boolean flag) {
        return CustomPropertyDataFetcherHelper.setUseNegativeCache(flag);
    }
}
