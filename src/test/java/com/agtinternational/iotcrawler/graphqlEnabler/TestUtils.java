package com.agtinternational.iotcrawler.graphqlEnabler;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TestUtils {
    protected static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static String readQuery(Path resourcePath) throws IOException {
        String ret = new String(Files.readAllBytes(resourcePath));
        return ret;
    }

    public static void executeQuery(Path filePath, GraphQL graphql, Object context) throws IOException {
        LOGGER.info("Executing {}", filePath);
        String query = readQuery(filePath);
        Map<String, Object> variables = new HashMap<>();
        //variables.put("id", "iotc:Stream_1");
        //variables.put("episode", "http://purl.org/iot/ontology/iot-stream#Stream_FIBARO%2520Wall%2520plug%2520living%2520room_CurrentEnergyUse");
//		variables.put("withFriends", false);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .operationName(null)
                .context(context)
                .build();

        LOGGER.info("Executing query");
        ExecutionResult executionResult = graphql.execute(executionInput);
        Map data = executionResult.getData();
        Assert.notNull(data);
        Object results = ((Map)data).values().iterator().next();
        Assert.notNull(results);

//		for(Object result: (List)results)
//			Assert.notNull(result);

        JSONObject jsonObject = new JSONObject();
        jsonObject.putAll(data);
        LOGGER.info(com.agtinternational.iotcrawler.core.Utils.prettyPrint(jsonObject.toString()));
    }

}
