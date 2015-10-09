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
package net.javacrumbs.fjdemo.parallel2;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;

public class LoggingSpliteratorWrapper<T> implements Spliterator<T>, Task {
    public static final String CREATED = "created";
    public static final String FOR_EACH_REMAINING = "processing";
    public static final String FOR_EACH_REMAINING_END = "forEachRemainingEnd";
    public static final String STOLEN = "stolen";
    public static final String SPLIT = "split";
    public static final int SCALE = 10;
    private final Spliterator<T> wrapped;
    private final int taskId;
    private final int from;

    private final Task parentTask;
    private Thread ownerThread;

    public LoggingSpliteratorWrapper(Spliterator<T> wrapped, int taskId, int from, Task parentTask) {
        this.taskId = taskId;
        this.parentTask = parentTask;
        this.wrapped = requireNonNull(wrapped);
        this.ownerThread = currentThread();
        this.from = from;
        logAndSleep(CREATED);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        return wrapped.tryAdvance(action);
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        logAndSleep(FOR_EACH_REMAINING);
        wrapped.forEachRemaining(action);
        logAndSleep(FOR_EACH_REMAINING_END);
    }

    @Override
    public Spliterator<T> trySplit() {
        logAndSleep(SPLIT);
        return createNewInstance(wrapped.trySplit(), taskId, from + (int) estimateSize());
    }

    protected LoggingSpliteratorWrapper<T> createNewInstance(Spliterator<T> spliterator, int taskId, int from) {
        return new LoggingSpliteratorWrapper<>(spliterator, taskId, from, this);
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

    private void logAndSleep(String message) {
        if (currentThread() != getOwnerThread()) {
            log(STOLEN);
            ownerThread = currentThread();
        }
        log(message);
        sleep();
    }

    protected void log(String message) {
//        System.out.println(getThreadName() + " " + getIdentifier() + " " + (parentTask != null ? parentTask.getIdentifier() : "x") + " " + wrapped.estimateSize() + " " + message);
        System.out.println(getThreadName() + " " + getIdentifier() + " " + message);
    }

    protected String getThreadName() {
        return currentThread().getName();
    }

    private void sleep() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getIdentifier() {
        return taskId + "[" + from + ".." + (from + estimateSize()) + "]";
    }

    @Override
    public String getInterval() {
        return from + "-" + (from + estimateSize());
    }

    @Override
    public int getTaskId() {
        return taskId;
    }

    public Task getParentTask() {
        return parentTask;
    }

    public Thread getOwnerThread() {
        return ownerThread;
    }


}


