package com.agtinternational.iotcrawler.graphqlEnabler.wiring;

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


import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class IoTCrawlerWiring{
    static Logger LOGGER = LoggerFactory.getLogger(IoTCrawlerWiring.class);

    public static class Builder{
        public GenericMDRWiring build(){
            String folderName = "schemas/";
            List<String> files = new ArrayList<>();
            files.add(folderName+"iotcrawler.graphqls");
            try {
                List<String> files2 = IOUtils.readLines(this.getClass().getClassLoader().getResourceAsStream(folderName), Charsets.UTF_8);
                for(String file: files2)
                    if(!files.contains(file))
                        files.add(folderName+file);
            }
            catch (Exception e){
                LOGGER.error("Failed to read resources");
            }

            Map<String, String> schemasStrings = new HashMap<>();
            for(String urlStr: files) {
                LOGGER.debug("Trying to read schema {}", urlStr);
                String schemaString = null;
                try {
                    URL url = Resources.getResource(urlStr);
                    schemaString = Resources.toString(url, Charsets.UTF_8);
                    schemasStrings.put(urlStr, schemaString);

                } catch (Exception e) {
                    LOGGER.error("Failed to read schema {}: {}", urlStr, e.getLocalizedMessage());
                }
            }
            LOGGER.debug("Schemas readed: {}", schemasStrings.size());
            GenericMDRWiring ret = new GenericMDRWiring();
            ret.setSchemaString(schemasStrings);
            return ret;
        }
    }
}
