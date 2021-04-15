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


import com.agtinternational.iotcrawler.core.Utils;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.fiware.models.NGSILD.Property;
import com.agtinternational.iotcrawler.fiware.models.NGSILD.Relationship;
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

    private static String Const_HasValue = "https://uri.etsi.org/ngsi-ld/hasValue";
    private static String Const_HasObject = "https://uri.etsi.org/ngsi-ld/hasObject";
    private static String Const_Type = "@type";
    private static String Const_Id = "@id";


    public static Object getPropertyValue(String propertyName, Object object, GraphQLType graphQLType) throws Exception {
        return getPropertyValue(propertyName, object, graphQLType, null);
    }

    public static Object getPropertyValue(String propertyName, Object object, GraphQLType fieldType, DataFetchingEnvironment environment) throws Exception {
        if (object == null) {
            return null;
        }
        if (object instanceof Map) {
            return ((Map<?, ?>) object).get(propertyName);
        }

        String key = mkKey(object, propertyName);
        if (isNegativelyCached(key)) {
            return null;
        }
        boolean dfeInUse = environment != null;
        try {

            Object ret = getPropertyViaGetterMethod(object, propertyName, fieldType, (root, methodName) -> findPubliclyAccessibleMethod(propertyName, root, methodName, dfeInUse), environment);
            return ret;
        } catch (NoSuchMethodException ignored) {
            String abc = "123";
        }


        if(ApplicationController.getExtendeDatafetcher()) {
            // PS: Fallback try to get value from object attributes

            EntityLD entity = (EntityLD) object;


            Iterator it = (Iterator) entity.getAttributes().entrySet().iterator();
            while (it.hasNext()) {

                Map.Entry pair = (Map.Entry) it.next();
                String aKey = pair.getKey().toString();
                Property aProp = (pair.getValue() instanceof Property ? (Property) pair.getValue() : null);

                // Only check properties
                if (aKey.contains(propertyName) && aProp != null) {

                    Map<String, Object> aPropAttributes = aProp.getAttributes();
                    if (aPropAttributes.containsKey(Const_HasValue)) {
                        // Property tempProp = (Property) aPropAttributes.get(Const_HasValue);
                        Attribute tempAttr = (Attribute) aPropAttributes.get(Const_HasValue);
                        if (tempAttr != null) // tempProp
                            return tempAttr.getValue(); // tempProp
                    }

                }
            }
        }

        Boolean changed = false;

        if(object instanceof EntityLD) {  //Handling multivalue properties
            object = Arrays.asList(new Object[]{object});
            changed = true;
        }

        List referenceIDs = new ArrayList();
        Object value = null;
        if(object instanceof Iterable) {
            Iterator iterator = ((Iterable) object).iterator();
            while (iterator.hasNext()) {
                Object object0 = iterator.next();
                if (object0 != null)
                    if (object0 instanceof EntityLD) {

//                        try {
//
//                            Object ret = getPropertyViaGetterMethod(object0, propertyName, graphQLType, (root, methodName) -> findPubliclyAccessibleMethod(propertyName, root, methodName, dfeInUse), environment);
//                            value = ret;
//                        } catch (NoSuchMethodException ignored) {
                        //String objectType = Utils.getFragment(((EntityLD) object0).getType());
                        String objectType = environment.getParentType().getName();
                        String propertyNameURI = HierarchicalWiring.findURI(objectType, propertyName);
                        //if (propertyNameURI == null)
                        //Getting parent type Might be problematic!
                        //propertyNameURI = GenericMDRWiring.findURI(environment.getParentType().getName(), propertyName);

                        if (propertyNameURI != null) {
                            if(System.getenv().containsKey(CUT_TYPE_URIS))
                                propertyNameURI = Utils.cutURL(propertyNameURI, (Map)((EntityLD) object0).getContext());
                            Object attribute = ((EntityLD) object0).getAttribute(propertyNameURI);
                            //processing only relations

//                            if (propertyNameURI.startsWith("http://") && attribute == null)
//                                attribute = ((EntityLD) object0).getAttribute(propertyNameURI);
                            if (attribute == null)
                                LOGGER.warn("Attribute " + propertyNameURI + " not found in " + ((EntityLD) object0).getId());
                                //throw new Exception("Attribute " + propertyNameURI + " not found in " + ((EntityLD) object0).getId());
                            else {
                                boolean convertedToList = false;
                                if(!(attribute instanceof Iterable)) {
                                    List list = new ArrayList();
                                    list.add(attribute);
                                    attribute = list;
                                    convertedToList = true;
                                }
                                Iterator iterator1 = ((Iterable)attribute).iterator();
                                while(iterator1.hasNext()){
                                    attribute = iterator1.next();
                                    if (attribute instanceof Attribute) {
                                        value = ((Attribute) attribute).getValue();
//                            if(graphQLType instanceof GraphQLList && !(value instanceof List))
//                                value = Arrays.asList(new Object[]{ value });
                                        if(value!=null) {
                                            if (((Attribute) attribute).getType().get().equals("Relationship")) {
                                                if (value instanceof List)
                                                    referenceIDs.addAll((List) value);
                                                else
                                                    referenceIDs.add(value);
                                            } else {
                                                if (convertedToList)
                                                    return value;
                                                return attribute; //return the whole array instead of one value
                                            }
                                        }
                                        else {
                                            if(ApplicationController.getExtendeDatafetcher()) {
                                                // PS: Fallback, value is empty, check in attributes
                                                Map<String, Object> attributesOfRel = ((Relationship) attribute).getAttributes();

                                                if (attributesOfRel.containsKey(Const_Type)) {

                                                    if (((String) attributesOfRel.get(Const_Type)).contains("Relationship")) {

                                                        if (attributesOfRel.containsKey(Const_HasObject)) {
                                                            Map<String, Object> attributesOfSubRel = ((Map<String, Object>) attributesOfRel.get(Const_HasObject));
                                                            String refID = ((String) attributesOfSubRel.get(Const_Id));
                                                            referenceIDs.add(refID);
                                                        }
                                                    }

                                                }
                                            }

                                        }


                                    } else
                                        throw new NotImplementedException(attribute.getClass().getCanonicalName() + " as attribute type");
                                }
                            }

                        } else
                            LOGGER.warn("No URI found for " + propertyName + " in " + ((EntityLD) object0).getId());
                        //}
                    } else
                        throw new NotImplementedException("");
            }
        }

        if(referenceIDs.size()>0){
            GraphQLType wrappedType = null;
            if (fieldType instanceof GraphQLList)
                wrappedType = ((GraphQLList) fieldType).getWrappedType();
            else if (fieldType instanceof GraphQLNonNull)
                wrappedType = ((GraphQLNonNull) fieldType).getWrappedType();
            else  if (fieldType instanceof GraphQLObjectType)
                wrappedType = fieldType;
            else if(fieldType!=null)
                LOGGER.warn("Skipping fieldType="+fieldType.getClass().getName());

            String propertyType = null;
            if (wrappedType instanceof GraphQLList)
                propertyType = ((GraphQLList) wrappedType).getWrappedType().getName();
            else if (wrappedType instanceof GraphQLObjectType)
                propertyType = ((GraphQLObjectType) wrappedType).getName();
            else if (wrappedType instanceof GraphQLNonNull)
                propertyType = ((GraphQLNonNull) wrappedType).getWrappedType().getName();
            else if(wrappedType!=null)
                LOGGER.warn("Skipping wrappedType="+wrappedType.getClass().getName());

            if (propertyType != null){
                String propertyTypeURI = HierarchicalWiring.findURI(propertyType);
                if (propertyTypeURI != null) {
                    DataLoader loader = HierarchicalWiring.dataLoaderRegistry.getDataLoader(propertyType);
                    if (loader == null)
                        throw new Exception("No data loader for " + propertyType);
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
        }

        return value;
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