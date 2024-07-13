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

package link.thingscloud.netty.remoting;

import link.thingscloud.netty.remoting.api.RemotingClient;
import link.thingscloud.netty.remoting.api.RemotingServer;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.config.RemotingClientConfig;
import link.thingscloud.netty.remoting.config.RemotingServerConfig;
import link.thingscloud.netty.remoting.impl.command.RemotingCommandFactoryImpl;
import link.thingscloud.netty.remoting.impl.netty.NettyRemotingClientImpl;
import link.thingscloud.netty.remoting.impl.netty.NettyRemotingServerImpl;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public class RemotingBootstrapFactory {

    private RemotingBootstrapFactory() {
    }

    public static final RemotingCommandFactoryImpl FACTORY = new RemotingCommandFactoryImpl();

    public static RemotingCommand createRequest() {
        return FACTORY.createRequest();
    }

    public static RemotingCommand createResponse(final RemotingCommand request) {
        return FACTORY.createResponse(request);
    }

    public static RemotingServer createRemotingServer(final RemotingServerConfig config) {
        return new NettyRemotingServerImpl(config);
    }

    public static RemotingClient createRemotingClient(final RemotingClientConfig config) {
        return new NettyRemotingClientImpl(config);
    }
}
