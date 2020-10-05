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
import com.agtinternational.iotcrawler.core.models.IoTStream;
import com.agtinternational.iotcrawler.core.models.ObservableProperty;
import com.agtinternational.iotcrawler.core.models.Platform;
import com.agtinternational.iotcrawler.core.models.Sensor;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.AutomatedTests;
import com.agtinternational.iotcrawler.graphqlEnabler.EnvVariablesSetter;
import com.agtinternational.iotcrawler.graphqlEnabler.TestUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class TestsCore extends TestUtils {
    protected static Logger LOGGER = LoggerFactory.getLogger(AutomatedTests.class);

    protected void initEntities(){
        entities = createEntities();
        //entities = readEntitiesFromFiles(new File("samples"));
    }

    protected List<EntityLD> createEntities(){
        boolean cutURIs = true;
        Map<String, String[]> sensorsAndProperties = new HashMap<>();
        sensorsAndProperties.put("AEON Labs ZW100 MultiSensor 6", new String[]{ "BatteryLevel", "Brightness", "MotionAlarm", "MotionAlarmCancelationDelay", "RelativeHumidity", "TamperAlarm", "Temperature", "UV" });
        sensorsAndProperties.put("FIBARO System FGWPE/F Wall Plug Gen5", new String[]{"AccumulatedEnergyUse","CurrentEnergyUse"});
        sensorsAndProperties.put("FIBARO Wall plug living room", new String[]{"AccumulatedEnergyUse","CurrentEnergyUse"});

        Map<String, ObservableProperty> observableProperties = new HashMap<>();
        List<EntityLD> entities = new ArrayList<>();

        Platform platform = new Platform("urn:ngsi-ld:Platform_homee_00055110D732", "Platform homee_00055110D732");
        for(String deviceName: sensorsAndProperties.keySet()) {
            for (String propertyName : sensorsAndProperties.get(deviceName)) {
                String propertyId = "urn:ngsi-ld:" + propertyName.replace(" ", "_").replace("/","-");

                if(!observableProperties.containsKey(propertyId)){
                    observableProperties.put(propertyId, new ObservableProperty(propertyId, propertyName));
                }

                ObservableProperty observableProperty = observableProperties.get(propertyId);

                String sensorName = deviceName+" "+propertyName;
                String sensorAndPropertyForId = sensorName.replace(" ", "_").replace("/","-");
                Sensor sensor = new Sensor("urn:ngsi-ld:Sensor_" +sensorAndPropertyForId, sensorName);

                IoTStream ioTStream = new IoTStream("urn:ngsi-ld:Stream_" +sensorAndPropertyForId);
                //StreamObservation streamObservation = new StreamObservation("");

                ioTStream.generatedBy(sensor);

                sensor.observes(observableProperty);
                observableProperty.isObservedBy(sensor);
                platform.hosts(sensor);
                sensor.isHostedBy(platform);

                try {
                    entities.add(ioTStream.toEntityLD(cutURIs));
                    entities.add(sensor.toEntityLD(cutURIs));
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        try {
            entities.add(platform.toEntityLD(cutURIs));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        observableProperties.values().forEach(observableProperty->{
            try {
                entities.add(observableProperty.toEntityLD(cutURIs));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        });

        for(EntityLD entityLD: entities){
            try {
                Files.write(Paths.get("samples", Utils.getFragment(entityLD.getId().replace(":","-") + ".json")), Utils.prettyPrint(entityLD.toJsonObject()).getBytes());
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        return entities;
    }

    @Before
    public void init(){
        EnvVariablesSetter.init();
    }

    @Test
    @Ignore
    public void registerEntities() throws Exception {
        initEntities();
        super.registerEntities();
    }

    @Test
    @Ignore
    public void deleteEntities() throws Exception {
        initEntities();
        super.deleteEntities();
    }


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
        
        executeQuery(Paths.get("queries","core","getPlatformById"));
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

    @Test
    public void getStreamObservationsTest() throws Exception {

        executeQuery(Paths.get("queries","core","getStreamObservations"));
    }
}
