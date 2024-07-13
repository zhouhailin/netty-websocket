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

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public interface ChannelEventListener {

    /**
     * 连接创建
     *
     * @param channel 连接对象
     */
    void onOpened(RemotingChannel channel);

    /**
     * 连接关闭
     *
     * @param channel 连接对象
     */
    void onClosed(RemotingChannel channel);
//
//    /**
//     * 接收到消息
//     *
//     * @param channel 连接对象
//     * @param message 消息对象
//     */
//    void onMessage(RemotingChannel channel, RemotingCommand message);

    /**
     * 异常
     *
     * @param channel 连接对象
     * @param cause   异常对象
     */
    void onException(RemotingChannel channel, Throwable cause);

}
