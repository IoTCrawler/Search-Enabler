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


import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;

import java.util.Collections;

import static com.agtinternational.iotcrawler.core.Constants.IOTCRAWLER_ORCHESTRATOR_URL;

public class HttpApplicationTest {

//    @Before
//    public void init() throws Exception {
//        EnvVariablesSetter.init();
//
//    }

    public static void main(String[] args) throws Exception {
        EnvVariablesSetter.init();
        if (!System.getenv().containsKey(IOTCRAWLER_ORCHESTRATOR_URL))
            throw new Exception("IOTCRAWLER_ORCHESTRATOR_URL not specified");

        SpringApplication app = new SpringApplication(HttpApplication.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "8081"));
        app.run(args);
    }

}
