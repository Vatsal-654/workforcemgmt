package com.railse.hiring.workforcemgmt.service;

import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.model.TaskComment;
import com.railse.hiring.workforcemgmt.model.enums.Priority;

import java.util.List;

public interface TaskManagementService {
    List<TaskManagementDto> createTasks(TaskCreateRequest request);
    List<TaskManagementDto> updateTasks(UpdateTaskRequest request);
    String assignByReference(AssignByReferenceRequest request);
    List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request);
    void addComment(Long taskId, String comment, String commentedBy);
    TaskManagementDto findTaskById(Long taskId); // already exists, but now returns with full detail


//     new methods introduced for performance improvement

//    TaskManagementDto updateTaskPriority(Long taskId, Priority priority);
TaskManagementDto updateTaskPriority(Long taskId, Priority priority, String updatedBy);

    List<TaskManagementDto> getTasksByPriority(Priority priority);

}
