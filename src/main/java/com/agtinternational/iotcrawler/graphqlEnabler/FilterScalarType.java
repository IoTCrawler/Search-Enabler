//package com.agtinternational.iotcrawler.graphqlEnabler;
//
///*-
// * #%L
// * graphql-enabler
// * %%
// * Copyright (C) 2019 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
// * %%
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * #L%
// */
//
//import com.agtinternational.iotcrawler.core.models.Sensor;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.google.gson.JsonPrimitive;
//import graphql.language.Directive;
//import graphql.language.ObjectField;
//import graphql.language.ObjectValue;
//import graphql.language.StringValue;
//import graphql.schema.*;
//
//import java.util.Arrays;
//import java.util.UUID;
//
//public class FilterScalarType extends GraphQLScalarType {
//
//    public FilterScalarType() {
//        super("sensor", "FilterScalarType", new Coercing<JsonElement,String>() {
//
//            @Override
//            public String serialize(Object o) throws CoercingSerializeException {
//                return null;
//            }
//
//            @Override
//            public JsonElement parseValue(Object o) throws CoercingParseValueException {
//                return null;
//            }
//
//            @Override
//            public JsonElement parseLiteral(Object o) throws CoercingParseLiteralException {
//                return parse(o);
//            }
//        });//, Arrays.asList(new GraphQLDirective[0]{ new GraphQLDirective(null, null, null, null, ); }), null );
//
//
//    }
//
//    private static JsonElement parse(Object o){
//        if(o instanceof StringValue)
//            return new JsonPrimitive(((StringValue)o).getValue());
//
//        if(o instanceof ObjectValue) {
//            JsonObject jsonObject = new JsonObject();
//            for(ObjectField objectField :((ObjectValue) o).getObjectFields()){
//                JsonElement value = parse(objectField.getValue());
//                jsonObject.add(objectField.getName(), value);
//            }
//            return jsonObject;
//        }
//        return null;
//    }
//
//}
