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

import static java.util.Arrays.stream;

public class ThreadBox extends JPanel {
    private static final Color[] TASK_COLORS = new Color[]{
            Color.BLACK,
            Color.RED,
            Color.BLUE,
            Color.MAGENTA,
    };

    private static final int MAX_TASKS = 30;
    private final ThreadCanvas threadCanvas;
    private int currentTask = 0;
    private Task[] tasks = new Task[MAX_TASKS];

    public ThreadBox(String name) {
        setLayout(new BorderLayout());
        threadCanvas = new ThreadCanvas();
        add(threadCanvas, BorderLayout.CENTER);
        add(new JLabel(name), BorderLayout.SOUTH);
    }

    public void addTask(Task task) {
        tasks[currentTask++] = task;
    }

    public void removeTask(Task task) {
        for (int i = 0; i < MAX_TASKS; i++) {
            if (task == tasks[i]) {
                tasks[i] = null;
            }
        }
        if (stream(tasks).allMatch(t -> t == null)) {
            currentTask = 0;
        }
    }

    public void highlightTask(Task task) {
    }

    private class ThreadCanvas extends JPanel {
        private static final int WIDTH = 100;
        private static final int HEIGHT = 300;
        private static final int TASK_HEIGHT = 10;

        public ThreadCanvas() {
            setBorder(BorderFactory.createLineBorder(Color.black));
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int i = 0; i < MAX_TASKS; i++) {
                Task task = tasks[i];
                if (task != null) {
                    g.setColor(TASK_COLORS[task.getTaskId() % TASK_COLORS.length]);
                    g.drawRect(task.getStart(), HEIGHT - (i + 1) * TASK_HEIGHT, task.getWidth(), TASK_HEIGHT);
                }
            }
        }
    }
}
