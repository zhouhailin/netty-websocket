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

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
public class ChannelEventListenerGroup {

    private final List<ChannelEventListener> listeners = new ArrayList<>(4);

    public int size() {
        return this.listeners.size();
    }

    public void registerChannelEventListener(final ChannelEventListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void onChannelOpened(final RemotingChannel channel) {
        for (ChannelEventListener listener : listeners) {
            listener.onOpened(channel);
        }
    }

    public void onChannelClosed(final RemotingChannel channel) {
        for (ChannelEventListener listener : listeners) {
            listener.onClosed(channel);
        }
    }

    public void onChannelException(final RemotingChannel channel, final Throwable cause) {
        for (ChannelEventListener listener : listeners) {
            listener.onException(channel, cause);
        }
    }

}
