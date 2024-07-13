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

package link.thingscloud.netty.remoting.spring.boot.starter.config;

import link.thingscloud.netty.remoting.api.RemotingService;
import link.thingscloud.netty.remoting.api.RequestProcessor;
import link.thingscloud.netty.remoting.api.channel.ChannelEventListener;
import link.thingscloud.netty.remoting.api.interceptor.Interceptor;
import link.thingscloud.netty.remoting.spring.boot.starter.annotation.RemotingChannelEventListener;
import link.thingscloud.netty.remoting.spring.boot.starter.annotation.RemotingInterceptor;
import link.thingscloud.netty.remoting.spring.boot.starter.annotation.RemotingRequestProcessor;
import link.thingscloud.netty.remoting.spring.boot.starter.annotation.RemotingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

/**
 * @author : zhouhailin
 * @version 0.2.0
 */
public abstract class AbstractRemotingAutoConfiguration {

    @Autowired(required = false)
    private List<Interceptor> interceptors = Collections.emptyList();
    @Autowired(required = false)
    private List<RequestProcessor> requestProcessors = Collections.emptyList();
    @Autowired(required = false)
    private List<ChannelEventListener> channelEventListeners = Collections.emptyList();

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public void registerRequestProcessor(RemotingService remotingService, RemotingType type) {
        requestProcessors.forEach(requestProcessor -> {
            RemotingRequestProcessor annotation = requestProcessor.getClass().getAnnotation(RemotingRequestProcessor.class);
            if (annotation != null && annotation.type() != type) {
                log.info("{} registerRequestProcessor code : {}, class : {}", remotingService.getClass().getSimpleName(), annotation.code(), requestProcessor.getClass().getName());
                remotingService.registerRequestProcessor(annotation.code(), requestProcessor);
            }
        });
    }

    public void registerInterceptor(RemotingService remotingService, RemotingType type) {
        interceptors.forEach(interceptor -> {
            RemotingInterceptor annotation = interceptor.getClass().getAnnotation(RemotingInterceptor.class);
            if (annotation != null && annotation.type() != type) {
                log.info("{} registerInterceptor {}", remotingService.getClass().getSimpleName(), interceptor.getClass().getName());
                remotingService.registerInterceptor(interceptor);
            }
        });
    }

    public void registerChannelEventListener(RemotingService remotingService, RemotingType type) {
        channelEventListeners.forEach(listener -> {
            RemotingChannelEventListener annotation = listener.getClass().getAnnotation(RemotingChannelEventListener.class);
            if (annotation != null && annotation.type() != type) {
                log.info("{} registerChannelEventListener {}", listener.getClass().getSimpleName(), listener.getClass().getName());
                remotingService.registerChannelEventListener(listener);
            }
        });
    }
}
