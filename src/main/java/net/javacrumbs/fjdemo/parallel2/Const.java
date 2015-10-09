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

public class Const {
    static final int COLUMN_WIDTH = 100;
    static final int COLUMN_HEIGHT = 300;


    static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial Black", Font.BOLD, 16));
        return label;
    }

    static final Color[] TASK_COLORS = new Color[]{
        Color.BLUE,
        Color.GREEN,
        Color.RED,
        new Color(112,78,212),
        Color.MAGENTA,
        new Color(76, 160, 255),
        new Color(212, 146, 52),
        new Color(134, 219, 52),
        Color.GRAY
    };
}
