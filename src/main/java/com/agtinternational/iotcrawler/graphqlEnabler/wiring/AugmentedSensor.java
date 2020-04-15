//package com.agtinternational.iotcrawler.graphqlEnabler.wiring;
//
///*-
// * #%L
// * graphql-enabler
// * %%
// * Copyright (C) 2019 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
// * %%
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * #L%
// */
//
//import com.agtinternational.iotcrawler.core.models.IoTStream;
//import com.agtinternational.iotcrawler.core.models.ObservableProperty;
//import com.agtinternational.iotcrawler.core.models.Platform;
//import com.agtinternational.iotcrawler.core.models.Sensor;
//import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
//import org.dataloader.DataLoader;
//
//public class AugmentedSensor extends Sensor {
//
//    public AugmentedSensor(String uri) {
//        super(uri);
//    }
//
//    public AugmentedSensor(Sensor base) {
//        super(base.getURI(), base.getModel());
//    }
//
//    public String getId(){
//        return getURI();
//    }
//
//    public String getLabel(){
//        return label();
//    }
//
//    public Object getIsHostedBy() {
//        DataLoader loader = IoTCrawlerWiring.dataLoaderRegistry.getDataLoader(Platform.class.getSimpleName());
//        String platformId = (String)isHostedBy();
//        return loader.load(platformId);
//
//    }
//
//    public Object getObserves() {
//        DataLoader loader = IoTCrawlerWiring.dataLoaderRegistry.getDataLoader(ObservableProperty.class.getSimpleName());
//        return loader.load(observes().toString());
//
//    }
//}
