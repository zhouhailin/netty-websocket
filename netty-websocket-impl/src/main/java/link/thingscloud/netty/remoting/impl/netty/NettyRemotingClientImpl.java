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
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import link.thingscloud.netty.remoting.api.AsyncHandler;
import link.thingscloud.netty.remoting.api.RemotingClient;
import link.thingscloud.netty.remoting.api.channel.RemotingChannel;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.api.command.TrafficType;
import link.thingscloud.netty.remoting.api.exception.RemotingConnectFailureException;
import link.thingscloud.netty.remoting.api.exception.RemotingTimeoutException;
import link.thingscloud.netty.remoting.common.TlsMode;
import link.thingscloud.netty.remoting.config.RemotingClientConfig;
import link.thingscloud.netty.remoting.impl.channel.NettyRemotingChannelImpl;
import link.thingscloud.netty.remoting.impl.command.CodecHelper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Slf4j
public class NettyRemotingClientImpl extends AbstractRemotingServiceImpl implements RemotingClient {

    private EventLoopGroup group;

    /**
     * SSL context via which to create {@link SslHandler}.
     */
    protected SslContext sslContext;

    private final RemotingClientConfig config;
    private final Bootstrap clientBootstrap = new Bootstrap();

    private final ClientChannelManager clientChannelManager;

    public NettyRemotingClientImpl(RemotingClientConfig remotingClientConfig) {
        super(remotingClientConfig);
        this.config = remotingClientConfig;
        loadSslContext();
        this.clientChannelManager = new ClientChannelManager(clientBootstrap, sslContext, remotingClientConfig);
    }

