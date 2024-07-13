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

package link.thingscloud.netty.remoting.impl.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import link.thingscloud.netty.remoting.api.channel.RemotingChannel;
import link.thingscloud.netty.remoting.config.RemotingClientConfig;
import link.thingscloud.netty.remoting.impl.channel.NettyRemotingChannelImpl;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Slf4j
@Data
@Accessors(chain = true)
public class ClientChannelManager {

    private static final long LOCK_TIMEOUT_MILLIS = 3000;
    private final Lock lockChannelTables = new ReentrantLock();
    private final Bootstrap bootstrap;
    private final SslContext sslContext;
    private final RemotingClientConfig config;
    private final ConcurrentHashMap<String, RemotingChannel> channelTables = new ConcurrentHashMap<>();

    public static final String WSS = "wss";

    public void clear() {
        for (RemotingChannel channel : this.channelTables.values()) {
            this.closeChannel(channel.channel());
        }
        this.channelTables.clear();
    }

    public Channel createIfAbsent(URI uri) {
        RemotingChannel channel = this.channelTables.get(uri.toString());
        if (channel != null && channel.isActive()) {
            return channel.channel();
        }
        return this.createChannel(uri);
    }

    private Channel createChannel(URI uri) {
        RemotingChannel remotingChannel = null;
        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    remotingChannel = this.channelTables.get(uri.toString());
                    if (remotingChannel != null) {
                        if (remotingChannel.isActive()) {
                            return remotingChannel.channel();
                        } else {
                            this.channelTables.remove(uri.toString());
                        }
                    }
                    // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
                    // If you change it to V00, ping is not supported and remember to change
                    // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
                    HttpHeaders httpHeaders = new DefaultHttpHeaders();
                    WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
                            .newHandshaker(uri, WebSocketVersion.V13, null, true, httpHeaders, config.getMaxFramePayloadLength());
                    long start = System.currentTimeMillis();
                    Channel channel = this.bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
                    if (sslContext != null && uri.getScheme().equalsIgnoreCase(WSS)) {
                        channel.pipeline().addFirst(sslContext.newHandler(channel.alloc(), uri.getHost(), uri.getPort()));
                    }
                    long connectingTime = System.currentTimeMillis() - start;
                    log.debug("createChannel:  channel is established after sync, connectionId : {}, use {}ms", channel.id(), connectingTime);
                    NettyRemotingClientImpl.WebSocketClientFrameHandler handler = (NettyRemotingClientImpl.WebSocketClientFrameHandler) channel.pipeline().get("hookedHandler");
                    handler.setHandshaker(handshaker);
                    handshaker.handshake(channel);
                    start = System.currentTimeMillis();
                    this.waitHandshake(handler.getHandshakeFuture(), channel);
                    long handshakeTime = System.currentTimeMillis() - start;
                    log.info("createChannel : connection is established after handshake, connectionId : {}, use {}ms", channel.id(), handshakeTime);
                    remotingChannel = new NettyRemotingChannelImpl(channel);
                    this.channelTables.put(uri.toString(), remotingChannel);
                } catch (Exception e) {
                    log.error("createChannel: connect remote host exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                log.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException ingore) {
        }
        return remotingChannel == null ? null : remotingChannel.channel();
    }

    private void waitHandshake(ChannelFuture handshakeFuture, Channel channel) throws WebSocketHandshakeException, InterruptedException {
        if (!handshakeFuture.await(config.getHandshakeTimeoutMillis(), TimeUnit.MILLISECONDS)) {
            if (channel.isActive()) {
                channel.close();
            }

            if (handshakeFuture.cause() != null) {
                throw new WebSocketHandshakeException("Handshake timeout!", handshakeFuture.cause());
            } else {
                throw new WebSocketHandshakeException("Handshake timeout!");
            }
        }
    }

    public void closeChannel(URI uri, Channel channel) {
        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    boolean removeItemFromTable = true;
                    RemotingChannel prevCW = this.channelTables.get(uri.toString());
                    //Workaround for null
                    if (null == prevCW) {
                        return;
                    }

                    log.info("Begin to close the remote uri {} channel {}", uri, prevCW);

                    if (prevCW.channel() != channel) {
                        log.info("Channel {} has been closed,this is a new channel {}", prevCW.channel(), channel);
                        removeItemFromTable = false;
                    }

                    if (removeItemFromTable) {
                        this.channelTables.remove(uri.toString());
                        log.info("Channel {} has been removed !", uri);
                    }
                    channel.close().addListener((ChannelFutureListener) future -> log.warn("Close channel {} {}", channel, future.isSuccess()));
                } catch (Exception e) {
                    log.error("Close channel error !", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                log.warn("Can not lock channel table in {} ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            log.error("Close channel error !", e);
        }
    }

    public void closeChannel(final Channel channel) {
        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    boolean removeItemFromTable = true;
                    RemotingChannel prevCW = null;
                    String addrRemote = null;

                    for (Map.Entry<String, RemotingChannel> entry : channelTables.entrySet()) {
                        RemotingChannel prev = entry.getValue();
                        if (prev.channel() != null) {
                            if (prev.channel() == channel) {
                                prevCW = prev;
                                addrRemote = entry.getKey();
                                break;
                            }
                        }
                    }

                    if (null == prevCW) {
                        log.info("eventCloseChannel: the channel[{}] has been removed from the channel table before", addrRemote);
                        removeItemFromTable = false;
                    }

                    if (removeItemFromTable) {
                        this.channelTables.remove(addrRemote);
                        log.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
                    }
                } catch (Exception e) {
                    log.error("closeChannel: close the channel exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                log.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            log.error("closeChannel exception", e);
        }
    }
}
