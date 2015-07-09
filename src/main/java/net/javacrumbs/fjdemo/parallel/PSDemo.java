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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.maxBy;
import static java.util.stream.IntStream.range;

public class PSDemo {
    private static int THREADS = 4;

    static {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(THREADS));
    }

    private final ThreadBox[] fjThreadBoxes = new ThreadBox[THREADS];
    private final JPanel panel = new JPanel();
    private final Executor executor = Executors.newFixedThreadPool(4);

    private void start() {
        JFrame frame = new JFrame("Visualisation of parallel stream processing");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1024, 640);
        frame.setLayout(new BorderLayout());

        panel.setLayout(new FlowLayout());

        for (int i = 0; i < THREADS; i++) {
            ThreadBox tb = new ThreadBox();
            panel.add(tb);
            fjThreadBoxes[i] = tb;
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> {
            executor.execute(this::runCalculation);
        });
        buttonPanel.add(startButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void runCalculation() {
        Stream<Integer> stream = StreamSupport.stream(new SwingLoggingSpliteratorWrapper<>(range(0, 1000).spliterator(), 0, null), true);

        stream.parallel().collect(maxBy(Comparator.<Integer>naturalOrder()));
    }

    private class SwingLoggingSpliteratorWrapper<T> extends LoggingSpliteratorWrapper<T> {

        public static final String FJ_THREAD_NAME_PREFIX = "ForkJoinPool.commonPool-worker-";

        public SwingLoggingSpliteratorWrapper(Spliterator<T> wrapped, int taskId, Task parentTask) {
            super(wrapped, taskId, parentTask);
        }

        @Override
        protected LoggingSpliteratorWrapper<T> createNewInstance(Spliterator<T> spliterator, int taskId) {
            return new SwingLoggingSpliteratorWrapper<>(spliterator, taskId, this);
        }

        @Override
        protected void log(String message) {
            super.log(message);
            String threadName = getThreadName();

            if (isFJThread(threadName)) {
                int threadNo = getThreadNo(threadName);
                if (CREATED.equals(message)) {
                    threadSafe(() -> fjThreadBoxes[threadNo].addTask(this));
                }
                if (FOR_EACH_REMAINING.equals(message)) {
                    threadSafe(() -> fjThreadBoxes[threadNo].removeTask(this));
                }
                if (STOLEN.equals(message)) {
                    String ownerThreadName = getOwnerThread().getName();
                    if (isFJThread(ownerThreadName)) {
                        threadSafe(() -> fjThreadBoxes[getThreadNo(ownerThreadName)].removeTask(this));
                    }
                }
            }
        }

        private boolean isFJThread(String threadName) {
            return threadName.startsWith(FJ_THREAD_NAME_PREFIX);
        }

        public int getThreadNo(String threadName) {
            return Integer.parseInt(threadName.substring(threadName.lastIndexOf('-') + 1));
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
            panel.repaint();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new PSDemo().start();
    }
}
