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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import link.thingscloud.netty.remoting.api.AsyncHandler;
import link.thingscloud.netty.remoting.api.RemotingEndPoint;
import link.thingscloud.netty.remoting.api.RemotingService;
import link.thingscloud.netty.remoting.api.RequestProcessor;
import link.thingscloud.netty.remoting.api.channel.ChannelEventListener;
import link.thingscloud.netty.remoting.api.channel.ChannelEventListenerGroup;
import link.thingscloud.netty.remoting.api.channel.RemotingChannel;
import link.thingscloud.netty.remoting.api.command.RemotingCommand;
import link.thingscloud.netty.remoting.api.command.RemotingCommandFactory;
import link.thingscloud.netty.remoting.api.command.TrafficType;
import link.thingscloud.netty.remoting.api.exception.RemotingAccessException;
import link.thingscloud.netty.remoting.api.exception.RemotingRuntimeException;
import link.thingscloud.netty.remoting.api.exception.RemotingTimeoutException;
import link.thingscloud.netty.remoting.api.exception.SemaphoreExhaustedException;
import link.thingscloud.netty.remoting.api.interceptor.Interceptor;
import link.thingscloud.netty.remoting.api.interceptor.InterceptorGroup;
import link.thingscloud.netty.remoting.api.interceptor.RequestContext;
import link.thingscloud.netty.remoting.api.interceptor.ResponseContext;
import link.thingscloud.netty.remoting.common.ResponseFuture;
import link.thingscloud.netty.remoting.common.SemaphoreReleaseOnlyOnce;
import link.thingscloud.netty.remoting.config.RemotingConfig;
import link.thingscloud.netty.remoting.impl.command.CodecHelper;
import link.thingscloud.netty.remoting.impl.command.RemotingCommandFactoryImpl;
import link.thingscloud.netty.remoting.impl.command.RemotingSysResponseCode;
import link.thingscloud.netty.remoting.internal.RemotingUtil;
import link.thingscloud.netty.remoting.internal.ThreadHelper;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Slf4j
public abstract class AbstractRemotingServiceImpl implements RemotingService {

    /**
     * Semaphore to limit maximum number of on-going one-way requests, which protects system memory footprint.
     */
    private final Semaphore semaphoreOneway;

    /**
     * Semaphore to limit maximum number of on-going asynchronous requests, which protects system memory footprint.
     */
    private final Semaphore semaphoreAsync;

    /**
     * This map caches all on-going requests.
     */
    private final Map<Integer, ResponseFuture> ackTables = new ConcurrentHashMap<>(256);

    private final Map<Integer, RequestProcessor> processorTables = new ConcurrentHashMap<>(256);

    /**
     * Provides custom interceptor at the occurrence of beforeRequest and afterResponseReceived event.
     */
    private final InterceptorGroup interceptorGroup = new InterceptorGroup();

    protected final ChannelEventListenerGroup listenerGroup = new ChannelEventListenerGroup();
    /**
     * Executor to execute RequestProcessor without specific executor.
     */
    private final ExecutorService publicExecutor;
    /**
     * Invoke the async handler in this executor when process response.
     */
    private final ExecutorService asyncHandlerExecutor;

    /**
     * This scheduled executor provides the ability to govern on-going response table.
     */
    protected ScheduledExecutorService houseKeepingService = ThreadHelper.newSingleThreadScheduledExecutor("HouseKeepingService", true);

    private final RemotingCommandFactory remotingCommandFactory;

    /**
     * websocket binary frame or text frame
     */
    private final boolean binary;

    protected AbstractRemotingServiceImpl(RemotingConfig remotingConfig) {
        this.binary = remotingConfig.isBinary();
        this.semaphoreOneway = new Semaphore(remotingConfig.getOnewayInvokeSemaphore(), true);
        this.semaphoreAsync = new Semaphore(remotingConfig.getAsyncInvokeSemaphore(), true);
        this.publicExecutor = ThreadHelper.newFixedThreadPool(remotingConfig.getPublicExecutorThreads(), 10000, "Remoting-PublicExecutor", true);
        this.asyncHandlerExecutor = ThreadHelper.newFixedThreadPool(remotingConfig.getAsyncHandlerExecutorThreads(), 10000, "Remoting-AsyncExecutor", true);
        this.remotingCommandFactory = new RemotingCommandFactoryImpl();
    }

    @Override
    public void registerRequestProcessor(int requestCode, RequestProcessor processor) {
        processorTables.putIfAbsent(requestCode, processor);
    }

