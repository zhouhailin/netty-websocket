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

import link.thingscloud.netty.remoting.api.channel.RemotingChannel;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public interface RemotingServer extends RemotingService {


    /**
     * 获取本地监听端口
     *
     * @return 本地监听端口
     */
    int localListenPort();


    /**
     * 同步调用
     *
     * @param channel       连接对象
     * @param request       请求对象
     * @param timeoutMillis 超时时间
     * @return 响应对象
     */
    RemotingCommand invoke(RemotingChannel channel, RemotingCommand request, long timeoutMillis);

    /**
     * 异步调用
     *
     * @param channel       连接对象
     * @param request       请求对象
     * @param handler       异步回调
     * @param timeoutMillis 超时时间
     */
    void invokeAsync(RemotingChannel channel, RemotingCommand request, AsyncHandler handler, long timeoutMillis);

    /**
     * 单向调用
     *
     * @param channel 连接对象
     * @param request 请求对象
     */
    void invokeOneway(RemotingChannel channel, RemotingCommand request);

}
