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

import link.thingscloud.netty.remoting.api.channel.ChannelEventListener;
import link.thingscloud.netty.remoting.api.command.RemotingCommandFactory;
import link.thingscloud.netty.remoting.api.interceptor.Interceptor;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public interface RemotingService {

    String DEFAULT_URI = "/netty/remoting/websocket";

    /**
     * 服务启动
     */
    void start();

    /**
     * 服务停止
     */
    void stop();

    /**
     * 注册拦截器
     *
     * @param interceptor 拦截器
     */
    void registerInterceptor(Interceptor interceptor);

    /**
     * 注册请求处理器
     *
     * @param requestCode 请求码
     * @param processor   处理器
     */
    void registerRequestProcessor(int requestCode, RequestProcessor processor);

    /**
     * 注册通道监听器
     *
     * @param listener 通道监听器
     */
    void registerChannelEventListener(ChannelEventListener listener);

    /**
     * 获取命令工厂
     *
     * @return 命令工厂
     */
    RemotingCommandFactory commandFactory();

}
