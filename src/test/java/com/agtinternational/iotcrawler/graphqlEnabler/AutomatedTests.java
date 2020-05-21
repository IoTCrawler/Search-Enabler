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


import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import graphql.GraphQL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

//@RunWith(SpringRunner.class)
//@SpringBootTest
@RunWith(Parameterized.class)
public class AutomatedTests extends TestUtils {
	protected static Logger LOGGER = LoggerFactory.getLogger(AutomatedTests.class);
	private Path queryFilePath;

	GraphQLProvider graphQLProvider;
	GraphQL graphql;
	Context context;
	List<EntityLD> entities = new ArrayList<>();

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

	@Before
	public void init(){
		EnvVariablesSetter.init();
	}

	@Test
	public void executeQueryTest() throws Exception {
		initGraphQL();
		executeQuery(queryFilePath);
	}

}
