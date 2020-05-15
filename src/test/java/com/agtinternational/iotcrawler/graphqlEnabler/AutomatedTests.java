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
import com.agtinternational.iotcrawler.core.models.*;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
@RunWith(Parameterized.class)
public class AutomatedTests {
	protected static Logger LOGGER = LoggerFactory.getLogger(AutomatedTests.class);
	private Path queryFilePath;

	GraphQLProvider graphQLProvider;
	GraphQL graphql;
	Context context;
	List<EntityLD> entities = new ArrayList<>();

	@Before
	public void init() throws Exception {
		EnvVariablesSetter.init();

		LOGGER.info("Initing graphql provider");

		graphQLProvider = new GraphQLProvider(new IoTCrawlerWiring.Builder().build());
		graphQLProvider.init();
		graphql = graphQLProvider.graphQL();

		context = graphQLProvider.getContext();
		//entities = readEntitiesFileFiles();

	}

	@Parameterized.Parameters
	public static Collection parameters() throws Exception {

		List<Object[]> objects = new ArrayList<>();
		File folder = new File("queries/core");
		if(folder.exists()) {
			Files.list(folder.toPath()).forEach(file->{
				objects.add(new Object[]{ file });
			});
		}
		return objects;

	}

	public AutomatedTests(Path queryFilePath){
		this.queryFilePath = queryFilePath;
	}

	@Test
	public void executeQueryTest() throws Exception {
		TestUtils.executeQuery(queryFilePath, graphql, context);
	}

}
