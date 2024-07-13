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

import link.thingscloud.netty.remoting.api.AsyncHandler;
import link.thingscloud.netty.remoting.api.RemotingClient;
import link.thingscloud.netty.remoting.api.RemotingServer;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.api.command.RemotingCommandFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Slf4j
@SpringBootApplication
public class BenchmarkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkApplication.class, args);
    }

    @Autowired
    private RemotingClient remotingClient;
    @Autowired
    private RemotingServer remotingServer;
    @Autowired
    private RemotingCommandFactory factory;

    @PostConstruct
    public void start() {
        RemotingCommand request = factory.createRequest();
        request.setCmdCode((short) 13);
        remotingClient.invokeOneWay("127.0.0.1:9876", request);
        remotingClient.invokeAsync("127.0.0.1:9876", request, new AsyncHandler() {
            @Override
            public void onFailure(RemotingCommand request, Throwable cause) {
                log.info("invokeAsync onFailure : {}, cause : ", request, cause);
            }

            @Override
            public void onSuccess(RemotingCommand response) {
                log.info("invokeAsync onSuccess : {}", response);

            }
        }, 1000);
        RemotingCommand response = remotingClient.invoke("127.0.0.1:9876", request, 100);
        log.info("invoke response : {}", response);

    }

}
