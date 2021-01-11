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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.SCHEMAS_FOLDER_NAME;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static org.aksw.commons.util.MapReader.getResourceAsStream;

public class MultipleSchemasWiring {
    static Logger LOGGER = LoggerFactory.getLogger(MultipleSchemasWiring.class);

    public static class Builder{
        public HierarchicalWiring build(){

            //Map<String, String> schemas = readSchemasByReadingFolder();
            Map<String, String> schemas = readSchemasFromResources();

            HierarchicalWiring ret = new HierarchicalWiring();
            ret.setSchemaString(schemas);
            return ret;
        }
    }

    public static Map<String, String> readSchemasFromResources(){

        List<String> files = new ArrayList<>();
        //adding "iotcrawler.graphqls" first!
        files.add(Paths.get(SCHEMAS_FOLDER_NAME, "iotcrawler.graphqls").toString());
        //reading folder and adding other schemas
        try {
            List<String> files2 = IOUtils.readLines(getResourceAsStream("/"+ SCHEMAS_FOLDER_NAME), Charsets.UTF_8);
            for(String file: files2) {
                String filePath = Paths.get(SCHEMAS_FOLDER_NAME, file).toString();
                if (!files.contains(filePath))
                    files.add(filePath);
            }
        }
        catch (Exception e){
            LOGGER.error("Failed to read resources: {}",e.getLocalizedMessage());
        }

        return readSchemasFromResourceFiles(files);

    }

    public static Map<String, String> readSchemasByReadingFolder(){

        List<String> pathsToRead = new ArrayList<>();
        List<String> filePathsToRead = new ArrayList<>();
        //adding default schema first!
        filePathsToRead.add(Paths.get(SCHEMAS_FOLDER_NAME,"iotcrawler.graphqls").toString());

      	//adding others by reading the given folder
        if (Files.isDirectory(Paths.get(SCHEMAS_FOLDER_NAME)))
            try {
                Files.list(Paths.get(SCHEMAS_FOLDER_NAME)).forEach(path -> {
                    filePathsToRead.add(path.toString());
                });
            }
        catch (Exception e){
                LOGGER.error("Failed to list folder {}: {}", SCHEMAS_FOLDER_NAME, e.getLocalizedMessage());
        }
        else
            filePathsToRead.add(SCHEMAS_FOLDER_NAME);


        return readSchemasFromResourceFiles(filePathsToRead);
    }

    private static Map<String, String> readSchemasFromResourceFiles(List<String> files){
        Map<String, String> schemas = new HashMap<>();
        for(String urlStr: files){
            LOGGER.debug("Trying to read schema {}", urlStr);
            String schemaString = null;
            try {
                URL url = Resources.getResource(urlStr);
                schemaString = Resources.toString(url, Charsets.UTF_8);
                schemas.put(urlStr, schemaString);

            } catch (Exception e) {
                LOGGER.error("Failed to read schema {}: {}", urlStr, e.getLocalizedMessage());
            }
        }
        LOGGER.debug("Schemas readed: {}", schemas.size());
        return schemas;
    }

}
