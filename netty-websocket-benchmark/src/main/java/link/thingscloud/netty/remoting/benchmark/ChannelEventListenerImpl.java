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

import link.thingscloud.netty.remoting.api.channel.ChannelEventListener;
import link.thingscloud.netty.remoting.api.channel.RemotingChannel;
import link.thingscloud.netty.remoting.spring.boot.starter.annotation.RemotingChannelEventListener;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Slf4j
@RemotingChannelEventListener
public class ChannelEventListenerImpl implements ChannelEventListener {

    @Override
    public void onOpened(RemotingChannel channel) {
        log.info("onOpened : {}", channel);
    }

    @Override
    public void onClosed(RemotingChannel channel) {
        log.info("onClosed : {}", channel);
    }

    @Override
    public void onException(RemotingChannel channel, Throwable cause) {
        log.error("onException : {}", channel, cause);
    }
}

