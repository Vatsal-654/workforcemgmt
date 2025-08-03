package com.railse.hiring.workforcemgmt.repository;

import com.railse.hiring.workforcemgmt.model.TaskActivity;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class ActivityRepository {

    private final List<TaskActivity> activities = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public void addActivity(TaskActivity activity) {
        activity.setId(idGenerator.getAndIncrement());
        activities.add(activity);
    }

    public List<TaskActivity> getActivitiesForTask(Long taskId) {
        return activities.stream()
                .filter(a -> a.getTaskId().equals(taskId))
                .sorted(Comparator.comparingLong(TaskActivity::getTimestamp))
                .collect(Collectors.toList());
    }
}
