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

package link.thingscloud.netty.remoting.api.channel;

import io.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public interface RemotingChannel {

    /**
     * Channel 标识
     *
     * @return channel id
     */
    String id();

    /**
     * 凭证对象
     *
     * @return channel holder
     */
    Object credentials();

    /**
     * 设置凭证信息
     *
     * @param credential 凭证信息
     */
    void credentials(Object credentials);

    /**
     * 通道
     *
     * @return channel
     */
    Channel channel();

    /**
     * 获取本地网卡
     *
     * @return 本地网卡信息
     */
    SocketAddress localAddress();

    /**
     * 获取远程网卡
     *
     * @return 远程网卡信息
     */
    SocketAddress remoteAddress();

    /**
     * 是否可写
     *
     * @return 是否可写
     */
    boolean isWritable();

    /**
     * 是否活跃
     *
     * @return 是否活跃
     */
    boolean isActive();

    /**
     * 关闭连接
     */
    void close();

}
