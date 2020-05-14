//package com.agtinternational.iotcrawler.graphqlEnabler.wiring;
//
///*-
// * #%L
// * search-enabler
// * %%
// * Copyright (C) 2019 - 2020 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
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
//import graphql.schema.GraphQLDirective;
//import graphql.schema.GraphQLFieldDefinition;
//import graphql.schema.idl.SchemaDirectiveWiring;
//import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
//
//public class DirectivesWiring implements SchemaDirectiveWiring {
//
//    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment){
//        GraphQLDirective graphQLDirective = environment.getDirective();
//        String uri = graphQLDirective.getArgument("uri").getValue().toString();
//
//        return null;
//    }
//
//
////    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment){
////            return null;
////            }
//
//}
