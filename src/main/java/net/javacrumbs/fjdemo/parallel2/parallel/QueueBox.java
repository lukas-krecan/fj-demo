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
import java.util.*;
import java.util.List;

import static java.util.Arrays.stream;

public class QueueBox extends JPanel {
    static final Color[] TASK_COLORS = new Color[]{
            Color.BLACK,
            Color.RED,
            Color.BLUE,
            Color.MAGENTA,
    };

    private final QueueCanvas queueCanvas;
    private List<Task> tasks = Collections.emptyList();

    public QueueBox() {
        setLayout(new BorderLayout());
        queueCanvas = new QueueCanvas();
        add(queueCanvas, BorderLayout.CENTER);
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }


    private class QueueCanvas extends JPanel {
        private static final int WIDTH = 100;
        private static final int HEIGHT = 300;
        private static final int TASK_HEIGHT = 10;

        public QueueCanvas() {
            setBorder(BorderFactory.createLineBorder(Color.black));
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                if (task != null) {
                    g.setColor(TASK_COLORS[task.getTaskId() % TASK_COLORS.length]);
                    g.drawRect(task.getStart(), tasks.size() * TASK_HEIGHT - (i + 1) * TASK_HEIGHT, task.getWidth(), TASK_HEIGHT);
                }
            }
        }
    }
}
