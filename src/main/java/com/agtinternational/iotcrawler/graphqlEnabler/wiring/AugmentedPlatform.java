package com.agtinternational.iotcrawler.graphqlEnabler.wiring;

/*-
 * #%L
 * graphql-enabler
 * %%
 * Copyright (C) 2019 AGT International. Author Pavel Smirnov (psmirnov@agtinternational.com)
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

import com.agtinternational.iotcrawler.core.models.ObservableProperty;
import com.agtinternational.iotcrawler.core.models.Platform;
import com.agtinternational.iotcrawler.core.models.Sensor;
import com.agtinternational.iotcrawler.graphqlEnabler.wiring.IoTCrawlerWiring;
import org.dataloader.DataLoader;

import java.util.ArrayList;
import java.util.List;

public class AugmentedPlatform extends Platform {

    public AugmentedPlatform(String uri) {
        super(uri);
    }

    public AugmentedPlatform(Platform base) {

        super(base.getURI(), base.getModel());
    }

    public String getId(){
        return getURI();
    }

    public String getLabel(){
        return label();
    }

    public String getLocation(){
        return location();
    }

    public Object getHosts(){
        DataLoader loader = IoTCrawlerWiring.dataLoaderRegistry.getDataLoader(Sensor.class.getSimpleName());

        Object hosts = hosts();
        if(hosts==null)
            return null;

        List ids = new ArrayList();

        if(hosts instanceof Iterable)
            ids.addAll((List)hosts);
        else
            ids.add(hosts);

        return loader.loadMany(ids);

    }
}
