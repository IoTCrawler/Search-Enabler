package com.agtinternational.iotcrawler.graphqlEnabler;

/*-
 * #%L
 * graphql-enabler
 * %%
 * Copyright (C) 2019 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
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
import com.agtinternational.iotcrawler.fiware.clients.NgsiLDClient;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.agtinternational.iotcrawler.fiware.clients.Constants.NGSILD_BROKER_URL;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class IoTCrawlerProvierTests {
	protected Logger LOGGER = LoggerFactory.getLogger(IoTCrawlerProvierTests.class);


	GraphQLProvider graphQLProvider;
	GraphQL graphql;
	Context context;

	@Before
	public void init() throws Exception {
		EnvVariablesSetter.init();

		LOGGER.info("Initing graphql provider");

		graphQLProvider = new GraphQLProvider(new IoTCrawlerWiring.Builder().build());
		graphQLProvider.init();
		graphql = graphQLProvider.graphQL();

		context = graphQLProvider.getContext();

	}



	private String getQuery(String resourcePath) throws IOException {
		String ret = new String(Files.readAllBytes(Paths.get(resourcePath)));
//		URL url = Resources.getResource(resourcePath);
//		String sdl = Resources.toString(url, Charsets.UTF_8);
		return ret;
	}

    @Test
    @Ignore
    public void registerEntities() throws Exception {
        LOGGER.info("registerEntities()");


        NgsiLDClient ngsiLDClient = new NgsiLDClient(System.getenv(NGSILD_BROKER_URL));
		IoTCrawlerRESTClient ioTCrawlerRESTClient = new IoTCrawlerRESTClient(System.getenv(NGSILD_BROKER_URL));

        List<Path> filesToRead= new ArrayList<>();
        File folder = new File("samples");
        if(folder.exists()) {
            try {
                Files.list(folder.toPath()).forEach(file->{
                    filesToRead.add(file);
                });
            } catch (IOException e) {
                LOGGER.error("Failed to list directory {}", folder.getAbsolutePath());
                e.printStackTrace();
            }
        }

        int exceptions=0;
        for(Path path : filesToRead)
        {
            byte[] modelJson = Files.readAllBytes(path);
            EntityLD entityLD = EntityLD.fromJsonString(new String(modelJson));

//			Boolean cutURIs = (System.getenv().containsKey(CUT_TYPE_URIS)?Boolean.parseBoolean(System.getenv(CUT_TYPE_URIS)):false);
//			if(cutURIs) {
//				 entityLD = RDFModel.fromEntity(entityLD).toEntityLD(cutURIs);
//			}else
//				entityLD.setContext(null);
			try {
				ngsiLDClient.deleteEntitySync(entityLD.getId());
			}
			catch (Exception e){
				LOGGER.error(e.getLocalizedMessage());
			}
			try {
				boolean result = ngsiLDClient.addEntitySync(entityLD);
			}
			catch (Exception e){
				throw e;
			}

        }
//        catch (Exception e){
//			LOGGER.error("Problem with {}: {}", path, e.getLocalizedMessage());
//			exceptions++;
//		}
		Assert.isTrue(exceptions==0);
        LOGGER.info("Entities were registered");
    }

	@Test
	@Ignore
	public void deleteEntities() throws Exception {
		LOGGER.info("deleteEntities()");


		NgsiLDClient ngsiLDClient = new NgsiLDClient(System.getenv(NGSILD_BROKER_URL));
		IoTCrawlerRESTClient ioTCrawlerRESTClient = new IoTCrawlerRESTClient(System.getenv(NGSILD_BROKER_URL));

		List<Path> filesToRead= new ArrayList<>();
		File folder = new File("samples");
		if(folder.exists()) {
			try {
				Files.list(folder.toPath()).forEach(file->{
					filesToRead.add(file);
				});
			} catch (IOException e) {
				LOGGER.error("Failed to list directory {}", folder.getAbsolutePath());
				e.printStackTrace();
			}
		}

		int exceptions=0;
		for(Path path : filesToRead)
		{
			byte[] modelJson = Files.readAllBytes(path);
			EntityLD entityLD = EntityLD.fromJsonString(new String(modelJson));

			try {
				ngsiLDClient.deleteEntitySync(entityLD.getId());
			}
			catch (Exception e){

			}

		}

		Assert.isTrue(exceptions==0);
		LOGGER.info("Entities were registered");
	}

	@Test
	public void getStreamByIdTest() throws Exception {
		String query = getQuery("queries/getStreamById");

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

		Assert.notNull(data.get("stream"));

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll(data);
		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));
	}

//	@Test
//	public void getEntitiesByAttributeValueTest() throws Exception {
//		String query = getQuery("getEntityByAttribute");
//
//		Map<String, Object> variables = new HashMap<>();
//		//variables.put("id", "http://purl.org/iot/ontology/iot-stream#Stream_FIBARO%2520Wall%2520plug%2520living%2520room_CurrentEnergyUse");
//		//variables.put("episode", "http://purl.org/iot/ontology/iot-stream#Stream_FIBARO%2520Wall%2520plug%2520living%2520room_CurrentEnergyUse");
////		variables.put("withFriends", false);
//
//		ExecutionInput executionInput = ExecutionInput.newExecutionInput()
//				.query(query)
//				.variables(variables)
//				.operationName(null)
//				.context(context)
//				.build();
//
//		LOGGER.info("Executing query");
//		ExecutionResult executionResult = graphql.execute(executionInput);
//		Object data = executionResult.getData();
//        Assert.notNull(data);
//		JSONObject jsonObject = new JSONObject();
		//jsonObject.putAll((Map)data);
		//LOGGER.info(Utils.prettyPrint(jsonObject.toString()));
//
//	}

	@Test
	public void getStreamsTest() throws Exception {
		String query = getQuery("queries/getStreams");

		Map<String, Object> variables = new HashMap<>();
		//variables.put("id", "http://purl.org/iot/ontology/iot-stream#Stream_FIBARO%2520Wall%2520plug%2520living%2520room_CurrentEnergyUse");

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

		for(Object result: (List)results)
			Assert.notNull(result);

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll(data);
		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));

	}


	@Test
	public void getSensorByIdTest() throws Exception {
		String query = getQuery("queries/getSensorById");
		Map<String, Object> variables = new HashMap<>();

		ExecutionInput executionInput = ExecutionInput.newExecutionInput()
				.query(query)
				.variables(variables)
				.operationName(null)
				.context(context)
				.build();

		ExecutionResult executionResult = graphql.execute(executionInput);
		Map data = executionResult.getData();
		Assert.notNull(data);

		Object results = ((Map)data).values().iterator().next();
		Assert.notNull(results);

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll(data);
		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));
	}

	@Test
	public void getSensorsTest() throws Exception {
		String query = getQuery("queries/getSensors");

		Map<String, Object> variables = new HashMap<>();
		//variables.put("id", "http://purl.org/iot/ontology/iot-stream#Stream_FIBARO%2520Wall%2520plug%2520living%2520room_CurrentEnergyUse");

		ExecutionInput executionInput = ExecutionInput.newExecutionInput()
				.query(query)
				.variables(variables)
				.operationName(null)
				.context(context)
				.build();

		LOGGER.info("Executing query");
		ExecutionResult executionResult = graphql.execute(executionInput);
		Map data = executionResult.getData();
		Object results = ((Map)data).values().iterator().next();
		Assert.notNull(results);

//		for(Object result: (List)results)
//			Assert.notNull(result);

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll(data);
		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));
	}

	@Test
	public void getPlatformByIdTest() throws Exception {
		String query = getQuery("queries/getPlatformById");

		Map<String, Object> variables = new HashMap<>();

		ExecutionInput executionInput = ExecutionInput.newExecutionInput()
				.query(query)
				.variables(variables)
				.operationName(null)
				.context(context)
				.build();

		ExecutionResult executionResult = graphql.execute(executionInput);
		Map data = executionResult.getData();
		Assert.notNull(data);

		Assert.notNull(data.get("platform"));

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll(data);
		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));
	}

	@Test
	public void getPlatformsTest() throws Exception {
		String query = getQuery("queries/getPlatforms");

		Map<String, Object> variables = new HashMap<>();

		ExecutionInput executionInput = ExecutionInput.newExecutionInput()
				.query(query)
				.variables(variables)
				.operationName(null)
				.context(context)
				.build();

		ExecutionResult executionResult = graphql.execute(executionInput);

		Map data = executionResult.getData();
		Assert.notNull(data);

		List entities = (List)data.get("platforms");
		Assert.isTrue(entities.size()>0);
		LOGGER.info("Platforms: {}", entities.size());

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll((Map)data);

		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));
	}



	@Test
	public void getObservablePropertyByIdTest() throws Exception {
		String query = getQuery("queries/getObservablePropertyById");

		Map<String, Object> variables = new HashMap<>();

		ExecutionInput executionInput = ExecutionInput.newExecutionInput()
				.query(query)
				.variables(variables)
				.operationName(null)
				.context(context)
				.build();

		ExecutionResult executionResult = graphql.execute(executionInput);
		Object data = executionResult.getData();
		Assert.notNull(data);

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll((Map)data);
		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));

	}


	@Test
	public void getObservablePropertiesTest() throws Exception {
		String query = getQuery("queries/getObservableProperties");

		Map<String, Object> variables = new HashMap<>();

		ExecutionInput executionInput = ExecutionInput.newExecutionInput()
				.query(query)
				.variables(variables)
				.operationName(null)
				.context(context)
				.build();

		ExecutionResult executionResult = graphql.execute(executionInput);
		Map data = executionResult.getData();
		Assert.notNull(data);

		List entities = (List)data.get("observableProperties");
		Assert.isTrue(entities.size()>0);
		LOGGER.info("Observable Proeperties: {}", entities.size());

		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll((Map)data);

		LOGGER.info(Utils.prettyPrint(jsonObject.toString()));

	}

}