    @Override
    public void registerChannelEventListener(ChannelEventListener listener) {
        listenerGroup.registerChannelEventListener(listener);
    }

    @Override
    public RemotingCommandFactory commandFactory() {
        return remotingCommandFactory;
    }

    @Override
    public void start() {
        startUpHouseKeepingService();
    }

    @Override
    public void stop() {
        ThreadHelper.shutdownGracefully(houseKeepingService, 3000, TimeUnit.MILLISECONDS);
        ThreadHelper.shutdownGracefully(publicExecutor, 2000, TimeUnit.MILLISECONDS);
        ThreadHelper.shutdownGracefully(asyncHandlerExecutor, 2000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void registerInterceptor(Interceptor interceptor) {
        this.interceptorGroup.registerInterceptor(interceptor);
    }

    protected void startUpHouseKeepingService() {
        this.houseKeepingService.scheduleAtFixedRate(this::scanResponseTable, 3000, 1000, TimeUnit.MICROSECONDS);
    }

    void scanResponseTable() {
        final List<Integer> rList = new ArrayList<>();

        for (final Map.Entry<Integer, ResponseFuture> next : this.ackTables.entrySet()) {
            ResponseFuture responseFuture = next.getValue();

            if ((responseFuture.getBeginTimestamp() + responseFuture.getTimeoutMillis()) <= System.currentTimeMillis()) {
                rList.add(responseFuture.getRequestId());
            }
        }

        for (Integer requestId : rList) {
            ResponseFuture rf = this.ackTables.remove(requestId);
            if (rf != null) {
                log.warn("Removes timeout request {} ", rf.getRequestCommand());
                rf.setCause(new RemotingTimeoutException(String.format("Request to %s timeout", rf.getRemoteAddr()), rf.getTimeoutMillis()));
                executeAsyncHandler(rf);
            }
        }
    }

    public RemotingCommand invokeWithInterceptor(final Channel channel, final RemotingCommand request, long timeoutMillis) {
        request.setTrafficType(TrafficType.REQUEST_SYNC);
        final String remoteAddr = RemotingUtil.extractRemoteAddress(channel);
        this.interceptorGroup.beforeRequest(new RequestContext(RemotingEndPoint.REQUEST, remoteAddr, request));
        return this.invoke0(remoteAddr, channel, request, timeoutMillis);
    }

    private RemotingCommand invoke0(final String remoteAddr, final Channel channel, final RemotingCommand request, final long timeoutMillis) {
        try {
            final int requestId = request.getRequestId();
            final ResponseFuture responseFuture = new ResponseFuture(requestId, timeoutMillis);
            responseFuture.setRequestCommand(request);
            responseFuture.setRemoteAddr(remoteAddr);

            this.ackTables.put(requestId, responseFuture);

            ChannelFutureListener listener = f -> {
                if (f.isSuccess()) {
                    responseFuture.setSendRequestOK(true);
                    return;
                } else {
                    responseFuture.setSendRequestOK(false);

                    ackTables.remove(requestId);
                    responseFuture.setCause(new RemotingAccessException(RemotingUtil.extractRemoteAddress(channel), f.cause()));
                    responseFuture.putResponse(null);

                    log.warn("Send request command to {} failed !", remoteAddr);
                }
            };

            this.writeAndFlush(channel, request, listener);

            RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMillis);

            if (null == responseCommand) {
                if (responseFuture.isSendRequestOK()) {
                    responseFuture.setCause(new RemotingTimeoutException(RemotingUtil.extractRemoteAddress(channel), timeoutMillis));
                    throw responseFuture.getCause();
                } else {
                    throw responseFuture.getCause();
                }
            }

            return responseCommand;
        } finally {
            this.ackTables.remove(request.getRequestId());
        }
    }

    public void invokeAsyncWithInterceptor(final Channel channel, final RemotingCommand request, final AsyncHandler asyncHandler, long timeoutMillis) {
        request.setTrafficType(TrafficType.REQUEST_ASYNC);
        final String remoteAddr = RemotingUtil.extractRemoteAddress(channel);
        this.interceptorGroup.beforeRequest(new RequestContext(RemotingEndPoint.REQUEST, remoteAddr, request));
        this.invokeAsync0(remoteAddr, channel, request, asyncHandler, timeoutMillis);
    }

    private void invokeAsync0(final String remoteAddr, final Channel channel, final RemotingCommand request, final AsyncHandler asyncHandler, final long timeoutMillis) {
        boolean acquired = this.semaphoreAsync.tryAcquire();
        if (acquired) {
            final int requestId = request.getRequestId();

            SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);

            final ResponseFuture responseFuture = new ResponseFuture(requestId, timeoutMillis, asyncHandler, once);
            responseFuture.setRequestCommand(request);
            responseFuture.setRemoteAddr(remoteAddr);

            this.ackTables.put(requestId, responseFuture);
            try {
                ChannelFutureListener listener = f -> {
                    responseFuture.setSendRequestOK(f.isSuccess());
                    if (f.isSuccess()) {
                        return;
                    }

                    requestFail(requestId, new RemotingAccessException(RemotingUtil.extractRemoteAddress(channel), f.cause()));
                    log.warn("Send request command to channel {} failed.", remoteAddr);
                };

                this.writeAndFlush(channel, request, listener);
            } catch (Exception e) {
                requestFail(requestId, new RemotingAccessException(RemotingUtil.extractRemoteAddress(channel), e));
                log.error("Send request command to channel " + channel + " error !", e);
            }
        } else {
            String info = String.format("No available async semaphore to issue the request request %s", request.toString());
            requestFail(new ResponseFuture(request.getRequestId(), timeoutMillis, asyncHandler, null), new SemaphoreExhaustedException(info));
            log.error(info);
        }
    }

