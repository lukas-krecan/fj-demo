/**
 * Copyright 2009-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.fjdemo.parallel;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class LoggingSpliteratorWrapper<T> implements Spliterator<T> {
    private final Spliterator<T> wrapped;
    private static final AtomicInteger idGenerator = new AtomicInteger();
    private final int taskId;
    private final int subtaskId;
    private final LogHandler logHandler;

    public LoggingSpliteratorWrapper(Spliterator<T> wrapped, LogHandler logHandler, int taskId) {
        this.taskId = taskId;
        this.wrapped = requireNonNull(wrapped);
        this.logHandler = requireNonNull(logHandler);
        this.subtaskId = idGenerator.getAndIncrement();
        log("created");
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        return wrapped.tryAdvance(action);
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        log("forEachRemaining");
        wrapped.forEachRemaining(action);
    }

    @Override
    public Spliterator<T> trySplit() {
        LoggingSpliteratorWrapper<T> newSpliterator = new LoggingSpliteratorWrapper<>(wrapped.trySplit(), logHandler, taskId);
        log("split");
        return newSpliterator;
    }

    @Override
    public long estimateSize() {
        return wrapped.estimateSize();
    }

    @Override
    public Comparator<? super T> getComparator() {
        return wrapped.getComparator();
    }

    @Override
    public int characteristics() {
        return wrapped.characteristics();
    }

    private void log(String message) {
        logHandler.handleLog(Thread.currentThread().getName(), taskId, subtaskId, wrapped.estimateSize(), message);
    }

    public interface LogHandler {
        void handleLog(String thread, int taskId, int subtaskId, long size, String message);
    }
}
