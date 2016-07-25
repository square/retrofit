/*
 * Copyright (C) 2015 Square, Inc.
 *
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
 */
package retrofit2.converter.ndjson;


import java.io.Serializable;

/**
 * NDJSon response wrapper. This class holds accessible JSON content parts
 * Created by Cotuna Aurelian on 7/2/2016.
 */

public class NDJsonResponse extends BaseResponse implements Serializable {

    /**
     * JSON parts from the raw content returned in the NDJSON format
     */
    private String[] splitJSONContent;

    public NDJsonResponse(String rawContent) {
        //We make sure that the NDJSON format is respected and there is a new line character after
        //each part, and before the next one. We are going to split the segments of NDJSON based
        //on that caracter
        String filteredRawContent = rawContent.replaceAll("\\}\\s*\\{", "\\}\\%n\\{");

        this.splitJSONContent = filteredRawContent.split("%n");
        setResponseType(NDJSON_RESPONSE);
    }

    public String[] getSplitJSONContent() {
        return splitJSONContent;
    }
}
