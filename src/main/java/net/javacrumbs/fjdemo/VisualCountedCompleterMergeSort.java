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


import java.util.Arrays;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinTask;

import javax.swing.*;

public class VisualCountedCompleterMergeSort extends AbstractVisualForkJoinMergeSort {

    @Override
    protected ForkJoinTask<Void> createTask(int[] numbers) {
        return new SortTask(null, numbers, 0, 0);
    }

    private class SortTask extends CountedCompleter<Void> {
        private final int[] numbers;
        private final int row;
        private final int col;
        private final JLabel label;
        private SortTask taskLeft;
        private SortTask taskRight;

        public SortTask(SortTask completer, int[] numbers, int row, int col) {
            super(completer);
            this.numbers = numbers;
            this.row = row;
            this.col = col;
            this.label = createLabel(col, row, numbers);
        }

        @Override
        public void compute() {
            System.out.println("Thread " + threadNo() + ": Sorting " + Arrays.toString(numbers));
            setLabelColor(label, threadColor());
            switch (numbers.length) {
                case 1:
                    taskFinished();
                    tryComplete();
                    return;
                case 2:
                    swapIfNeeded(numbers);
                    taskFinished();
                    tryComplete();
                    return;
                default:
                    // not sorting in place as we should to make implementation more simple
                    int[] left = getLeftHalf(numbers);
                    int[] right = getRightHalf(numbers);
                    setLabelColor(label, COLOR_WAIT);
                    setPendingCount(1);
                    taskLeft = new SortTask(this, left, row + 1, col);
                    taskRight = new SortTask(this, right, row + 1, col + left.length);
                    taskRight.fork();
                    taskLeft.compute();
            }
        }


        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            if (caller != this) {
                merge(label, taskLeft.numbers, taskRight.numbers, numbers);
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
        VisualCountedCompleterMergeSort demo = new VisualCountedCompleterMergeSort();
        demo.start();
    }
}

