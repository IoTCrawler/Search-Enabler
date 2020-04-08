package com.agtinternational.iotcrawler.graphqlEnabler;

import org.junit.Before;
import org.junit.Test;

public class HttpApplicationTest {

//    @Before
//    public void init() throws Exception {
//        EnvVariablesSetter.init();
//
//    }

    public static void main(String[] args) throws Exception {
        EnvVariablesSetter.init();
        HttpApplication.main(new String[]{});
    }

}
