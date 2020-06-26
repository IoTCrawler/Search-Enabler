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
import com.agtinternational.iotcrawler.fiware.models.EntityLD;

//import com.agtinternational.iotcrawler.smartHomeApp.homeActivity.activity.ReadingBookActivity;
//import com.agtinternational.iotcrawler.smartHomeApp.homeActivity.activity.WatchingTVActivity;
//import com.agtinternational.iotcrawler.smartHomeApp.homeActivity.activity.WorkingOnComputerActivity;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestsHomeActivity extends TestUtils {
    protected static Logger LOGGER = LoggerFactory.getLogger(TestsHomeActivity.class);

    @Before
    public void init(){
        EnvVariablesSetter.init();
    }

    @Override
    protected void initGraphQL() throws Exception {
        initGraphQL(Arrays.asList(Paths.get("schemas", "homeActivity.graphqls").toString()));
    }

    protected void initEntities(){
        //entities = createEntities();
        entities = readEntitiesFromFiles(new File("samples","homeActivity"));
    }

//    protected List<EntityLD> createEntities(){
//        boolean cutURIs = false;
//
//        List<EntityLD> entities = new ArrayList<>();
//
//        ReadingBookActivity activity1 = new ReadingBookActivity("urn:reading:book1");
//        WatchingTVActivity activity2 = new WatchingTVActivity("urn:watching:tv1");
//        WorkingOnComputerActivity activity3 = new WorkingOnComputerActivity("urn:watching:tv1");
//
//        try {
//            entities.add(activity1.toEntityLD(cutURIs));
//            entities.add(activity2.toEntityLD(cutURIs));
//            entities.add(activity3.toEntityLD(cutURIs));
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//
//
//        for(EntityLD entityLD: entities){
//            try {
//                Files.write(Paths.get("samples", Utils.getFragment(entityLD.getId().replace(":","-") + ".json")), Utils.prettyPrint(entityLD.toJsonObject()).getBytes());
//            }
//            catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//        return entities;
//    }


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
    public void getActivitiesTest() throws Exception {

        executeQuery(Paths.get("queries","homeActivity","getActivities"));
    }
}
