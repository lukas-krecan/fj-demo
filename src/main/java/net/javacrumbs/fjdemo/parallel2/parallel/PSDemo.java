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
package net.javacrumbs.fjdemo.parallel2.parallel;


import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class PSDemo {
    private static int FJ_THREADS = 4;
    private static int EX_THREADS = 4;

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
        frame.setSize(1024, 640);
        frame.setLayout(new BorderLayout());

        JPanel verticalPanel = new JPanel();
        verticalPanel.setLayout(new BorderLayout());

        JPanel fjPanel = new JPanel();
        fjPanel.setLayout(new FlowLayout());

        for (int i = 0; i < FJ_THREADS; i++) {
            QueueBox submissionQueue = new QueueBox();
            queueBoxes[i * 2] = submissionQueue;
            QueueBox workerQueue = new QueueBox();
            queueBoxes[i * 2 + 1] = workerQueue;
            ThreadBox threadBox = new ThreadBox();
            fjThreadBoxes[i] = threadBox;
            fjPanel.add(new WorkerBox(threadBox, submissionQueue, workerQueue, "Worker " + (i + 1)));
        }

        JPanel exPanel = new JPanel();
        exPanel.setLayout(new FlowLayout());

        for (int i = 0; i < EX_THREADS; i++) {
            ThreadBox threadBox = new ThreadBox();
            exThreadBoxes[i] = threadBox;
            exPanel.add(threadBox);
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> executor.execute(this::runCalculation));
        buttonPanel.add(startButton);

        verticalPanel.add(fjPanel, BorderLayout.CENTER);
        verticalPanel.add(exPanel, BorderLayout.SOUTH);

        frame.add(verticalPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void runCalculation() {
        Stream<Integer> stream = StreamSupport.stream(new SwingLoggingSpliteratorWrapper<>(range(0, 1000).spliterator(), taskIdGenerator.getAndIncrement(), 0, null), true);

        stream.parallel().collect(maxBy(Comparator.<Integer>naturalOrder()));
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
            Thread currentThread = currentThread();
            ThreadBox currentThreadBox = getThreadBox(currentThread);
            if (FOR_EACH_REMAINING.equals(message) ||  SPLIT.equals(message)) {
                threadSafe(() -> currentThreadBox.setTask(this, message));
            }
            if (FOR_EACH_REMAINING_END.equals(message)) {
                threadSafe(() -> currentThreadBox.setTask(null, ""));
            }
            logQueues();
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
                }
            }
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

        @Override
        public String toString() {
            return getIdentifier() + "-" + (getOwnerThread() != null ? getOwnerThread().getName() : null);
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
