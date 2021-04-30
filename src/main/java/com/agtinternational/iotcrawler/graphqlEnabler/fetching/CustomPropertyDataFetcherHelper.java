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


import com.agtinternational.iotcrawler.core.Utils;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.rule.ContextRule;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.HierarchicalWiring;
import com.orange.ngsi2.model.Attribute;
import graphql.GraphQLException;
import graphql.Internal;
import graphql.schema.*;

import org.apache.commons.lang3.NotImplementedException;
import org.dataloader.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.agtinternational.iotcrawler.core.Constants.CUT_TYPE_URIS;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.schema.GraphQLTypeUtil.isNonNull;
import static graphql.schema.GraphQLTypeUtil.unwrapOne;

@Internal
public class CustomPropertyDataFetcherHelper {
    private static Logger LOGGER = LoggerFactory.getLogger(CustomPropertyDataFetcherHelper.class);

    private static final AtomicBoolean USE_SET_ACCESSIBLE = new AtomicBoolean(true);
    private static final AtomicBoolean USE_NEGATIVE_CACHE = new AtomicBoolean(true);
    private static final ConcurrentMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> NEGATIVE_CACHE = new ConcurrentHashMap<>();

    public static Object getPropertyValue(String propertyName, Object object, GraphQLType graphQLType) throws Exception {
        return getPropertyValue(propertyName, object, graphQLType, null);
    }

