package com.agtinternational.iotcrawler.graphqlEnabler.smartHome;

import com.agtinternational.iotcrawler.core.models.Platform;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.EnvVariablesSetter;
import com.agtinternational.iotcrawler.graphqlEnabler.TestUtils;
import com.agtinternational.iotcrawler.graphqlEnabler.smartConnect.IndoorTemperatureSensor;
import com.agtinternational.iotcrawler.graphqlEnabler.smartConnect.TemperatureSensor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestsSmartHome extends TestUtils {

    @Before
    public void init(){
        EnvVariablesSetter.init();
    }

    @Override
    protected void initGraphQL() throws Exception {

        String[] paths = new String[]{
                //Paths.get("schemas","smartConnect.graphqls").toString(),
                Paths.get("schemas","homeActivity.graphqls").toString(),
                Paths.get("schemas","smartHome.graphqls").toString()
        };
        initGraphQL(Arrays.asList(paths));
    }

    protected void initEntities(){
        entities = createEntities();
        //entities = readEntitiesFromFiles(new File("samples"));
    }

    private List<EntityLD> createEntities(){
        List<EntityLD> ret = new ArrayList<>();

        Platform platform = new Platform("urn:ngsi-ld:Platform_homee_1", "Platform homee_1");
        TemperatureSensor temperatureSensor = new TemperatureSensor("urn:ngsi-ld:TemperatureSensor_1");
        temperatureSensor.isHostedBy(platform);

        IndoorTemperatureSensor indoorTemperatureSensor = new IndoorTemperatureSensor("urn:ngsi-ld:IndoorTemperatureSensor_1");
        indoorTemperatureSensor.isHostedBy(platform);

        platform.hosts(temperatureSensor);
        platform.hosts(indoorTemperatureSensor);

        try {
            boolean cutURIs=false;
            ret.add(platform.toEntityLD(cutURIs));
            ret.add(temperatureSensor.toEntityLD(cutURIs));
            ret.add(indoorTemperatureSensor.toEntityLD(cutURIs));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    @Ignore
    @Test
    public void registerEntities() throws Exception {
        initEntities();
        super.registerEntities();
    }

    @Ignore
    @Test
    public void deleteEntities() throws Exception {
        initEntities();
        super.deleteEntities();
    }

    @Test
    public void getElectricityMeters() throws Exception {
        executeQuery(Paths.get("queries","smartHome","getElectricityMeters"));
    }

    @Test
    public void getHouseholdStateObservations() throws Exception {
        executeQuery(Paths.get("queries","smartHome","getHouseholdStateObservations"));
    }

}