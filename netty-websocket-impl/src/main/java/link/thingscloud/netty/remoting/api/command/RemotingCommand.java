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

package link.thingscloud.netty.remoting.api.command;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Data
@Accessors(chain = true)
public class RemotingCommand {
    /**
     * 业务编码，值必须大于 0
     */
    private int cmdCode;
    /**
     * 请求标识 - 自增自标识
     */
    private int requestId;
    /**
     * 请求消息类型
     */
    private TrafficType trafficType;
    /**
     * 语言类型
     */
    private LanguageType languageType;
    /**
     * 版本号
     */
    private String version;
    /**
     * 响应结果类型
     */
    private int opCode;
    /**
     * 备注信息
     */
    private String remark;
    /**
     * 属性信息
     */
    private Map<String, Object> properties;
    /**
     * 消息体
     */
    private Object payload;
}