    public static Object getPropertyValue(String propertyName, Object object0, GraphQLType fieldType, DataFetchingEnvironment environment) throws Exception {
        if (object0 == null) {
            return null;
        }
        if (object0 instanceof Map) {
            return ((Map<?, ?>) object0).get(propertyName);
        }

        String key = mkKey(object0, propertyName);
        if (isNegativelyCached(key)) {
            return null;
        }
        boolean dfeInUse = environment != null;
        try {

            Object ret = getPropertyViaGetterMethod(object0, propertyName, fieldType, (root, methodName) -> findPubliclyAccessibleMethod(propertyName, root, methodName, dfeInUse), environment);
            return ret;
        } catch (NoSuchMethodException ignored) {
            String abc = "123";
        }

        //Boolean changed = false;
        Object object = object0;
        if(object instanceof EntityLD) {  //Handling multivalue properties
            object = Arrays.asList(new Object[]{object});
            //changed = true;
        }

        GraphQLType wrappedType = null;
        if (fieldType instanceof GraphQLList)
            wrappedType = ((GraphQLList) fieldType).getWrappedType();
        else if (fieldType instanceof GraphQLNonNull)
            wrappedType = ((GraphQLNonNull) fieldType).getWrappedType();
        else  if (fieldType instanceof GraphQLObjectType || fieldType instanceof GraphQLScalarType)
            wrappedType = fieldType;
        else if(fieldType!=null)
            LOGGER.warn("Skipping fieldType="+fieldType.getClass().getName());

        GraphQLType unwrappedPropertyType = null;
        if (wrappedType instanceof GraphQLList)
            unwrappedPropertyType = ((GraphQLList) wrappedType).getWrappedType();
        else if (wrappedType instanceof GraphQLObjectType)
            unwrappedPropertyType = ((GraphQLObjectType) wrappedType);
        else if (wrappedType instanceof GraphQLNonNull)
            unwrappedPropertyType = ((GraphQLNonNull) wrappedType).getWrappedType();
        else if(wrappedType!=null)
            LOGGER.warn("Skipping wrappedType="+wrappedType.getClass().getName());

        if(wrappedType==null)
            throw new Exception("Wrapped property type cannot be null");

        List referenceIDs = new ArrayList();
        Object valueToReturn = null;
        if(object instanceof Iterable) {
            Iterator iterator = ((Iterable) object).iterator();
            while (iterator.hasNext()) {
                Object undefinedTypeObject = iterator.next();
                if (undefinedTypeObject != null)
                    if (!(undefinedTypeObject instanceof EntityLD))
                        throw new NotImplementedException(undefinedTypeObject.getClass().getCanonicalName()+" not implemented");

                EntityLD entityLD = (EntityLD) undefinedTypeObject;
//                        try {
//
//                            Object ret = getPropertyViaGetterMethod(object0, propertyName, graphQLType, (root, methodName) -> findPubliclyAccessibleMethod(propertyName, root, methodName, dfeInUse), environment);
//                            value = ret;
//                        } catch (NoSuchMethodException ignored) {
                //String objectType = Utils.getFragment(((EntityLD) object0).getType());
                String environmentTypeName = environment.getParentType().getName();
                String propertyNameURI = HierarchicalWiring.findURI(environmentTypeName, propertyName);
                //if (propertyNameURI == null)
                //Getting parent type Might be problematic!
                //propertyNameURI = GenericMDRWiring.findURI(environment.getParentType().getName(), propertyName);

                if (propertyNameURI == null){
                    LOGGER.warn("No URI found for " + propertyName + " in " + ((EntityLD) undefinedTypeObject).getId());
                    continue;
                }

                if(System.getenv().containsKey(CUT_TYPE_URIS))
                    propertyNameURI = Utils.cutURL(propertyNameURI, (Map)(entityLD).getContext());
                Object attributeValue = entityLD.getAttribute(propertyNameURI);
                //processing only relations

//                            if (propertyNameURI.startsWith("http://") && attribute == null)
//                                attribute = ((EntityLD) object0).getAttribute(propertyNameURI);
                String rulesKey = environment.getParentType().getName()+"."+propertyName;
                List<ContextRule> contextRules = HierarchicalWiring.getIfThenRulesRegistry().get(rulesKey);
                if (attributeValue == null && contextRules==null) {
                    LOGGER.warn("Value of " + propertyNameURI + " attribute not found in " + entityLD.getId());
                    continue;
                }

                if (contextRules != null) {
                    for (ContextRule contextRule : contextRules) {
                        String conditionPropertyURI = HierarchicalWiring.findURI(environmentTypeName, contextRule.getConditionToMeet().getKey());
                        Attribute attributeToMeet = (Attribute) entityLD.getAttribute(conditionPropertyURI);
                        if(attributeToMeet!=null){
                            Object value = attributeToMeet.getValue();
                            if (value != null && contextRule.getConditionToMeet().meets(value.toString())) {
                                conditionPropertyURI = HierarchicalWiring.findURI(environmentTypeName, contextRule.getConditionToApply().getDefinesField());
                                Object originalValue = entityLD.getAttribute(conditionPropertyURI);
                                if (originalValue != null) {

                                    Map arguments = new HashMap();
                                    conditionPropertyURI = HierarchicalWiring.findURI(unwrappedPropertyType.getName(), contextRule.getConditionToApply().getKey());
                                    arguments.put(conditionPropertyURI, "\""+contextRule.getConditionToApply().getValue()+"\"");
                                    List<EntityLD> entitiesMatchingCondition = RecursiveDataFetcher.fetch(unwrappedPropertyType.getName(), arguments, environment);
                                    List<String> idsMatchingTheRule = entitiesMatchingCondition.stream().map(entityLD1 -> entityLD1.getId()).collect(Collectors.toList());

                                    Object originalValueAsList = originalValue;
                                    boolean convertedToList = false;
                                    if (!(originalValueAsList instanceof Iterable)) {
                                        List list = new ArrayList();
                                        list.add(originalValue);
                                        originalValueAsList = list;
                                        convertedToList = true;
                                    }
                                    Iterator iterator1 = ((Iterable) originalValueAsList).iterator();

                                    while (iterator1.hasNext()) {
                                        attributeValue = iterator1.next();
                                        if (attributeValue instanceof Attribute) {
                                            valueToReturn = ((Attribute) attributeValue).getValue();
                                            if (((Attribute) attributeValue).getType().get().equals("Relationship")) {
                                                if (valueToReturn instanceof List) {
                                                    for (Object referenceId : (List) valueToReturn)
                                                        if (idsMatchingTheRule.contains(referenceId))
                                                            referenceIDs.add(referenceId);
                                                } else if (idsMatchingTheRule.contains(valueToReturn))
                                                    referenceIDs.add(valueToReturn);
                                            } else {
                                                if (convertedToList)
                                                    return valueToReturn;
                                                return attributeValue; //return the whole array instead of one value
                                            }
                                        } else
                                            throw new NotImplementedException(attributeValue.getClass().getCanonicalName() + " as attribute type");
                                    }
                                }

                            }
                        }
                    }
                } else {
                    boolean convertedToList = false;
                    if (!(attributeValue instanceof Iterable)) {
                        List list = new ArrayList();
                        list.add(attributeValue);
                        attributeValue = list;
                        convertedToList = true;
                    }
                    Iterator iterator1 = ((Iterable) attributeValue).iterator();
                    while (iterator1.hasNext()) {
                        attributeValue = iterator1.next();
                        if (attributeValue instanceof Attribute) {
                            valueToReturn = ((Attribute) attributeValue).getValue();
//                            if(graphQLType instanceof GraphQLList && !(value instanceof List))
//                                value = Arrays.asList(new Object[]{ value });

                            if (((Attribute) attributeValue).getType().get().equals("Relationship")) {
                                if (valueToReturn instanceof List)
                                    referenceIDs.addAll((List) valueToReturn);
                                else
                                    referenceIDs.add(valueToReturn);
                            } else {
                                if (convertedToList)
                                    return valueToReturn;
                                return attributeValue; //return the whole array instead of one value
                            }
                        } else
                            throw new NotImplementedException(attributeValue.getClass().getCanonicalName() + " as attribute type");
                    }
                }
            }
        }

        if(referenceIDs.size()>0){
            String unwrappedPropertyTypeName = unwrappedPropertyType.getName();
            String propertyTypeURI = HierarchicalWiring.findURI(unwrappedPropertyTypeName);
            if (propertyTypeURI != null) {
                DataLoader loader = HierarchicalWiring.dataLoaderRegistry.getDataLoader(unwrappedPropertyTypeName);
                if (loader == null)
                    throw new Exception("No data loader for " + unwrappedPropertyTypeName);
                CompletableFuture future;
                if (fieldType instanceof GraphQLList)
                //if(referenceIDs.size()>1)
                    future = loader.loadMany(referenceIDs);
                else
                    future = loader.load(referenceIDs.get(0));
                return future;
                //Object ret = future.get();
                //return ret;
            }

        }

        return valueToReturn;
    }

