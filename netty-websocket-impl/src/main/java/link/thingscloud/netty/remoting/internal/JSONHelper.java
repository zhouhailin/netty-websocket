/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package link.thingscloud.netty.remoting.internal;

import com.google.gson.Gson;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public class JSONHelper {
    private static final Gson GSON = new Gson();

    private JSONHelper() {
    }

    public static String encode(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T decode(String str, Class<T> beanClass) {
        return GSON.fromJson(str, beanClass);
    }

    public static <T> T translate(Object obj, Class<T> beanClass) {
        return decode(encode(obj), beanClass);
    }

}
