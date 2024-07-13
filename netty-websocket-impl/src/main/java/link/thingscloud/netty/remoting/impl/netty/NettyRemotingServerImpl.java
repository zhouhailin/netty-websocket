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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import link.thingscloud.netty.remoting.api.AsyncHandler;
import link.thingscloud.netty.remoting.api.RemotingServer;
import link.thingscloud.netty.remoting.api.channel.RemotingChannel;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.common.TlsMode;
import link.thingscloud.netty.remoting.config.RemotingServerConfig;
import link.thingscloud.netty.remoting.impl.channel.NettyRemotingChannelImpl;
import link.thingscloud.netty.remoting.impl.command.CodecHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Slf4j
public class NettyRemotingServerImpl extends AbstractRemotingServiceImpl implements RemotingServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private int port;
    private final RemotingServerConfig config;

    /**
     * SSL context via which to create {@link SslHandler}.
     */
    protected volatile SslContext sslContext;

    public static final String HANDSHAKE_HANDLER_NAME = "handshakeHandler";
    public static final String HA_PROXY_DECODER = "HAProxyDecoder";
    public static final String HA_PROXY_HANDLER = "HAProxyHandler";
    public static final String TLS_MODE_HANDLER = "TlsModeHandler";
    public static final String TLS_HANDLER_NAME = "sslHandler";
    public static final String FILE_REGION_ENCODER_NAME = "fileRegionEncoder";

    private TlsModeHandler tlsModeHandler;

    public NettyRemotingServerImpl(RemotingServerConfig remotingServerConfig) {
        super(remotingServerConfig);
        this.config = remotingServerConfig;
        loadSslContext();
    }

    @Override
    public void start() {
        super.start();
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(HANDSHAKE_HANDLER_NAME, new HandshakeHandler());
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
                        if (config.isCompression()) {
                            pipeline.addLast(new WebSocketServerCompressionHandler());
                        }
                        pipeline.addLast(new ReadTimeoutHandler(config.getReadTimeoutSeconds(), TimeUnit.SECONDS));
                        pipeline.addLast(new WebSocketServerProtocolHandler(config.getUrl(), null, true));
                        pipeline.addLast(new WebSocketFrameHandler());
                    }
                });
        ChannelFuture channelFuture = b.bind(config.getPort()).syncUninterruptibly();
        this.port = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();
    }

    @Override
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        super.stop();
    }


    @Override
    public int localListenPort() {
        return port;
    }

    @Override
    public RemotingCommand invoke(RemotingChannel channel, RemotingCommand request, long timeoutMillis) {
        return invokeWithInterceptor(channel.channel(), request, timeoutMillis);
    }

    @Override
    public void invokeAsync(RemotingChannel channel, RemotingCommand request, AsyncHandler handler, long timeoutMillis) {
        invokeAsyncWithInterceptor(channel.channel(), request, handler, timeoutMillis);
    }

    @Override
    public void invokeOneway(RemotingChannel channel, RemotingCommand request) {
        invokeOnewayWithInterceptor(channel.channel(), request);
    }

    public void loadSslContext() {
        TlsMode tlsMode = TlsSystemConfig.tlsMode;
        this.tlsModeHandler = new TlsModeHandler(TlsSystemConfig.tlsMode);

        log.info("Server is running in TLS {} mode", tlsMode.getName());
        if (tlsMode != TlsMode.DISABLED) {
            try {
                sslContext = TlsHelper.buildSslContext(false);
                log.info("SSLContext created for server");
            } catch (CertificateException | IOException e) {
                log.error("Failed to create SSLContext for server", e);
            }
        }
    }

    protected class HandshakeHandler extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
            try {
                ctx.pipeline().addAfter(ctx.name(), TLS_MODE_HANDLER, tlsModeHandler);
                try {
                    // Remove this handler
                    ctx.pipeline().remove(this);
                } catch (NoSuchElementException e) {
                    log.error("Error while removing HandshakeHandler", e);
                }
            } catch (Exception e) {
                log.error("process proxy protocol negotiator failed.", e);
                throw e;
            }
        }

    }

    @ChannelHandler.Sharable
    protected class TlsModeHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final TlsMode tlsMode;

        private static final byte HANDSHAKE_MAGIC_CODE = 0x16;

        TlsModeHandler(TlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            // Peek the current read index byte to determine if the content is starting with TLS handshake
            byte b = msg.getByte(msg.readerIndex());
            if (b == HANDSHAKE_MAGIC_CODE) {
                switch (tlsMode) {
                    case DISABLED:
                        ctx.close();
                        log.warn("Clients intend to establish an SSL connection while this server is running in SSL disabled mode");
                        throw new UnsupportedOperationException("The NettyRemotingServer in SSL disabled mode doesn't support ssl client");
                    case PERMISSIVE:
                    case ENFORCING:
                        if (null != sslContext) {
                            ctx.pipeline().addAfter(TLS_MODE_HANDLER, TLS_HANDLER_NAME, sslContext.newHandler(ctx.channel().alloc()));
                            log.info("Handlers prepended to channel pipeline to establish SSL connection");
                        } else {
                            ctx.close();
                            log.error("Trying to establish an SSL connection but sslContext is null");
                        }
                        break;
                    default:
                        log.warn("Unknown TLS mode");
                        break;
                }
            } else if (tlsMode == TlsMode.ENFORCING) {
                ctx.close();
                log.warn("Clients intend to establish an insecure connection while this server is running in SSL enforcing mode");
            }

            try {
                // Remove this handler
                ctx.pipeline().remove(this);
            } catch (NoSuchElementException e) {
                log.error("Error while removing TlsModeHandler", e);
            }

            // Hand over this message to the next .
            ctx.fireChannelRead(msg.retain());
        }

    }

    protected class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

        private RemotingChannel remotingChannel;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
            // ping and pong frames already handled
            if (frame instanceof TextWebSocketFrame) {
                processMessageReceived(ctx, remotingChannel, CodecHelper.decode(((TextWebSocketFrame) frame).text()));
            } else if (frame instanceof BinaryWebSocketFrame) {
                ByteBuf buf = frame.content();
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                processMessageReceived(ctx, remotingChannel, CodecHelper.decode(bytes));
            } else {
                String message = "unsupported frame type: " + frame.getClass().getName();
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
            publicExecutor.execute(() -> listenerGroup.onChannelOpened(remotingChannel));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("Channel {} became inactive, remote address {}.", ctx.channel(), ctx.channel().remoteAddress());
            publicExecutor.execute(() -> listenerGroup.onChannelClosed(remotingChannel));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.info("Close channel {} because of error : ", ctx.channel(), cause);
            publicExecutor.execute(() -> listenerGroup.onChannelException(remotingChannel, cause));
            ctx.channel().close().addListener((ChannelFutureListener) future -> log.warn("Close channel {} because of error {}, result is {}", ctx.channel(), cause, future.isSuccess()));
        }

    }

}
