/*
 * Copyright (C) 2007-2015, GoodData(R) Corporation. All rights reserved.
 */
package net.javacrumbs.fjdemo;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public abstract class AbstractVisualForkJoinMergeSort {
    protected static final int ROW_HEIGHT = 40;
    protected static final int COL_WIDTH = 37;
    protected static final Color COLOR_WAIT = new Color(212, 146, 52);
    protected static final Color COLOR_SCHEDULED = new Color(134, 219, 52);
    protected static final Color COLOR_FINISHED = Color.GRAY;
    private static final Color[] THREAD_COLORS = new Color[]{
            Color.YELLOW,
            Color.CYAN,
            Color.WHITE,
            Color.RED,
            Color.PINK,
            Color.ORANGE,
            Color.MAGENTA,
            new Color(76, 160, 255)
    };
    private final Random random = new Random();
    protected JPanel panel;
    private JSlider numThreads;
    private JSlider problemSize;
    private JSlider speed;
    private JButton startButton;
    private JCheckBox randomCheckBox = new JCheckBox("Random data", false);
    private JCheckBox randomDelayCheckBox = new JCheckBox("Random speed", false);

    /**
     * Returns color of current thread.
     */
    protected static Color threadColor() {

        return THREAD_COLORS[(threadNo() - 1) % THREAD_COLORS.length];
    }

    protected static int threadNo() {
        String name = Thread.currentThread().getName();
        return name.charAt(name.length() - 1);
    }

    private JSlider createSlider(int min, int max, int value, final String message) {
        final JSlider result = new JSlider(min, max, value);
        result.setPaintTicks(true);
        result.setPaintLabels(true);
        result.setMinorTickSpacing(1);
        final TitledBorder border = BorderFactory.createTitledBorder(message + " " + result.getValue());
        result.setBorder(border);
        result.addChangeListener(event -> border.setTitle(message + " " + result.getValue()));
        return result;
    }

    private JLabel newLabel(String text, Color color) {
        JLabel result = new JLabel(text);
        result.setBackground(color);
        result.setOpaque(true);
        result.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        setDefaultFont(result);
        return result;
    }

    public void start() {
        JFrame frame = new JFrame("Visualisation of merge sort using fork join");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1024, 640);

        frame.setLayout(new BorderLayout());

        panel = new JPanel();
        panel.setLayout(null);


        numThreads = createSlider(1, 8, 3, "Number of threads");
        problemSize = createSlider(4, 64, 32, "Problem size");
        speed = new JSlider(0, 1000, 700);
        speed.setBorder(BorderFactory.createTitledBorder("Speed"));

        //Creates Start Button
        startButton = new JButton("Start");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                numThreads.setEnabled(false);
                problemSize.setEnabled(false);
                startButton.setEnabled(false);
                panel.setPreferredSize(new Dimension(problemSize.getValue() * COL_WIDTH, 7 * ROW_HEIGHT));
                panel.removeAll();
                new Thread(AbstractVisualForkJoinMergeSort.this::runDemo).start();
            }
        });


        Box vbox = Box.createVerticalBox();

        Box hbox1 = Box.createHorizontalBox();
        hbox1.add(new JLabel("<html><ol><li>Thread takes a task from the queue. If the tasks is too big (longer than two elements in our case) it is split to two smaller tasks</li><li>The subtasks are placed to the queue to be processed</li> <li>While the task waits for its subtasks to finish, the thread is free to take another task from the queue (step 1.)</li> <li>When the subtasks are finished their results are merged</li> </ol> </html>"));
        vbox.add(hbox1);

        JPanel panel1 = new JPanel();
        panel1.add(newLabel("Waiting in queue", COLOR_SCHEDULED));
        panel1.add(newLabel("Waiting for subtasks", COLOR_WAIT));
        panel1.add(newLabel("Finished", COLOR_FINISHED));
        for (int i = 0; i < THREAD_COLORS.length; i++) {
            panel1.add(newLabel("Thread " + (i + 1), THREAD_COLORS[i]));
        }
        vbox.add(panel1);

        Box hbox2 = Box.createHorizontalBox();
        hbox2.add(speed);
        hbox2.add(numThreads);
        hbox2.add(problemSize);
        vbox.add(hbox2);


        Box hbox3 = Box.createHorizontalBox();
        hbox3.add(startButton);
        hbox3.add(randomCheckBox);
        hbox3.add(randomDelayCheckBox);
        vbox.add(hbox3);


        frame.add(vbox, BorderLayout.NORTH);
        frame.add(new JScrollPane(panel), BorderLayout.CENTER);
        frame.setVisible(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Sets color of the label
     *
     * @param label
     * @param color
     */
    protected void setLabelColor(final JLabel label, final Color color) {
        final Color threadColor = threadColor();
        threadSafe(() -> {
            label.setBackground(color);
            label.setForeground(threadColor);
        });
    }

    /**
     * Runs closure in Swing Event thread and repaints the panel. Sleeps after the change.
     *
     * @param r
     */
    protected void threadSafe(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
            panel.repaint();
            if (randomDelayCheckBox.isSelected()) {
                Thread.sleep((long) ((1000 - speed.getValue()) * (random.nextFloat() + 0.5)));
            } else {
                Thread.sleep(1000 - speed.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Splits a list of numbers in half
     */
    protected Map<Integer, int[]> split(int[] list) {
        int listSize = list.length;
        int middleIndex = listSize / 2;
        Map<Integer, int[]> result = new HashMap<>();
        result.put(0, Arrays.copyOf(list, middleIndex));
        result.put(1, Arrays.copyOfRange(list, middleIndex, list.length));
        return result;
    }

    /**
     * Merges two sorted lists into one
     */
    protected int[] merge(JLabel label, int[] a, int[] b, int[] result) {
        setLabelColor(label, threadColor());
        int i = 0, j = 0, idx = 0;

        while ((i < a.length) && (j < b.length)) {
            if (a[i] <= b[j]) {
                result[idx] = a[i++];
            } else {
                result[idx] = b[j++];
            }
            idx++;
        }

        if (i < a.length) {
            for (; i < a.length; i++) {
                result[idx++] = a[i];
            }
        } else {
            for (; j < b.length; j++) {
                result[idx++] = b[j];
            }
        }
        return result;
    }

    /**
     * Executes the demo.
     */
    private void runDemo() {
        ForkJoinPool threadPool = new ForkJoinPool(numThreads.getValue());
        int[] numbers = new int[problemSize.getValue()];

        for (int i = 0; i < numbers.length; i++) {
            if (randomCheckBox.isSelected()) {
                numbers[i] = random.nextInt(100);
            } else {
                numbers[i] = problemSize.getValue() - i;
            }
        }
        threadPool.invoke(createTask(numbers));

        threadSafe(() -> {
            numThreads.setEnabled(true);
            problemSize.setEnabled(true);
            startButton.setEnabled(true);
        });
        System.out.println("Sorted numbers: " + Arrays.toString(numbers));
    }



    /**
     * Creates label that visualizes the task
     *
     * @return
     * @param col
     * @param row
     * @param numbers
     */
    protected JLabel createLabel(int col, int row, int[] numbers) {
        final JLabel label = new JLabel(" " + Arrays.toString(numbers));
        label.setBounds(col * COL_WIDTH, row * ROW_HEIGHT + 20, numbers.length * COL_WIDTH, ROW_HEIGHT);
        label.setBackground(COLOR_SCHEDULED);
        label.setOpaque(true);
        label.setToolTipText(Arrays.toString(numbers));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        label.setForeground(threadColor());
        setDefaultFont(label);
        threadSafe(() -> panel.add(label));
        return label;
    }

    protected abstract ForkJoinTask<Void> createTask(int[] numbers);

    protected void setDefaultFont(JLabel label) {
        label.setFont(new Font("Arial Black", Font.BOLD, 16));
    }
}
