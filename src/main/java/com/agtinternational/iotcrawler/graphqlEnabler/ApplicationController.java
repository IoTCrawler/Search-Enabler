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


import com.agtinternational.iotcrawler.graphqlEnabler.fetching.QueryExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.agtinternational.iotcrawler.graphqlEnabler.Constants.TRACK_EXECUTION_TIMES;

//@RestController
@Controller
public class ApplicationController {

    static Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);
    private final GraphQL graphql;
    private final ObjectMapper objectMapper;
    private final ContextProvider contextProvider;


    @Autowired
    public ApplicationController(GraphQL graphql, ObjectMapper objectMapper, ContextProvider contextProvider) {
        this.graphql = graphql;
        this.objectMapper = objectMapper;
        this.contextProvider = contextProvider;
    }

    @RequestMapping(value = "/version", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public void index(HttpServletResponse httpServletResponse) throws Exception {
        String response =  (System.getenv().containsKey("VERSION")?"Version:"+System.getenv().get("VERSION"):"Version not set");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        String body = objectMapper.writeValueAsString(response);
        PrintWriter writer = httpServletResponse.getWriter();
        writer.write(body);
        writer.close();
    }


    @RequestMapping(value = "/graphql", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    //@CrossOrigin
    public void graphqlGET(@RequestParam("query") String query,
                           @RequestParam(value = "operationName", required = false) String operationName,
                           @RequestParam("variables") String variablesJson,
                           HttpServletResponse httpServletResponse) throws IOException {
        if (query == null) {
            query = "";
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        ;
        if (variablesJson != null) {
            variables = objectMapper.readValue(variablesJson, new TypeReference<Map<String, Object>>() {
            });
        }
        executeGraphqlQuery(httpServletResponse, operationName, query, variables);
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/graphql", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    //@CrossOrigin
    public void graphql(@RequestBody Map<String, Object> body, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        String query = (String) body.get("query");
        if (query == null) {
            query = "";
        }
        String operationName = (String) body.get("operationName");
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        executeGraphqlQuery(httpServletResponse, operationName, query, variables);
    }

    private void executeGraphqlQuery(HttpServletResponse httpServletResponse, String operationName, String query, Map<String, Object> variables) throws IOException {
        //
        // the context object is something that means something to down stream code.  It is instructions
        // from yourself to your other code such as DataFetchers.  The engine passes this on unchanged and
        // makes it available to inner code
        //
        // the graphql guidance says  :
        //
        //  - GraphQL should be placed after all authentication middleware, so that you
        //  - have access to the same session and user information you would in your
        //  - HTTP endpoint handlers.
        //
        long started = System.currentTimeMillis();
        Context context = contextProvider.newContext();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .operationName(operationName)
                .context(context)
                .build();

        ExecutionResult executionResult = graphql.execute(executionInput);
        double took = (System.currentTimeMillis()-started)/1000.0;

        LOGGER.debug("Total resolution time: "+took);
        LOGGER.debug("Total execution time of "+ QueryExecutor.getTotalQueriesPerformed()+" queries: "+ QueryExecutor.getTotalQueryExectionTime()/1000.0);
        LOGGER.debug("Queries times "+ String.join("+", QueryExecutor.getTotalQueryExectionList()));

        if(System.getenv().containsKey(TRACK_EXECUTION_TIMES)) {
            String content = QueryExecutor.getTotalQueriesPerformed() + ";" + QueryExecutor.getTotalQueryExectionTime() / 1000.0 + ";" + took+"\n";
            if(!Paths.get("times.csv").toFile().exists())
                Files.write(Paths.get("times.csv"), content.getBytes(), StandardOpenOption.CREATE_NEW);
            else
                Files.write(Paths.get("times.csv"), content.getBytes(), StandardOpenOption.APPEND);
        }

        handleNormalResponse(httpServletResponse, executionResult);
    }

    private void handleNormalResponse(HttpServletResponse httpServletResponse, ExecutionResult executionResult) throws IOException {
        Map<String, Object> result = executionResult.toSpecification();
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        String body = objectMapper.writeValueAsString(result);
        PrintWriter writer = httpServletResponse.getWriter();
        writer.write(body);
        writer.close();
    }
}
