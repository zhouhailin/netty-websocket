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

package link.thingscloud.netty.remoting.impl.command;

import com.google.gson.Gson;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.internal.FuryHelper;
import link.thingscloud.netty.remoting.internal.JSONHelper;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public class CodecHelper {

    private static final Gson GSON = new Gson();

    private CodecHelper() {
    }

    public static String encode(Object msg) {
        return JSONHelper.encode(msg);
    }

    public static byte[] encodeBytes(Object msg) {
        return FuryHelper.encode(msg);
    }

    public static RemotingCommand decode(String body) {
        return JSONHelper.decode(body, RemotingCommand.class);
    }

    public static RemotingCommand decode(byte[] bytes) {
        return (RemotingCommand) FuryHelper.decode(bytes);
    }


}
