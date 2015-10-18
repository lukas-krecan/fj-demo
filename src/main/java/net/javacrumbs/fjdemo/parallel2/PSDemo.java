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


import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static net.javacrumbs.fjdemo.parallel2.Const.label;
import static net.javacrumbs.fjdemo.parallel2.LoggingSpliteratorWrapper.FOR_EACH_REMAINING;
import static net.javacrumbs.fjdemo.parallel2.LoggingSpliteratorWrapper.FOR_EACH_REMAINING_END;
import static net.javacrumbs.fjdemo.parallel2.LoggingSpliteratorWrapper.SPLIT;
import static net.javacrumbs.fjdemo.parallel2.LoggingSpliteratorWrapper.sleep;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.*;

public class PSDemo {

    private static final String MERGE = "merge";
    private static final String MERGE_FINISHED = "mergeFinished";

    static {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");
    }

    private static int FJ_THREADS = 4;
    private static int EX_THREADS = 10;

    private final QueueBox[] queueBoxes = new QueueBox[FJ_THREADS * 2];
    private final ThreadBox[] fjThreadBoxes = new ThreadBox[FJ_THREADS];
    private final ThreadBox[] exThreadBoxes = new ThreadBox[EX_THREADS];
    private final JFrame frame = new JFrame("Visualisation of parallel stream processing");
    private final Executor executor = new ThreadPoolExecutor(EX_THREADS, EX_THREADS,
        Long.MAX_VALUE, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());
    private final AtomicInteger taskIdGenerator = new AtomicInteger();

