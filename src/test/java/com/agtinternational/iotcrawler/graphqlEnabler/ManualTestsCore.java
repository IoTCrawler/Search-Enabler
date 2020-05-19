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
import com.agtinternational.iotcrawler.core.clients.IoTCrawlerRESTClient;
import com.agtinternational.iotcrawler.core.models.IoTStream;
import com.agtinternational.iotcrawler.core.models.ObservableProperty;
import com.agtinternational.iotcrawler.core.models.Platform;
import com.agtinternational.iotcrawler.core.models.Sensor;
import com.agtinternational.iotcrawler.fiware.clients.NgsiLDClient;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.agtinternational.iotcrawler.fiware.clients.Constants.NGSILD_BROKER_URL;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class ManualTestsCore extends SchemasTests {
    protected static Logger LOGGER = LoggerFactory.getLogger(AutomatedTests.class);


    @Test
    public void getStreamByIdTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getStreamById"));
    }

    @Test
    public void getStreamsTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getStreams"));
    }

    @Test
    public void getSensorByIdTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getSensorById"));
    }

    @Test
    public void getSensorsTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getSensors"));
    }

    @Test
    public void getTemperatureSensorsTest() throws Exception {
        
        executeQuery(Paths.get("queries","smartConnect","getTemperatureSensors"));
    }

    @Test
    public void getIndoorTemperatureSensorsTest() throws Exception {
        
        executeQuery(Paths.get("queries","smartConnect","getIndoorTemperatureSensors"));
    }


    @Test
    public void getSystemsTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getSystems"));
    }

    @Test
    public void getPlatformByIdTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getSystems"));
    }

    @Test
    public void getPlatformsTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getPlatforms"));
    }



    @Test
    public void getObservablePropertyByIdTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getObservablePropertyById"));
    }


    @Test
    public void getObservablePropertiesTest() throws Exception {
        
        executeQuery(Paths.get("queries","core","getObservableProperties"));
    }

}
