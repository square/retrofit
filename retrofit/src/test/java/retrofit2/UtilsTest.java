/*
 * Copyright (C) 2013 Square, Inc.
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
package retrofit2;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class UtilsTest {

    @Test
    public void toJSONString1() throws Exception {
        String json = Utils.toJSONString(null);
        Assert.assertEquals("{}", json);
    }

    @Test
    public void toJSONString2() throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        String json = Utils.toJSONString(map);
        Assert.assertEquals("{}", json);
    }

    @Test
    public void toJSONString3() throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("1", 1);
        map.put("2", null);
        map.put("3", 2.0f);
        map.put("4", 2L);
        map.put("5", "5");
        map.put("5", true);

        String json = Utils.toJSONString(map);
        Assert.assertEquals("{\"1\":1,\"3\":2.0,\"4\":2,\"5\":true}", json);
    }

    @Test
    public void toJSONString4() throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("1", 1);
        map.put("2", 2.0);
        map.put(null, 2.0f);
        map.put("4", 2L);
        map.put("5", "5");
        map.put("5", true);

        String json = Utils.toJSONString(map);
        Assert.assertEquals("{\"1\":1,\"2\":2.0,\"4\":2,\"5\":true}", json);
    }


    @Test
    public void toJSONString5() throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("1", 1);
        map.put("2", 2.0);
        map.put("3", 2.0f);
        map.put("4", 2L);
        map.put("5", "5");
        map.put("5", true);

        String json = Utils.toJSONString(map);
        Assert.assertEquals("{\"1\":1,\"2\":2.0,\"3\":2.0,\"4\":2,\"5\":true}", json);
    }
}