    private void start() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1300, 450);
        frame.setLayout(new BorderLayout());

        JPanel threadPanel = new JPanel();
        threadPanel.setLayout(new BoxLayout(threadPanel, BoxLayout.PAGE_AXIS));

        Box fjPanel = Box.createHorizontalBox();

        for (int i = 0; i < FJ_THREADS; i++) {
            QueueBox submissionQueue = new QueueBox("submission");
            queueBoxes[i * 2] = submissionQueue;
            QueueBox workerQueue = new QueueBox("worker");
            queueBoxes[i * 2 + 1] = workerQueue;
            ThreadBox threadBox = new ThreadBox(null, FlowLayout.CENTER);
            fjThreadBoxes[i] = threadBox;
            fjPanel.add(new WorkerBox(threadBox, submissionQueue, workerQueue));
        }

        for (int i = 0; i < EX_THREADS; i++) {
            JLabel caption = label("Thread " + (i + 1) + ": ");
            ThreadBox threadBox = new ThreadBox(caption, FlowLayout.LEADING);
            exThreadBoxes[i] = threadBox;
            threadPanel.add(threadBox);
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> executor.execute(this::runCalculation));
        buttonPanel.add(startButton);


        frame.add(threadPanel, BorderLayout.WEST);
        frame.add(fjPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }
    private void runCalculation() {
        int taskId = taskIdGenerator.getAndIncrement();
        SwingLoggingSpliteratorWrapper<Integer> spliterator = new SwingLoggingSpliteratorWrapper<>(range(0, 1000).spliterator(), taskId, 0, null);
        Stream<Integer> stream = StreamSupport.stream(spliterator, true);

        stream.parallel().collect(() -> new NumberCollector(taskId, this::logTask), NumberCollector::append, NumberCollector::combine);
    }

    /**
     * Assumes sorted numbers on input.
     */
    private static class NumberCollector {
        private final int taskId;
        private final BiConsumer<String, Task> logger;
        private Integer from;
        private Integer to;

        private NumberCollector(int taskId, BiConsumer<String, Task> logger) {
            this.taskId = taskId;
            this.logger = logger;
        }

        private void append(Integer i) {
            if (from == null) {
                from = i;
            }
            to = i;
        }

        private void combine(NumberCollector other) {
            logger.accept(MERGE, new Task() {
                @Override
                public String getIdentifier() {
                    return taskId + "[" + getInterval() + "]";
                }

                @Override
                public String getInterval() {
                    return from + "-" + (to + 1) + "-" + (other.to + 1);
                }

                @Override
                public int getTaskId() {
                    return taskId;
                }
            });
            from = Math.min(from, other.from);
            to = Math.max(to, other.to);
            sleep();
            logger.accept(MERGE_FINISHED, null);
        }
    }

    void logTask(String message, Task task) {
        Thread currentThread = currentThread();
        ThreadBox currentThreadBox = getThreadBox(currentThread);
        if (FOR_EACH_REMAINING.equals(message) ||  SPLIT.equals(message) || MERGE.equals(message)) {
            threadSafe(() -> currentThreadBox.setTask(task, message));
        }
        if (FOR_EACH_REMAINING_END.equals(message) || MERGE_FINISHED.equals(message)) {
            threadSafe(() -> currentThreadBox.setTask(null, ""));
        }
//        if (STOLEN.equals(message) && !isFJThread(currentThread)) {
//            System.out.println("Ha");
//        }
        logQueues();
    }

    private class SwingLoggingSpliteratorWrapper<T> extends LoggingSpliteratorWrapper<T> {
        public SwingLoggingSpliteratorWrapper(Spliterator<T> wrapped, int taskId, int from, Task parentTask) {
            super(wrapped, taskId, from, parentTask);
        }

        @Override
        protected LoggingSpliteratorWrapper<T> createNewInstance(Spliterator<T> spliterator, int taskId, int from) {
            return new SwingLoggingSpliteratorWrapper<>(spliterator, taskId, from, this);
        }

        @Override
        protected void log(String message) {
            super.log(message);
            logTask(message, this);
        }

        @Override
        public String toString() {
            return getIdentifier() + "-" + (getOwnerThread() != null ? getOwnerThread().getName() : null);
        }
    }

    private ThreadBox getThreadBox(Thread thread) {
        String threadName = thread.getName();
        int threadNo = Integer.parseInt(threadName.substring(threadName.lastIndexOf('-') + 1));
        if (isFJThread(thread)) {
            return fjThreadBoxes[threadNo];
        } else {
            return exThreadBoxes[threadNo % EX_THREADS];
        }
    }

    private boolean isFJThread(Thread thread) {
        return thread instanceof ForkJoinWorkerThread;
    }

    private void logQueues() {
        //implementation specific
        Object[] ws = (Object[]) getFieldValue(ForkJoinPool.commonPool(), "workQueues");
        if (ws != null) {
            //Worker queues are at odd indices. Shared (submission) queues are at even indices,
            for (int i = 0; i < ws.length; i += 1) {
                int j = i;
                List<Task> tasks = getTasks(ws[i]);
                threadSafe(() -> queueBoxes[j].setTasks(tasks));
                System.out.println("Queue content: " + i + " " + tasks);
            }
        }
    }


    private Object getFieldValue(Object object, String fieldName) {
        try {
            return getFieldValue(object.getClass(), object, fieldName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object getFieldValue(Class<?> sourceClass, Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = sourceClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }


    private List<Task> getTasks(Object workQueue) {
        if (workQueue != null) {
            ForkJoinTask<?>[] queue = (ForkJoinTask<?>[]) getFieldValue(workQueue, "array");
            int base = (int) getFieldValue(workQueue, "base");
            int top = (int) getFieldValue(workQueue, "top");
            if (base != top) {
                return Arrays.stream(Arrays.copyOfRange(queue, base, top)).map(this::getSpliterator).collect(toList());
            }
        }
        return Collections.emptyList();
    }


    private Task getSpliterator(ForkJoinTask<?> object) {
        try {
            return (Task) getFieldValue(Class.forName("java.util.stream.AbstractTask"), object, "spliterator");
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        } catch (NullPointerException ignore) {
            return null;
        }
    }


    /**
     * Runs closure in Swing Event thread and repaints the panel. Sleeps after the change.
     *
     * @param r
     */
    private void threadSafe(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
            frame.repaint();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new PSDemo().start();
    }
}
