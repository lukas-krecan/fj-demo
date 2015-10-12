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

import java.util.Arrays;
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
                    taskFinished();
                    return;
                case 2:
                    swapIfNeeded(numbers);
                    taskFinished();
                    return;
                default:
                    // not sorting in place as we should to make implementation more simple
                    int[] left = getLeftHalf(numbers);
                    int[] right = getRightHalf(numbers);
                    setLabelColor(label, COLOR_WAIT);
                    SortTask taskLeft = new SortTask(left, row + 1, col);
                    SortTask taskRight = new SortTask(right, row + 1, col + left.length);
                    taskRight.fork();
                    taskLeft.compute();
                    taskRight.join();
                    merge(label, left, right, numbers);
                    taskFinished();
            }
        }

        /**
         * Finishes the task
         *
         * @return
         */
        private void taskFinished() {
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

