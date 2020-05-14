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


import com.agtinternational.iotcrawler.core.clients.IoTCrawlerRESTClient;
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
public class ManualTests {
    protected static Logger LOGGER = LoggerFactory.getLogger(AutomatedTests.class);
    private Path queryFilePath;

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
        TestUtils.executeQuery(Paths.get("queries","core","getStreamById"), graphql, context);
    }

    @Test
    public void getStreamsTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getStreams"), graphql, context);
    }

    @Test
    public void getSensorByIdTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getSensorById"), graphql, context);
    }

    @Test
    public void getSensorsTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getSensors"), graphql, context);
    }

    @Test
    public void getTemperatureSensorsTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","smartConnect","getTemperatureSensors"), graphql, context);
    }

    @Test
    public void getIndoorTemperatureSensorsTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","smartConnect","getIndoorTemperatureSensors"), graphql, context);
    }


    @Test
    public void getSystemsTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getSystems"), graphql, context);
    }

    @Test
    public void getPlatformByIdTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getSystems"), graphql, context);
    }

    @Test
    public void getPlatformsTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getPlatforms"), graphql, context);
    }



    @Test
    public void getObservablePropertyByIdTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getObservablePropertyById"), graphql, context);
    }


    @Test
    public void getObservablePropertiesTest() throws Exception {
        TestUtils.executeQuery(Paths.get("queries","core","getObservableProperties"), graphql, context);
    }

}
