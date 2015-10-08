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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.IntStream.range;

public class QueueBox extends JPanel {
    static final Color[] TASK_COLORS = new Color[]{
        Color.BLACK,
        Color.RED,
        Color.BLUE,
        Color.MAGENTA,
    };

    private final QueuePanel queuePanel;

    public QueueBox() {
        setLayout(new BorderLayout());
        queuePanel = new QueuePanel();
        add(queuePanel, BorderLayout.CENTER);
    }

    public void setTasks(List<Task> tasks) {
        queuePanel.setTasks(tasks);
    }


    private class QueuePanel extends JPanel {
        private static final int WIDTH = 100;
        private static final int HEIGHT = 300;
        private final List<JLabel> workLabels;

        public QueuePanel() {
            setBorder(BorderFactory.createLineBorder(Color.black));
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            workLabels = new LinkedList<>();
            range(0, 10).forEach(i -> {
                JLabel workLabel = new JLabel();
                workLabels.add(0,workLabel);
                add(workLabel);
            });
        }

        public void setTasks(List<Task> tasks) {
            for (int i = 0; i < workLabels.size(); i++) {
                JLabel workLabel = workLabels.get(i);
                if (tasks.size() > i) {
                    Task task = tasks.get(i);
                    workLabel.setText(task.getStart() + "-" + (task.getStart() + task.getWidth()));
                    workLabel.setForeground(TASK_COLORS[task.getTaskId() % TASK_COLORS.length]);
                } else {
                    workLabel.setText("");
                }
            }
        }
    }
}
