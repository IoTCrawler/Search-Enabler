package com.agtinternational.iotcrawler.graphqlEnabler.smartConnect;

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

import com.agtinternational.iotcrawler.core.models.Sensor;
import com.agtinternational.iotcrawler.core.ontologies.IotStream;
import com.agtinternational.iotcrawler.core.ontologies.NGSI_LD;
import org.apache.jena.rdf.model.impl.ResourceImpl;


public class TemperatureSensor extends Sensor {

    public TemperatureSensor(String uri) {
        super(uri);
        namespaces.put("agt", "http://Agt/");
        //namespaces.put("search-enabler", "http://search-enabler.iotcrawler/");

        setProperty(NGSI_LD.alternativeType, new ResourceImpl("http://Agt/TemperatureSensor"));

    }
}
