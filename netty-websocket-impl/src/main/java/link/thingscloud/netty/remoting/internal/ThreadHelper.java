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

package link.thingscloud.netty.remoting.internal;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhouhailin
 * @since 0.2.0
 */
@Slf4j
public class ThreadHelper {

    private ThreadHelper() {
    }

    public static ThreadFactory newGenericThreadFactory(final String processName, final boolean isDaemon) {
        return new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, String.format("%s_%d", processName, this.threadIndex.incrementAndGet()));
                thread.setDaemon(isDaemon);
                return thread;
            }
        };
    }

    public static ExecutorService newFixedThreadPool(int nThreads, int workQueueCapacity, String processName, boolean isDaemon) {
        return new ThreadPoolExecutor(
                nThreads,
                nThreads,
                0,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(workQueueCapacity),
                newGenericThreadFactory(processName, isDaemon));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String processName, boolean isDaemon) {
        return Executors.newSingleThreadScheduledExecutor(newGenericThreadFactory(processName, isDaemon));
    }

    /**
     * An implementation of the graceful stop sequence recommended by
     * {@link ExecutorService}.
     *
     * @param executor executor
     * @param timeout  timeout
     * @param timeUnit timeUnit
     */
    public static void shutdownGracefully(ExecutorService executor, long timeout, TimeUnit timeUnit) {
        // Disable new tasks from being submitted.
        executor.shutdown();
        try {
            // Wait a while for existing tasks to terminate.
            if (!executor.awaitTermination(timeout, timeUnit)) {
                executor.shutdownNow();
                // Wait a while for tasks to respond to being cancelled.
                if (!executor.awaitTermination(timeout, timeUnit)) {
                    log.warn(String.format("%s didn't terminate!", executor));
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted.
            executor.shutdownNow();
            // Preserve interrupt status.
            Thread.currentThread().interrupt();
        }
    }

}
