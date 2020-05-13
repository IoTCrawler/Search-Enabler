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

import graphql.language.ArrayValue;
import graphql.language.StringValue;
import graphql.language.Value;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static List<String> extractValues(Value value){

        if(value instanceof StringValue)
            return Arrays.asList(new String[]{ ((StringValue)value).getValue() });

        if(value instanceof ArrayValue) {
            List<String> values = new ArrayList<>();
            for(Value value1: ((ArrayValue) value).getValues()){
                values.addAll(extractValues(value1));
            };
            return values;
        }else throw new NotImplementedException(value.getClass().getCanonicalName());

    }
}
