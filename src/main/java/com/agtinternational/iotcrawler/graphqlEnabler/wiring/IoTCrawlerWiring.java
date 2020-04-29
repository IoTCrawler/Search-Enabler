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
            return ret;
        }
    }
}
