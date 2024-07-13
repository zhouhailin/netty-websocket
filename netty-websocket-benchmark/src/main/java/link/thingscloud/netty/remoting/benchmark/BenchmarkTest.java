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

package link.thingscloud.netty.remoting.benchmark;

import link.thingscloud.netty.remoting.RemotingBootstrapFactory;
import link.thingscloud.netty.remoting.api.RemotingClient;
import link.thingscloud.netty.remoting.api.RemotingServer;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.common.TlsMode;
import link.thingscloud.netty.remoting.config.RemotingClientConfig;
import link.thingscloud.netty.remoting.config.RemotingServerConfig;
import link.thingscloud.netty.remoting.impl.command.RemotingCommandFactoryImpl;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public class BenchmarkTest {

    static RemotingCommandFactoryImpl factory = RemotingBootstrapFactory.FACTORY;

    public static void main(String[] args) {
        RemotingServer server = RemotingBootstrapFactory.createRemotingServer(new RemotingServerConfig());

        server.registerRequestProcessor(1, (channel, request) -> {
            System.out.println("received request : " + request);
            RemotingCommand response = factory.createResponse(request);
            response.setPayload("world");
            return response;
        });
        server.start();
        System.out.println("localListenPort : " + server.localListenPort());

        RemotingClient client = RemotingBootstrapFactory.createRemotingClient(new RemotingClientConfig().setTlsMode(TlsMode.ENFORCING.getName()));
        client.start();

        RemotingCommand request = client.commandFactory().createRequest();
        request.setCmdCode(1);
        request.setPayload("hello");

        RemotingCommand response = client.invoke("127.0.0.1:9876", request, 3000);
        System.out.println("received response : " + response);

        client.stop();
        server.stop();
    }
}
