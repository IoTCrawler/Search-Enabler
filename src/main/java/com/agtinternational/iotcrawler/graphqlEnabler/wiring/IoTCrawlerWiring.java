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

import com.agtinternational.iotcrawler.core.models.IoTStream;
import com.agtinternational.iotcrawler.core.models.ObservableProperty;
import com.agtinternational.iotcrawler.core.models.Platform;
import com.agtinternational.iotcrawler.core.models.Sensor;
import com.agtinternational.iotcrawler.core.ontologies.IotStream;
import com.agtinternational.iotcrawler.core.ontologies.SOSA;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import org.apache.jena.vocabulary.RDFS;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.agtinternational.iotcrawler.core.Constants.CUT_TYPE_URIS;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class IoTCrawlerWiring{

    public static class Builder{
        public GenericMDRWiring build(){
            Boolean cutURIs = (System.getenv().containsKey(CUT_TYPE_URIS)?Boolean.parseBoolean(System.getenv(CUT_TYPE_URIS)):false);
            String[] dataLoaderConcepts = new String[]{ "IoTStream", "Sensor", "Platform", "ObservableProperty" };

//            RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
//                    .type(newTypeWiring("Query")
//                            .dataFetcher("stream",  GenericMDRWiring.genericDataFetcher("IoTStream", false))
//                            .dataFetcher("streams", GenericMDRWiring.genericDataFetcher("IoTStream", false))
//                            .dataFetcher("sensor", GenericMDRWiring.genericDataFetcher("Sensor", false))
//                            .dataFetcher("sensors", GenericMDRWiring.genericDataFetcher("Sensor", false))
//                            .dataFetcher("platforms", GenericMDRWiring.genericDataFetcher("Platform", false))
//                            .dataFetcher("platform", GenericMDRWiring.genericDataFetcher("Platform", false))
//                            .dataFetcher("observableProperty", GenericMDRWiring.genericDataFetcher("ObservableProperty", false))
//                            .dataFetcher("observableProperties", GenericMDRWiring.genericDataFetcher("ObservableProperty", false))
//                            .dataFetcher("entities", GenericMDRWiring.genericDataFetcher("EntityLD", false))
//
//                            .dataFetcher("homeState", GenericMDRWiring.genericDataFetcher("HomeState", false))
//                            .dataFetcher("homeStates", GenericMDRWiring.genericDataFetcher("HomeState", false))
//                            .dataFetcher("activity", GenericMDRWiring.genericDataFetcher("Activity", false))
//                            .dataFetcher("activities", GenericMDRWiring.genericDataFetcher("Activity", false))
//
//                    )
//                    .scalar(ExtendedScalars.Object);



            String schemaString = null;
            try {
                URL url = Resources.getResource("iotcrawler.graphqls");
                schemaString = Resources.toString(url, Charsets.UTF_8);

            }
            catch (Exception e){
                e.printStackTrace();
            }
            GenericMDRWiring ret = new GenericMDRWiring();
            ret.setSchemaString(schemaString);
            //ret.setRuntimeWiringBuilder(runtimeWiringBuilder);
            //ret.setBindingRegistry(bindingRegistry);
//
//            for(String concept: dataLoaderConcepts)
//                ret.registerDataloaderConcept(concept);

            return ret;
        }
    }
}
