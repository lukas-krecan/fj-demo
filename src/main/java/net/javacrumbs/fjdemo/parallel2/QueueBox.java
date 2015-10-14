/**
 * Copyright 2009-2014 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.fjdemo.parallel2;

import static java.util.stream.IntStream.range;
import static net.javacrumbs.fjdemo.parallel2.Const.COLUMN_HEIGHT;
import static net.javacrumbs.fjdemo.parallel2.Const.COLUMN_WIDTH;
import static net.javacrumbs.fjdemo.parallel2.Const.TASK_COLORS;
import static net.javacrumbs.fjdemo.parallel2.Const.label;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;

public class QueueBox extends JComponent {
    private final List<JLabel> workLabels;

    public QueueBox() {
        setBorder(BorderFactory.createLineBorder(Color.black));
        workLabels = new LinkedList<>();
        range(0, 10).forEach(i -> {
            JLabel workLabel = label("");
            workLabels.add(0, workLabel);
            add(workLabel);
        });
    }

    public void setTasks(List<Task> tasks) {
        for (int i = 0; i < workLabels.size(); i++) {
            JLabel workLabel = workLabels.get(i);
            if (tasks.size() > i) {
                Task task = tasks.get(i);
                workLabel.setText(task.getInterval());
                workLabel.setForeground(TASK_COLORS[task.getTaskId() % TASK_COLORS.length]);
            } else {
                workLabel.setText("");
            }
        }
    }
}

