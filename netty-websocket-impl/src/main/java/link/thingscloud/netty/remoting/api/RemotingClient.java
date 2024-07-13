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

package link.thingscloud.netty.remoting.api;

import link.thingscloud.netty.remoting.api.command.RemotingCommand;

import java.net.URI;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public interface RemotingClient extends RemotingService {

    String getScheme();

    default RemotingCommand invoke(String address, RemotingCommand request, long timeoutMillis) {
        return invoke(URI.create(getScheme() + "://" + address + DEFAULT_URI), request, timeoutMillis);
    }

    RemotingCommand invoke(URI uri, RemotingCommand request, long timeoutMillis);

    default void invokeAsync(String address, RemotingCommand request, AsyncHandler asyncHandler, long timeoutMillis) {
        invokeAsync(URI.create(getScheme() + "://" + address + DEFAULT_URI), request, asyncHandler, timeoutMillis);
    }

    void invokeAsync(URI uri, RemotingCommand request, AsyncHandler asyncHandler, long timeoutMillis);

    default void invokeOneWay(String address, RemotingCommand request) {
        invokeOneWay(URI.create(getScheme() + "://" + address + DEFAULT_URI), request);
    }

    void invokeOneWay(URI uri, RemotingCommand request);

}
