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

package link.thingscloud.netty.remoting.impl.channel;

import io.netty.channel.Channel;
import link.thingscloud.netty.remoting.api.channel.RemotingChannel;

import java.net.SocketAddress;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public class NettyRemotingChannelImpl implements RemotingChannel {

    /**
     * 连接对象
     */
    private final Channel channel;
    private Object credentials;

    public NettyRemotingChannelImpl(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String id() {
        return channel.id().asShortText();
    }

    @Override
    public Object credentials() {
        return credentials;
    }

    @Override
    public void credentials(Object credentials) {
        this.credentials = credentials;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void close() {
        channel.close();
    }

}
