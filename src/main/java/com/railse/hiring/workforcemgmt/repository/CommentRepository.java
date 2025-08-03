package com.railse.hiring.workforcemgmt.repository;

import com.railse.hiring.workforcemgmt.model.TaskComment;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class CommentRepository {

    private final List<TaskComment> comments = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public void addComment(TaskComment comment) {
        comment.setId(idGenerator.getAndIncrement());
        comments.add(comment);
    }

    public List<TaskComment> getCommentsForTask(Long taskId) {
        return comments.stream()
                .filter(c -> c.getTaskId().equals(taskId))
                .sorted(Comparator.comparingLong(TaskComment::getTimestamp))
                .collect(Collectors.toList());
    }
}
