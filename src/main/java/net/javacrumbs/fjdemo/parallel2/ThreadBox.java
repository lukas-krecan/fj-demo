/**
 * Copyright 2009-2015 the original author or authors.
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

import javax.swing.*;
import java.awt.*;

import static net.javacrumbs.fjdemo.parallel2.Const.TASK_COLORS;
import static net.javacrumbs.fjdemo.parallel2.Const.label;

public class ThreadBox extends JPanel {
    private final JLabel work;
    private final JLabel label;

    public ThreadBox(JLabel caption, int align) {
        setLayout(new FlowLayout(align));
        work = label("");
        label = label("");
        label.setPreferredSize(new Dimension(Const.COLUMN_WIDTH, 40));
        if (caption != null) {
            add(caption);
        }
        add(label);
        add(work);
        setPreferredSize(new Dimension(Const.COLUMN_WIDTH * 3, 40));
    }

    public void setTask(Task task, String message) {
        label.setText(message);
        if (task != null) {
            work.setText(task.getInterval());
            work.setForeground(TASK_COLORS[task.getTaskId() % TASK_COLORS.length]);
        } else {
            work.setText("");
        }
    }
}