    @Override
    public void start() {
        super.start();
        this.group = new NioEventLoopGroup();


        this.clientBootstrap
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMillis())
                .option(ChannelOption.SO_SNDBUF, config.getTcpSoSndBufSize())
                .option(ChannelOption.SO_RCVBUF, config.getTcpSoRcvBufSize())
                .option(ChannelOption.SO_KEEPALIVE, config.isTcpSoKeepAlive())
                .option(ChannelOption.TCP_NODELAY, config.isTcpSoNoDelay())
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
                        if (config.isCompression()) {
                            pipeline.addLast(new WebSocketServerCompressionHandler());
                        }
                        //
//                        pipeline.addLast(new ReadTimeoutHandler(config.getReadTimeoutSeconds(), TimeUnit.SECONDS));
                        pipeline.addLast("hookedHandler", new WebSocketClientFrameHandler());
                    }
                });
    }

    @Override
    public void stop() {
        clientChannelManager.clear();
        this.group.shutdownGracefully();
        super.stop();
    }

    @Override
    public String getScheme() {
        return TlsMode.parse(config.getTlsMode()) == TlsMode.ENFORCING ? "wss" : "ws";
    }

    @Override
    public RemotingCommand invoke(URI uri, RemotingCommand request, long timeoutMillis) {
        request.setTrafficType(TrafficType.REQUEST_SYNC);
        Channel channel = this.clientChannelManager.createIfAbsent(uri);
        if (channel != null && channel.isActive()) {
            try {
                return this.invokeWithInterceptor(channel, request, timeoutMillis);
            } catch (RemotingTimeoutException e) {
                if (this.config.isClientCloseSocketIfTimeout()) {
                    log.warn("invoke: timeout, so close the socket {} ms, {}", timeoutMillis, uri);
                    this.clientChannelManager.closeChannel(uri, channel);
                }
                log.warn("invoke: wait response timeout<{}ms> exception, so close the channel[{}]", timeoutMillis, uri);
                throw e;
            } finally {
                if (this.config.isClientShortConnectionEnable()) {
                    this.clientChannelManager.closeChannel(uri, channel);
                }
            }
        } else {
            this.clientChannelManager.closeChannel(uri, channel);
            throw new RemotingConnectFailureException(uri.toString());
        }
    }

    @Override
    public void invokeAsync(URI uri, RemotingCommand request, AsyncHandler asyncHandler, long timeoutMillis) {
        final Channel channel = this.clientChannelManager.createIfAbsent(uri);
        if (channel != null && channel.isActive()) {
            this.invokeAsyncWithInterceptor(channel, request, asyncHandler, timeoutMillis);
        } else {
            this.clientChannelManager.closeChannel(uri, channel);
        }
    }

    @Override
    public void invokeOneWay(URI uri, RemotingCommand request) {
        final Channel channel = this.clientChannelManager.createIfAbsent(uri);
        if (channel != null && channel.isActive()) {
            this.invokeOnewayWithInterceptor(channel, request);
        } else {
            this.clientChannelManager.closeChannel(uri, channel);
        }
    }

    public void loadSslContext() {
        try {
            sslContext = TlsHelper.buildSslContext(true);
            log.info("SSL enabled for client");
        } catch (Exception e) {
            log.error("Failed to create SSLContext", e);
        }
    }


    protected class WebSocketClientFrameHandler extends SimpleChannelInboundHandler<Object> {

        private RemotingChannel remotingChannel;
        private WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            this.handshakeFuture = ctx.newPromise();
            super.handlerAdded(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            // ping and pong frames already handled
            Channel ch = ctx.channel();
            if (handshaker != null && !this.handshaker.isHandshakeComplete()) {
                try {
                    FullHttpResponse response = (FullHttpResponse) msg;
                    this.handshaker.finishHandshake(ch, response);
                    this.handshakeFuture.setSuccess();
                    log.debug("[{}] WebSocket Client connected! response headers : {}", ctx.channel().id(), response.headers());
                } catch (WebSocketHandshakeException var7) {
                    FullHttpResponse res = (FullHttpResponse) msg;
                    String errorMsg = String.format("WebSocket Client failed to connect,status:%s,reason:%s", res.status(), res.content().toString(CharsetUtil.UTF_8));
                    log.error("[{}] {}", ctx.channel().id(), errorMsg);
                    this.handshakeFuture.setFailure(new Exception(errorMsg));
                }
            } else if (msg instanceof FullHttpResponse) {
                throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + ((FullHttpResponse) msg).status() + ", content=" + ((FullHttpResponse) msg).content().toString(CharsetUtil.UTF_8) + ')');
            } else if (msg instanceof TextWebSocketFrame) {
                processMessageReceived(ctx, remotingChannel, CodecHelper.decode(((TextWebSocketFrame) msg).text()));
            } else if (msg instanceof BinaryWebSocketFrame) {
                ByteBuf buf = ((BinaryWebSocketFrame) msg).content();
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                processMessageReceived(ctx, remotingChannel, CodecHelper.decode(bytes));
            } else {
                String message = "unsupported frame type: " + msg.getClass().getName();
                throw new UnsupportedOperationException(message);
            }
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            log.info("Channel {} registered, remote address {}.", ctx.channel(), ctx.channel().remoteAddress());
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            log.info("Channel {} unregistered, remote address {}.", ctx.channel(), ctx.channel().remoteAddress());
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("Channel {} became active, remote address {}.", ctx.channel(), ctx.channel().remoteAddress());
            remotingChannel = new NettyRemotingChannelImpl(ctx.channel());
            listenerGroup.onChannelOpened(remotingChannel);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("Channel {} became inactive, remote address {}.", ctx.channel(), ctx.channel().remoteAddress());
            if (handshaker != null && !this.handshaker.isHandshakeComplete()) {
                String errorMsg;
                if (ctx.channel() != null) {
                    errorMsg = "channel inactive during handshake,connectionId:" + ctx.channel().id();
                } else {
                    errorMsg = "channel inactive during handshake";
                }
                log.info("Channel {} became inactive, remote address {}, errorMsg : {}.", ctx.channel(), ctx.channel().remoteAddress(), errorMsg);
                this.handshakeFuture.setFailure(new Exception(errorMsg));
            }

            listenerGroup.onChannelClosed(remotingChannel);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!this.handshakeFuture.isDone()) {
                this.handshakeFuture.setFailure(cause);
            }
            log.info("Close channel {} because of error : ", ctx.channel(), cause);
            listenerGroup.onChannelException(remotingChannel, cause);
            ctx.channel().close().addListener((ChannelFutureListener) future -> log.warn("Close channel {} because of error {}, result is {}", ctx.channel(), cause, future.isSuccess()));
        }

        public void setHandshaker(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        public ChannelFuture getHandshakeFuture() {
            return handshakeFuture;
        }
    }
}
