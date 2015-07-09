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
import java.util.Comparator;
import java.util.Spliterator;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.IntStream.range;

public class PSDemo {
    private static int FJ_THREADS = 4;
    private static int EX_THREADS = 4;

    static {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(FJ_THREADS));
    }

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
            ThreadBox tb = new ThreadBox("Executor " + (i+1));
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

        public static final String FJ_THREAD_NAME_PREFIX = "ForkJoinPool.commonPool-worker-";

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
            ThreadBox currentThreadBox = getThreadBox(currentThread());
            if (CREATED.equals(message)) {
                threadSafe(() -> currentThreadBox.addTask(this));
            }
            if (FOR_EACH_REMAINING.equals(message)) {
                threadSafe(() -> currentThreadBox.removeTask(this));
            }
            if (STOLEN.equals(message)) {
                threadSafe(() -> currentThreadBox.addTask(this));
                ThreadBox originalThreadBox = getThreadBox(getOwnerThread());
                threadSafe(() -> originalThreadBox.removeTask(this));
            }
        }

        private ThreadBox getThreadBox(Thread thread) {
            String threadName = thread.getName();
            int threadNo = Integer.parseInt(threadName.substring(threadName.lastIndexOf('-') + 1));
            if (isFJThread(threadName)) {
                return fjThreadBoxes[threadNo];
            } else {
                return exThreadBoxes[threadNo - 1];
            }
        }

        private boolean isFJThread(String threadName) {
            return threadName.startsWith(FJ_THREAD_NAME_PREFIX);
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