    private static boolean isNegativelyCached(String key) {
        if (USE_NEGATIVE_CACHE.get()) {
            return NEGATIVE_CACHE.containsKey(key);
        }
        return false;
    }

    private static void putInNegativeCache(String key) {
        if (USE_NEGATIVE_CACHE.get()) {
            NEGATIVE_CACHE.put(key, key);
        }
    }

    private interface MethodFinder {
        Method apply(Class<?> aClass, String s) throws NoSuchMethodException;
    }

    private static Object getPropertyViaGetterMethod(Object object, String propertyName, GraphQLType graphQLType, MethodFinder methodFinder, DataFetchingEnvironment environment) throws NoSuchMethodException {
        if (isBooleanProperty(graphQLType)) {
            try {
                return getPropertyViaGetterUsingPrefix(object, propertyName, "is", methodFinder, environment);
            } catch (NoSuchMethodException e) {
                return getPropertyViaGetterUsingPrefix(object, propertyName, "get", methodFinder, environment);
            }
        } else {
            return getPropertyViaGetterUsingPrefix(object, propertyName, "get", methodFinder, environment);
        }
    }

    private static Object getPropertyViaGetterUsingPrefix(Object object, String propertyName, String prefix, MethodFinder methodFinder, DataFetchingEnvironment environment) throws NoSuchMethodException {
        String getterName = prefix + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
            Method method = methodFinder.apply(object.getClass(), getterName);
            if (takesDataFetcherEnvironmentAsOnlyArgument(method)) {
                if (environment == null) {
                    throw new FastNoSuchMethodException(getterName);
                }
                return method.invoke(object, environment);
            } else {
                return method.invoke(object);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new GraphQLException(e);
        }
    }