    public void invokeOnewayWithInterceptor(final Channel channel, final RemotingCommand request) {
        request.setTrafficType(TrafficType.REQUEST_ONEWAY);
        this.interceptorGroup.beforeRequest(new RequestContext(RemotingEndPoint.REQUEST, RemotingUtil.extractRemoteAddress(channel), request));
        this.invokeOneway0(channel, request);
    }

    private void invokeOneway0(final Channel channel, final RemotingCommand request) {
        boolean acquired = this.semaphoreOneway.tryAcquire();
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);
            try {
                final SocketAddress socketAddress = channel.remoteAddress();

                ChannelFutureListener listener = f -> {
                    once.release();
                    if (!f.isSuccess()) {
                        log.warn("Send request command to channel {} failed !", socketAddress);
                    }
                };

                this.writeAndFlush(channel, request, listener);
            } catch (Exception e) {
                once.release();
                log.error("Send request command to channel " + channel + " error !", e);
            }
        } else {
            String info = String.format("No available oneway semaphore to issue the request %s", request.toString());
            log.error(info);
        }
    }

    private void requestFail(final int requestId, final RemotingRuntimeException cause) {
        ResponseFuture responseFuture = ackTables.remove(requestId);
        if (responseFuture != null) {
            responseFuture.setSendRequestOK(false);
            responseFuture.putResponse(null);
            responseFuture.setCause(cause);
            executeAsyncHandler(responseFuture);
        }
    }

    private void requestFail(final ResponseFuture responseFuture, final RemotingRuntimeException cause) {
        responseFuture.setCause(cause);
        executeAsyncHandler(responseFuture);
    }

    protected void processMessageReceived(ChannelHandlerContext ctx, RemotingChannel channel, RemotingCommand command) throws Exception {
        if (command != null) {
            switch (command.getTrafficType()) {
                case REQUEST_ONEWAY:
                case REQUEST_ASYNC:
                case REQUEST_SYNC:
                    processRequestCommand(ctx, channel, command);
                    break;
                case RESPONSE:
                    processResponseCommand(ctx, channel, command);
                    break;
                default:
                    log.warn("The traffic type {} is NOT supported!", command.getTrafficType());
                    break;
            }
        }
    }

    public void processRequestCommand(final ChannelHandlerContext ctx, RemotingChannel channel, final RemotingCommand cmd) {
        RequestProcessor requestProcessor = this.processorTables.get(cmd.getCmdCode());

        if (requestProcessor == null) {
            final RemotingCommand response = commandFactory().createResponse(cmd);
            response.setOpCode(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED);
            this.writeAndFlush(ctx.channel(), response);
            log.warn("The command code {} is NOT supported!", cmd.getCmdCode());
            return;
        }

        Runnable run = buildProcessorTask(ctx, cmd, requestProcessor, channel);

        try {
            publicExecutor.submit(run);
        } catch (RejectedExecutionException e) {
            log.warn(String.format("Request %s from %s is rejected by server executor %s !", cmd,
                    RemotingUtil.extractRemoteAddress(ctx.channel()), publicExecutor));

            if (cmd.getTrafficType() != TrafficType.REQUEST_ONEWAY) {
                RemotingCommand response = remotingCommandFactory.createResponse(cmd);
                response.setOpCode(RemotingSysResponseCode.SYSTEM_BUSY);
                response.setRemark("SYSTEM_BUSY");
                this.writeAndFlush(ctx.channel(), response);
            }
        }
    }

    private void processResponseCommand(ChannelHandlerContext ctx, RemotingChannel channel, RemotingCommand response) {
        final ResponseFuture responseFuture = ackTables.remove(response.getRequestId());
        if (responseFuture != null) {
            responseFuture.setResponseCommand(response);
            responseFuture.release();

            this.interceptorGroup.afterResponseReceived(new ResponseContext(RemotingEndPoint.REQUEST,
                    RemotingUtil.extractRemoteAddress(ctx.channel()), responseFuture.getRequestCommand(), response));

            if (responseFuture.getAsyncHandler() != null) {
                executeAsyncHandler(responseFuture);
            } else {
                responseFuture.putResponse(response);
                responseFuture.release();
            }
        } else {
            log.warn("Response {} from {} doesn't have a matched request!", response, RemotingUtil.extractRemoteAddress(ctx.channel()));
        }
    }

    /**
     * Execute callback in callback executor. If callback executor is null, run directly in current thread
     */
    private void executeAsyncHandler(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = asyncHandlerExecutor;
        if (executor != null) {
            try {
                executor.submit(() -> {
                    try {
                        responseFuture.executeAsyncHandler();
                    } catch (Throwable e) {
                        log.warn("Execute async handler in specific executor exception, ", e);
                    } finally {
                        responseFuture.release();
                    }
                });
            } catch (Throwable e) {
                runInThisThread = true;
                log.warn("Execute async handler in executor exception, maybe the executor is busy now", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeAsyncHandler();
            } catch (Throwable e) {
                log.warn("Execute async handler in current thread exception", e);
            } finally {
                responseFuture.release();
            }
        }
    }

    private Runnable buildProcessorTask(final ChannelHandlerContext ctx, final RemotingCommand cmd, final RequestProcessor requestProcessor, final RemotingChannel channel) {
        return () -> {
            try {
                interceptorGroup.beforeRequest(new RequestContext(RemotingEndPoint.RESPONSE, RemotingUtil.extractRemoteAddress(ctx.channel()), cmd));
                RemotingCommand response = requestProcessor.processRequest(channel, cmd);
                interceptorGroup.afterResponseReceived(new ResponseContext(RemotingEndPoint.RESPONSE, RemotingUtil.extractRemoteAddress(ctx.channel()), cmd, response));
                handleResponse(response, cmd, ctx);
            } catch (Throwable e) {
                log.error(String.format("Process request %s error !", cmd.toString()), e);

                handleException(e, cmd, ctx);
            }
        };
    }

    private void handleResponse(RemotingCommand response, RemotingCommand cmd, ChannelHandlerContext ctx) {
        if (cmd.getTrafficType() != TrafficType.REQUEST_ONEWAY) {
            if (response != null) {
                try {
                    this.writeAndFlush(ctx.channel(), response);
                } catch (Throwable e) {
                    log.error(String.format("Process request %s success, but transfer response %s failed !", cmd, response), e);
                }
            }
        }

    }

    private void handleException(Throwable e, RemotingCommand cmd, ChannelHandlerContext ctx) {
        if (cmd.getTrafficType() != TrafficType.REQUEST_ONEWAY) {
            RemotingCommand response = remotingCommandFactory.createResponse(cmd);
            response.setOpCode(RemotingSysResponseCode.SYSTEM_ERROR);
            response.setRemark("SYSTEM_ERROR");
            this.writeAndFlush(ctx.channel(), response);
        }
    }

    private void writeAndFlush(final Channel channel, final Object msg) {
        if (binary) {
            channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(CodecHelper.encodeBytes(msg))));
        } else {
            channel.writeAndFlush(new TextWebSocketFrame(CodecHelper.encode(msg)));
        }
    }

    private void writeAndFlush(final Channel channel, final Object msg, final ChannelFutureListener listener) {
        if (binary) {
            channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(CodecHelper.encodeBytes(msg)))).addListener(listener);
        } else {
            channel.writeAndFlush(new TextWebSocketFrame(CodecHelper.encode(msg))).addListener(listener);
        }
    }

}
