package com.agtinternational.iotcrawler.graphqlEnabler;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

public class TestsSmartEnergy  extends TestUtils {
    protected static Logger LOGGER = LoggerFactory.getLogger(TestsSmartEnergy.class);

    @Before
    public void init(){
        EnvVariablesSetter.init();
    }

    @Override
    protected void initGraphQL() throws Exception {
        initGraphQL(Arrays.asList(Paths.get("schemas", "smartEnergy.graphqls").toString()));
    }

    protected void initEntities(){
        //entities = createEntities();
        entities = readEntitiesFromFiles(new File("samples","smartEnergy"));
    }

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
    public void getAreasOfInterestTest() throws Exception {

        executeQuery(Paths.get("queries","smartEnergy","getAreasOfInterest"));
    }

    @Test
    public void getEnergyPlaformsTest() throws Exception {

        executeQuery(Paths.get("queries","smartEnergy","getEnergyPlatforms"));
    }
}
