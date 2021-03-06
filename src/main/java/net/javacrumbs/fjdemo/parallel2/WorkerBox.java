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

import javax.swing.*;
import java.awt.*;

public class WorkerBox extends JPanel {
    public WorkerBox(ThreadBox threadBox, QueueBox submission, QueueBox work) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JPanel queues = new JPanel();
        queues.setLayout(new FlowLayout());
        queues.add(submission);
        queues.add(work);

        add(threadBox);
        add(queues);
        add(Box.createVerticalGlue());

    }
}
