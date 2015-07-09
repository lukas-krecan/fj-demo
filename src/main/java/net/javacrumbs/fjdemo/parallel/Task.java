package net.javacrumbs.fjdemo.parallel;

public interface Task {
    int getSize();

    String getSubtaskId();

    int getTaskId();
}
