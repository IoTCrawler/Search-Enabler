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

import com.agtinternational.iotcrawler.graphqlEnabler.resolving.NGSILD_Client_Wrapper;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContextProvider {

    final DataLoaderRegistry dataLoaderRegistry;

    @Autowired
    public ContextProvider(DataLoaderRegistry dataLoaderRegistry) {

        this.dataLoaderRegistry = dataLoaderRegistry;
    }

    public Context newContext() {
        NGSILD_Client_Wrapper.resetTotalQueryExectionTime();
        NGSILD_Client_Wrapper.resetTotalQueriesPerformed();
        for(String key: dataLoaderRegistry.getKeys()) {
            DataLoader dataLoader = dataLoaderRegistry.getDataLoader(key);
            dataLoader.clearAll();
        }
        return new Context(dataLoaderRegistry);
    }

}
