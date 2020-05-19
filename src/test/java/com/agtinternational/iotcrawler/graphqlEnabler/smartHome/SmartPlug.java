package com.agtinternational.iotcrawler.graphqlEnabler.smartHome;

import com.agtinternational.iotcrawler.core.models.Sensor;
import com.agtinternational.iotcrawler.core.ontologies.NGSI_LD;
import org.apache.jena.rdf.model.impl.ResourceImpl;

public class SmartPlug extends Sensor {

    public SmartPlug(String uri) {
        super(uri);
        namespaces.put("agt", "http://Agt/");
        //namespaces.put("search-enabler", "http://search-enabler.iotcrawler/");

        setProperty(NGSI_LD.alternativeType, new ResourceImpl("http://Agt/TemperatureSensor"));

    }
}
