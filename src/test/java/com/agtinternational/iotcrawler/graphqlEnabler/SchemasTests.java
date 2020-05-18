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

import com.agtinternational.iotcrawler.core.clients.IoTCrawlerRESTClient;
import com.agtinternational.iotcrawler.fiware.clients.NgsiLDClient;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.GenericMDRWiring;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.agtinternational.iotcrawler.fiware.clients.Constants.NGSILD_BROKER_URL;


public class SchemasTests {
	protected static Logger LOGGER = LoggerFactory.getLogger(SchemasTests.class);

	protected GraphQLProvider graphQLProvider;
	protected GraphQL graphql;
	protected Context context;
	protected List<EntityLD> entities = new ArrayList<>();

	@Before
	public void init() throws Exception {
		EnvVariablesSetter.init();

		graphQLProvider = new GraphQLProvider(new IoTCrawlerWiring.Builder().build());
		graphQLProvider.init();
		graphql = graphQLProvider.graphQL();

		context = graphQLProvider.getContext();

	}

	protected void initGraphQL() throws Exception {
		initGraphQL(null);
	}

	protected void initGraphQL(Path path0) throws Exception {
		Map<String, String> schemas = new HashMap<>();
		List<Path> files = new ArrayList<>();
		files.add(Paths.get("schemas","iotcrawler.graphqls"));

		if(path0!=null) {
			if (Files.isDirectory(path0))
				Files.list(path0).forEach(path -> {
					files.add(path);
				});
			else
				files.add(path0);
		}

		for(Path path: files) {
			String schemaString = null;
			try {
				schemaString = new String(Files.readAllBytes(path));
			} catch (IOException e) {
				LOGGER.error("Failed to read {}", path);
			}
			schemas.put(path.getFileName().toString(), schemaString);
		}


		GenericMDRWiring wiring = new GenericMDRWiring();
		wiring.setSchemaString(schemas);

		LOGGER.info("Initing graphql provider");

		graphQLProvider = new GraphQLProvider(wiring);
		graphQLProvider.init();
		graphql = graphQLProvider.graphQL();

		context = graphQLProvider.getContext();
	}

	public static String readQuery(Path resourcePath) throws IOException {
		String ret = new String(Files.readAllBytes(resourcePath));
		return ret;
	}

	protected List<EntityLD> readEntitiesFromFiles(){
		List<EntityLD> ret = new ArrayList<>();
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

		for(Path path : filesToRead){
			byte[] modelJson = null;
			try {
				modelJson = Files.readAllBytes(path);
			}
			catch (Exception e){
				LOGGER.error("Failed to read file {}: {}", path, e.getLocalizedMessage());
				continue;
			}
			try {
				EntityLD entityLD = EntityLD.fromJsonString(new String(modelJson));
				ret.add(entityLD);
			}
			catch (Exception e){
				LOGGER.error("Failed to parse entity from file {}: {}", path, e.getLocalizedMessage());
			}
		}
		return ret;
	}

	public void executeQuery(Path filePath) throws IOException {
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

    @Test
    @Ignore
	public void registerEntities() throws Exception {
		LOGGER.info("registerEntities()");

		List<Exception> exceptions = new ArrayList<>();
		NgsiLDClient ngsiLDClient = new NgsiLDClient(System.getenv(NGSILD_BROKER_URL));
		for(EntityLD entityLD : entities){
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
				exceptions.add(new Exception("Problem with "+entityLD.getId()+": "+ e.getLocalizedMessage()));
			}

		}

		for(Exception e: exceptions)
			e.printStackTrace();

		Assert.isTrue(exceptions.size()==0);
		LOGGER.info("Entities were registered");
	}

	@Test
	@Ignore
	public void deleteEntities() throws Exception {
		LOGGER.info("deleteEntities()");

		NgsiLDClient ngsiLDClient = new NgsiLDClient(System.getenv(NGSILD_BROKER_URL));
		IoTCrawlerRESTClient ioTCrawlerRESTClient = new IoTCrawlerRESTClient(System.getenv(NGSILD_BROKER_URL));

		int exceptions=0;
		for(EntityLD entityLD : entities)
		{
			try {
				ngsiLDClient.deleteEntitySync(entityLD.getId());
			}
			catch (Exception e){
				LOGGER.error("Failed to delete entitiy {} from json: {}", entityLD.getId(), e.getLocalizedMessage());
			}

		}

		Assert.isTrue(exceptions==0);
		LOGGER.info("Entities were registered");
	}





}