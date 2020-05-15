package com.agtinternational.iotcrawler.graphqlEnabler.smartConnect;

import com.agtinternational.iotcrawler.core.Utils;
import com.agtinternational.iotcrawler.core.models.Platform;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;

import com.agtinternational.iotcrawler.graphqlEnabler.SchemasTests;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import net.minidev.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.Assert;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartConnectTests extends SchemasTests {


    protected List<EntityLD> getEntities(){
        return readEntitiesFromFiles();
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
    public void registerEntities() throws Exception {
        entities = createEntities();
        super.registerEntities();
    }

    @Ignore
    public void deleteEntities() throws Exception {
        entities = createEntities();
        super.deleteEntities();
    }

    @Test
    public void getTemperatureSensors() throws Exception {
        initGraphQL(Paths.get("schemas","smartConnect.graphqls"));

        String query = readQuery(Paths.get("queries","smartConnect","getTemperatureSensors"));

        Map<String, Object> variables = new HashMap<>();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .operationName(null)
                .context(context)
                .build();

        LOGGER.info("Executing query");
        ExecutionResult executionResult = graphql.execute(executionInput);
        Object data = executionResult.getData();
        Assert.notNull(((Map)data).values().iterator().next());
        JSONObject jsonObject = new JSONObject();
        jsonObject.putAll((Map)data);
        LOGGER.info(Utils.prettyPrint(jsonObject.toString()));
    }

}
