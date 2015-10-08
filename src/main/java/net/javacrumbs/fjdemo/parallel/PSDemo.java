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


import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.IntStream.range;

public class PSDemo {
    private static int FJ_THREADS = 4;
    private static int EX_THREADS = 4;

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

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        for (int i = 0; i < EX_THREADS; i++) {
            ThreadBox tb = new ThreadBox("Executor " + (i + 1));
            panel.add(tb);
            exThreadBoxes[i] = tb;
        }

        for (int i = 0; i < FJ_THREADS; i++) {
            ThreadBox tb = new ThreadBox("Worker " + (i + 1));
            panel.add(tb);
            fjThreadBoxes[i] = tb;
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> executor.execute(this::runCalculation));
        buttonPanel.add(startButton);

        frame.add(panel, BorderLayout.CENTER);
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
            if (CREATED.equals(message)) {
                threadSafe(() -> currentThreadBox.addTask(this));
            }
            if (FOR_EACH_REMAINING.equals(message)) {
                threadSafe(() -> currentThreadBox.removeTask(this));
            }
            if (FOR_EACH_REMAINING_END.equals(message)) {
                threadSafe(() -> currentThreadBox.highlightTask(this));
            }
            if (STOLEN.equals(message)) {
                if (currentThread.getName().startsWith("pool")) {
                    System.out.println("strange");
                }
                threadSafe(() -> currentThreadBox.addTask(this));
                ThreadBox originalThreadBox = getThreadBox(getOwnerThread());
                threadSafe(() -> originalThreadBox.removeTask(this));
            }
            logQueues(currentThread);
        }

        private void logQueues(Thread currentThread) {

            //implementation specific
            try {
                Object[] ws = (Object[]) getFieldValue(ForkJoinPool.commonPool(), "workQueues");
                if (ws != null) {
                    //Worker queues are at odd indices. Shared (submission) queues are at even indices,

                    for (int i = 0; i < ws.length; i += 1) {
                        if (ws[i] != null) {
                            Object workQueue = ws[i];
                            ForkJoinTask<?>[] queue = (ForkJoinTask<?>[]) getFieldValue(workQueue, "array");
                            int base = (int) getFieldValue(workQueue, "base");
                            int top = (int) getFieldValue(workQueue, "top");
                            if (base != top) {
                                String string = Arrays.stream(Arrays.copyOfRange(queue, base, top)).map(this::getSpliterator).collect(joining(", "));
                                System.out.println("Queue content: " + i + " " + string);
                            }
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private String getSpliterator(ForkJoinTask<?> object) {
            try {
                return Objects.toString(getFieldValue(Class.forName("java.util.stream.AbstractTask"), object, "spliterator"));
            } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (NullPointerException ignore) {
                return null;
            }
        }

        private Object getFieldValue(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
            return getFieldValue(object.getClass(), object, fieldName);
        }

        private Object getFieldValue(Class<?> sourceClass, Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
            Field field = sourceClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        }

        @Override
        public String toString() {
            return getIdentifier() + "-" + (getOwnerThread() != null ? getOwnerThread().getName() : null);
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
