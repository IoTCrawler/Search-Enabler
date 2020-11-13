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

import com.agtinternational.iotcrawler.core.ontologies.IotStream;

import java.util.Arrays;
import java.util.List;

public class Constants {
    //public static final String ALT_TYPE =  "http://search-enabler.iotcrawler/altType";
    public static String GRAPHQL_ENDPOINT_URL = "GRAPHQL_ENDPOINT_URL";
    public static String TRACK_EXECUTION_TIMES = "TRACK_EXECUTION_TIMES";
    public static String[] CORE_TYPES = new String[]{ "IoTStream", "Sensor", "Platform", "ObservableProperty", "StreamObservation" };
}