    /**
     * Invoking public methods on package-protected classes via reflection
     * causes exceptions. This method searches a class's hierarchy for
     * public visibility parent classes with the desired getter. This
     * particular case is required to support AutoValue style data classes,
     * which have abstract public interfaces implemented by package-protected
     * (generated) subclasses.
     */
    private static Method findPubliclyAccessibleMethod(String propertyName, Class<?> root, String methodName, boolean dfeInUse) throws NoSuchMethodException {
        Class<?> currentClass = root;
        while (currentClass != null) {
            String key = mkKey(currentClass, propertyName);
            Method method = METHOD_CACHE.get(key);
            if (method != null) {
                return method;
            }
            if (Modifier.isPublic(currentClass.getModifiers())) {
                if (dfeInUse) {
                    //
                    // try a getter that takes DataFetchingEnvironment first (if we have one)
                    try {
                        method = currentClass.getMethod(methodName, DataFetchingEnvironment.class);
                        if (Modifier.isPublic(method.getModifiers())) {
                            METHOD_CACHE.putIfAbsent(key, method);
                            return method;
                        }
                    } catch (NoSuchMethodException e) {
                        // ok try the next approach
                    }
                }
                method = currentClass.getMethod(methodName);
                if (Modifier.isPublic(method.getModifiers())) {
                    METHOD_CACHE.putIfAbsent(key, method);
                    return method;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return root.getMethod(methodName);
    }

    private static Method findViaSetAccessible(String propertyName, Class<?> aClass, String methodName, boolean dfeInUse) throws NoSuchMethodException {
        if (!USE_SET_ACCESSIBLE.get()) {
            throw new FastNoSuchMethodException(methodName);
        }
        Class<?> currentClass = aClass;
        while (currentClass != null) {
            String key = mkKey(currentClass, propertyName);
            Method method = METHOD_CACHE.get(key);
            if (method != null) {
                return method;
            }

            Predicate<Method> whichMethods = mth -> {
                if (dfeInUse) {
                    return hasZeroArgs(mth) || takesDataFetcherEnvironmentAsOnlyArgument(mth);
                }
                return hasZeroArgs(mth);
            };
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            Optional<Method> m = Arrays.stream(declaredMethods)
                    .filter(mth -> methodName.equals(mth.getName()))
                    .filter(whichMethods)
                    .min(mostMethodArgsFirst());
            if (m.isPresent()) {
                try {
                    // few JVMs actually enforce this but it might happen
                    method = m.get();
                    method.setAccessible(true);
                    METHOD_CACHE.putIfAbsent(key, method);
                    return method;
                } catch (SecurityException ignored) {
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        throw new FastNoSuchMethodException(methodName);
    }

    private static Object getPropertyViaFieldAccess(Object object, String propertyName) throws FastNoSuchMethodException {
        Class<?> aClass = object.getClass();
        String key = mkKey(aClass, propertyName);
        try {
            Field field = FIELD_CACHE.get(key);
            if (field == null) {
                field = aClass.getField(propertyName);
                FIELD_CACHE.putIfAbsent(key, field);
            }
            return field.get(object);
        } catch (NoSuchFieldException e) {
            if (!USE_SET_ACCESSIBLE.get()) {
                throw new FastNoSuchMethodException(key);
            }
            // if not public fields then try via setAccessible
            try {
                Field field = aClass.getDeclaredField(propertyName);
                field.setAccessible(true);
                FIELD_CACHE.putIfAbsent(key, field);
                return field.get(object);
            } catch (SecurityException | NoSuchFieldException ignored2) {
                throw new FastNoSuchMethodException(key);
            } catch (IllegalAccessException e1) {
                throw new GraphQLException(e);
            }
        } catch (IllegalAccessException e) {
            throw new GraphQLException(e);
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private static boolean isBooleanProperty(GraphQLType graphQLType) {
        if (graphQLType == GraphQLBoolean) {
            return true;
        }
        if (isNonNull(graphQLType)) {
            return unwrapOne(graphQLType) == GraphQLBoolean;
        }
        return false;
    }

    public static void clearReflectionCache() {
        METHOD_CACHE.clear();
        FIELD_CACHE.clear();
        NEGATIVE_CACHE.clear();
    }

    public static boolean setUseSetAccessible(boolean flag) {
        return USE_SET_ACCESSIBLE.getAndSet(flag);
    }

    public static boolean setUseNegativeCache(boolean flag) {
        return USE_NEGATIVE_CACHE.getAndSet(flag);
    }

    private static String mkKey(Object object, String propertyName) {
        return mkKey(object.getClass(), propertyName);
    }

    private static String mkKey(Class<?> clazz, String propertyName) {
        return clazz.getName() + "__" + propertyName;
    }

    // by not filling out the stack trace, we gain speed when using the exception as flow control
    private static boolean hasZeroArgs(Method mth) {
        return mth.getParameterCount() == 0;
    }

    private static boolean takesDataFetcherEnvironmentAsOnlyArgument(Method mth) {
        return mth.getParameterCount() == 1 &&
                mth.getParameterTypes()[0].equals(DataFetchingEnvironment.class);
    }

    private static Comparator<? super Method> mostMethodArgsFirst() {
        return Comparator.comparingInt(Method::getParameterCount).reversed();
    }

    @SuppressWarnings("serial")
    private static class FastNoSuchMethodException extends NoSuchMethodException {
        public FastNoSuchMethodException(String methodName) {
            super(methodName);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}