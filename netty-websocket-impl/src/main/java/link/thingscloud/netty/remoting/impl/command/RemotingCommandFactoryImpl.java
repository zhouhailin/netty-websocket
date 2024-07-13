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

package link.thingscloud.netty.remoting.impl.command;

import link.thingscloud.netty.remoting.api.command.LanguageType;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.api.command.RemotingCommandFactory;
import link.thingscloud.netty.remoting.api.command.TrafficType;
import link.thingscloud.netty.remoting.internal.VersionHelper;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public class RemotingCommandFactoryImpl implements RemotingCommandFactory {

    public final static RequestIdGenerator REQUEST_ID_GENERATOR = RequestIdGenerator.INST;

    @Override
    public RemotingCommand createRequest() {
        return new RemotingCommand()
                .setRequestId(REQUEST_ID_GENERATOR.incrementAndGet())
                .setTrafficType(TrafficType.REQUEST_SYNC)
                .setOpCode(RemotingSysResponseCode.SUCCESS)
                .setLanguageType(LanguageType.JAVA)
                .setVersion(VersionHelper.getVer());
    }

    @Override
    public RemotingCommand createResponse(final RemotingCommand request) {
        return new RemotingCommand()
                .setCmdCode(request.getCmdCode())
                .setRequestId(request.getRequestId())
                .setTrafficType(TrafficType.RESPONSE)
                .setLanguageType(LanguageType.JAVA)
                .setVersion(VersionHelper.getVer())
                .setOpCode(RemotingSysResponseCode.SUCCESS);
    }

}
