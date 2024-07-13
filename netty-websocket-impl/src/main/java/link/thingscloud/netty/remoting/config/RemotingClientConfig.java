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

package link.thingscloud.netty.remoting.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class RemotingClientConfig extends RemotingConfig {
    private int readTimeoutSeconds = 45;
    private int clientOnewayInvokeSemaphore = 65535;
    private int clientAsyncInvokeSemaphore = 65535;
    private int connectTimeoutMillis = 3000;
    private int handshakeTimeoutMillis = 5000;
    private boolean clientCloseSocketIfTimeout = false;
    private boolean clientShortConnectionEnable = false;
    private String tlsMode = "permissive";

    @Override
    public int getOnewayInvokeSemaphore() {
        return clientOnewayInvokeSemaphore;
    }

    @Override
    public int getAsyncInvokeSemaphore() {
        return clientAsyncInvokeSemaphore;
    }
}
