package net.javacrumbs.fjdemo.parallel;

public interface Task {
    int getWidth();

    int getStart();

    String getIdentifier();

    int getTaskId();
}
