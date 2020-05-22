package com.agtinternational.iotcrawler.graphqlEnabler;

import com.agtinternational.iotcrawler.core.Utils;
import com.agtinternational.iotcrawler.fiware.models.EntityLD;
import com.agtinternational.iotcrawler.graphqlEnabler.EnvVariablesSetter;
import com.agtinternational.iotcrawler.graphqlEnabler.TestUtils;

import com.agtinternational.iotcrawler.graphqlEnabler.TestsHomeActivity;
import com.agtinternational.iotcrawler.smartHomeApp.homeActivity.activity.ReadingBookActivity;
import com.agtinternational.iotcrawler.smartHomeApp.homeActivity.activity.WatchingTVActivity;
import com.agtinternational.iotcrawler.smartHomeApp.homeActivity.activity.WorkingOnComputerActivity;
import com.agtinternational.iotcrawler.smartHomeApp.models.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestsSmartHome extends TestUtils {
    protected static Logger LOGGER = LoggerFactory.getLogger(TestsHomeActivity.class);

    @Before
    public void init(){
        EnvVariablesSetter.init();
    }

    @Override
    protected void initGraphQL() throws Exception {
        List<String> paths = new ArrayList<>();
        paths.add(Paths.get("schemas", "homeActivity.graphqls").toString());
        paths.add(Paths.get("schemas", "smartHome.graphqls").toString());

        initGraphQL(paths);
    }

    protected void initEntities(){
        entities = createEntities();
        //entities = readEntitiesFromFiles(new File("samples"));
    }

    protected List<EntityLD> createEntities(){
        boolean cutURIs = false;

        List<EntityLD> entities = new ArrayList<>();

        Household houseHold1 = new Household("urn:household1");

        HouseholdStateStream householdStateStream = new HouseholdStateStream("urn:household1:stateStream");
        householdStateStream.observes(houseHold1);

        HouseholdStateObservation householdStateObservation = new HouseholdStateObservation("urn:household1:stateObservation");
        householdStateObservation.belongsTo(householdStateStream);

        WatchingTVActivity watchingTVActivity = new WatchingTVActivity("urn:watching:tv1");
        ReadingBookActivity readingBookActivity = new ReadingBookActivity("urn:reading:book1");
        WorkingOnComputerActivity workingOnComputerActivity = new WorkingOnComputerActivity("urn:working:on:computer1");

        householdStateObservation.addActivity(watchingTVActivity);
        householdStateObservation.addActivity(readingBookActivity);
        householdStateObservation.addActivity(workingOnComputerActivity);

        householdStateObservation.removeActivity(workingOnComputerActivity);

        try {
            entities.add(houseHold1.toEntityLD(cutURIs));
            entities.add(householdStateStream.toEntityLD(cutURIs));
            entities.add(householdStateObservation.toEntityLD(cutURIs));
        }
        catch (Exception e){
            e.printStackTrace();
        }


        for(EntityLD entityLD: entities){
            try {
                Files.write(Paths.get("samples", Utils.getFragment(entityLD.getId().replace(":","-") + ".json")), Utils.prettyPrint(entityLD.toJsonObject()).getBytes());
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        return entities;
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
    public void getHouseholds() throws Exception {

        executeQuery(Paths.get("queries","smartHome","getHouseholds"));
    }

    @Test
    public void getHouseholdStateStreams() throws Exception {

        executeQuery(Paths.get("queries","smartHome","getHouseholdStateStreams"));
    }

    @Test
    public void getHouseholdStateObservations() throws Exception {

        executeQuery(Paths.get("queries","smartHome","getHouseholdStateObservations"));
    }

    @Test
    public void getActivities() throws Exception {

        executeQuery(Paths.get("queries","smartHome","getActivities"));
    }

    @Test
    public void getAppliances() throws Exception {
        executeQuery(Paths.get("queries","smartHome","getAppliances"));
    }
}

