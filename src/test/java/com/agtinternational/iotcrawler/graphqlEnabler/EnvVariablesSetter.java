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


import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.agtinternational.iotcrawler.core.Constants.*;
import static com.agtinternational.iotcrawler.fiware.clients.Constants.NGSILD_BROKER_URL;

public class EnvVariablesSetter {

    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    public static void init(){

        //environmentVariables.set(CUT_TYPE_URIS, "true");
        //String brokerURL = "http://155.54.95.248:9090/ngsi-ld/";
        String brokerURL = "http://155.54.95.171:9090/ngsi-ld/";
        //String brokerURL = "http://10.67.1.107:9090/ngsi-ld/";
        //String brokerURL = "http://localhost:3001/ngsi-ld/";
        //String brokerURL = "http://i5-nuc:9090/ngsi-ld/";
        //String brokerURL = "http://192.168.178.26:9090/ngsi-ld/";

        if(!System.getenv().containsKey(NGSILD_BROKER_URL))
            environmentVariables.set(NGSILD_BROKER_URL, brokerURL);

        if(!System.getenv().containsKey(IOTCRAWLER_ORCHESTRATOR_URL))
            environmentVariables.set(IOTCRAWLER_ORCHESTRATOR_URL,brokerURL);

    }
}
