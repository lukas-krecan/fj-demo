package net.javacrumbs.fjdemo;
//
//Copyright  2008-10  The original author or authors
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.


import javax.swing.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Shows use of the ForkJoin mechanics to implement merge sort.
 * <p>
 * Author: Vaclav Pech, Lukas Krecan, Pavel Jetensky
 * Date: Mar 26, 2011
 */

public class VisualForkJoinMergeSort extends AbstractVisualForkJoinMergeSort {

    @Override
    protected ForkJoinTask<Void> createTask(int[] numbers) {
        return new SortTask(numbers, 0, 0);
    }

    private class SortTask extends RecursiveAction {
        private static final long serialVersionUID = -686960611134519371L;

        private final int[] numbers;
        private final int row;
        private final int col;
        private final JLabel label;

        public SortTask(int[] numbers, int row, int col) {
            this.numbers = numbers;
            this.row = row;
            this.col = col;
            this.label = createLabel(col, row, numbers);
        }

        @Override
        protected void compute() {
            System.out.println("Thread " + threadNo() + ": Sorting " + Arrays.toString(numbers));
            setLabelColor(label, threadColor());
            switch (numbers.length) {
                case 1:
                    finishTask();
                    return;
                case 2:
                    if (numbers[0] > numbers[1]) {
                        int tmp = numbers[0];
                        numbers[0] = numbers[1];
                        numbers[1] = tmp;
                    }
                    finishTask();
                    return;
                default:
                    Map<Integer, int[]> split = split(numbers);
                    int[] a = split.get(0);
                    int[] b = split.get(1);
                    setLabelColor(label, COLOR_WAIT);
                    SortTask taskA = new SortTask(a, row + 1, col);
                    SortTask taskB = new SortTask(b, row + 1, col + a.length);
                    taskB.fork();
                    taskA.compute();
                    taskB.join();
                    merge(label, a, b, numbers);
                    finishTask();
            }
        }

        /**
         * Finishes the task
         *
         * @return
         */
        private void finishTask() {
            threadSafe(() -> {
                label.setText(Arrays.toString(numbers));
                label.setBackground(COLOR_FINISHED);
            });
        }
    }

    public static void main(String[] args) {
        VisualForkJoinMergeSort demo = new VisualForkJoinMergeSort();
        demo.start();
    }
}

