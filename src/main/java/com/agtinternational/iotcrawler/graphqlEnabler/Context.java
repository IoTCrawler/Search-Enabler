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

import org.apache.jena.vocabulary.RDFS;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
public class Context {

    final DataLoaderRegistry dataLoaderRegistry;

    Context(DataLoaderRegistry dataLoaderRegistry) {

        this.dataLoaderRegistry = dataLoaderRegistry;
    }

    public DataLoader<String, Object> getLoader(String key) {
        return dataLoaderRegistry.getDataLoader(key);
    }

    public DataLoader<String, Object> getDefaultDataLoader() {
        return dataLoaderRegistry.getDataLoader("default");
    }

}
