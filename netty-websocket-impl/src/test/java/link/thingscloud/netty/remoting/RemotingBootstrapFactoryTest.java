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

import link.thingscloud.netty.remoting.api.RemotingServer;
import link.thingscloud.netty.remoting.config.RemotingServerConfig;
import org.junit.jupiter.api.Test;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
class RemotingBootstrapFactoryTest {

    @Test
    void createRequest() {
        System.out.println(RemotingBootstrapFactory.createRequest());
    }

    @Test
    void createResponse() {
        System.out.println(RemotingBootstrapFactory.createRequest());
        System.out.println(RemotingBootstrapFactory.createRequest());

    }

    @Test
    void createRemotingServer() {

    }

    @Test
    void testCreateRequest() {
    }

    @Test
    void testCreateResponse() {
    }

    @Test
    void testCreateRemotingServer() {
        RemotingServer remotingServer = RemotingBootstrapFactory.createRemotingServer(new RemotingServerConfig());
        remotingServer.start();
    }

    @Test
    void createRemotingClient() {

    }
}