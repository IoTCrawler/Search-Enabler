//package com.agtinternational.iotcrawler.graphqlEnabler;
//
//import com.agtinternational.iotcrawler.fiware.models.EntityLD;
//import graphql.ExecutionInput;
//import graphql.ExecutionResult;
//import graphql.GraphQL;
//import net.minidev.json.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.util.Assert;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class TestUtils {
//    protected static Logger LOGGER = LoggerFactory.getLogger(Utils.class);
//
//    public static String readQuery(Path resourcePath) throws IOException {
//        String ret = new String(Files.readAllBytes(resourcePath));
//        return ret;
//    }
//
//    protected List<EntityLD> readEntitiesFromFiles(){
//        List<EntityLD> ret = new ArrayList<>();
//        List<Path> filesToRead= new ArrayList<>();
//        File folder = new File("samples");
//        if(folder.exists()) {
//            try {
//                Files.list(folder.toPath()).forEach(file->{
//                    filesToRead.add(file);
//                });
//            } catch (IOException e) {
//                LOGGER.error("Failed to list directory {}", folder.getAbsolutePath());
//                e.printStackTrace();
//            }
//        }
//
//        for(Path path : filesToRead){
//            byte[] modelJson = null;
//            try {
//                modelJson = Files.readAllBytes(path);
//            }
//            catch (Exception e){
//                LOGGER.error("Failed to read file {}: {}", path, e.getLocalizedMessage());
//                continue;
//            }
//            try {
//                EntityLD entityLD = EntityLD.fromJsonString(new String(modelJson));
//                ret.add(entityLD);
//            }
//            catch (Exception e){
//                LOGGER.error("Failed to parse entity from file {}: {}", path, e.getLocalizedMessage());
//            }
//        }
//        return ret;
//    }
//
//    public static void executeQuery(Path filePath, GraphQL graphql, Object context) throws IOException {
//        LOGGER.info("Executing {}", filePath);
//        String query = readQuery(filePath);
//        Map<String, Object> variables = new HashMap<>();
//        //variables.put("id", "iotc:Stream_1");
//        //variables.put("episode", "http://purl.org/iot/ontology/iot-stream#Stream_FIBARO%2520Wall%2520plug%2520living%2520room_CurrentEnergyUse");
////		variables.put("withFriends", false);
//
//        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
//                .query(query)
//                .variables(variables)
//                .operationName(null)
//                .context(context)
//                .build();
//
//        LOGGER.info("Executing query");
//        ExecutionResult executionResult = graphql.execute(executionInput);
//        Map data = executionResult.getData();
//        Assert.notNull(data);
//        Object results = ((Map)data).values().iterator().next();
//        Assert.notNull(results);
//
////		for(Object result: (List)results)
////			Assert.notNull(result);
//
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.putAll(data);
//        LOGGER.info(com.agtinternational.iotcrawler.core.Utils.prettyPrint(jsonObject.toString()));
//    }
//
//}
