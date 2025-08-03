package com.railse.hiring.workforcemgmt.service.impl;


import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.model.TaskComment;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.model.enums.Task;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.ActivityRepository;
import com.railse.hiring.workforcemgmt.repository.CommentRepository;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class TaskManagementServiceImpl implements TaskManagementService {


    private final TaskRepository taskRepository;
    private final ITaskManagementMapper taskMapper;
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ActivityRepository activityRepository;


    public TaskManagementServiceImpl(TaskRepository taskRepository, ITaskManagementMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }


    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        TaskManagementDto dto = taskMapper.modelToDto(task);
        dto.setComments(commentRepository.getCommentsForTask(id));
        dto.setActivities(activityRepository.getActivitiesForTask(id));
        return dto;
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();
        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");

            TaskManagement saved = taskRepository.save(newTask);
            createdTasks.add(saved);

            // ✨ Log activity
            activityRepository.addActivity(new TaskActivity(
                    null,
                    saved.getId(),
                    "Task created by " + createRequest.getCreatedBy(),
                    System.currentTimeMillis()
            ));
        }

        return taskMapper.modelListToDtoList(createdTasks);
    }


    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();
        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));

            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
            }

            TaskManagement saved = taskRepository.save(task);
            updatedTasks.add(saved);

            // ✨ Log activity
            activityRepository.addActivity(new TaskActivity(
                    null,
                    saved.getId(),
                    "Task updated by " + updateRequest.getUpdatedBy(),
                    System.currentTimeMillis()
            ));
        }
        return taskMapper.modelListToDtoList(updatedTasks);
    }



    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());


        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());


            // BUG #1 is here. It should assign one and cancel the rest.
            // Instead, it reassigns ALL of them.

//           Current problem with the code is When assigning tasks by reference,
//           all matching tasks are reassigned, even if duplicates exist.
//            And we want here is that it should reassign one and cancel the rest
//            if (!tasksOfType.isEmpty()) {
//                for (TaskManagement taskToUpdate : tasksOfType) {
//                    taskToUpdate.setAssigneeId(request.getAssigneeId());
//                    taskRepository.save(taskToUpdate);
//                }
//            }
            if (!tasksOfType.isEmpty()) {
                boolean assignedOne = false;
                for (TaskManagement taskToUpdate : tasksOfType) {
                    if (!assignedOne) {
                        taskToUpdate.setAssigneeId(request.getAssigneeId());
                        taskRepository.save(taskToUpdate);
                        assignedOne = true;
                    }
                    else {
                        taskToUpdate.setStatus(TaskStatus.CANCELLED);
                        taskToUpdate.setDescription("Cancelled due to duplicate assignment.");
                        taskRepository.save(taskToUpdate);
                    }
                }
            }

            else {
                // Create a new task if none exist
                TaskManagement newTask = new TaskManagement();
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(newTask);
            }
        }
        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }


//New Method Introduced for performance
//@Override
public TaskManagementDto updateTaskPriority(Long taskId, Priority priority, String updatedBy) {
    TaskManagement task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

    task.setPriority(priority);
    taskRepository.save(task);

    // ✨ Log activity
    activityRepository.addActivity(new TaskActivity(
            null,
            taskId,
            "Priority changed to " + priority,
            System.currentTimeMillis()
    ));

    return taskMapper.modelToDto(task);
}

    //New Method Introduced for performance
    @Override
    public List<TaskManagementDto> getTasksByPriority(Priority priority) {
        List<TaskManagement> tasks = taskRepository.findAll().stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
        return taskMapper.modelListToDtoList(tasks);
    }



    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());


        // BUG #2 is here. It should filter out CANCELLED tasks but doesn't.
        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task ->
//                task.getStatus() != TaskStatus.CANCELLED &&
//                task.getTaskDeadlineTime() != null &&
//                task.getTaskDeadlineTime() >= start &&
//                task.getTaskDeadlineTime() <= end
//                )

                task.getStatus() != TaskStatus.CANCELLED)

//                        This is to be used in case we just want to solve the bug but if we want to improve the performace then use the below filters
//                        .filter(task -> request.getStartDate() == null || task.getTaskDeadlineTime() >= request.getStartDate())
//                        .filter(task -> request.getEndDate() == null || task.getTaskDeadlineTime() <= request.getEndDate())
                .filter(task -> task.getStatus() == TaskStatus.ASSIGNED || task.getStatus() == TaskStatus.STARTED)
                .filter(task -> {
                    boolean withinRange = task.getTaskDeadlineTime() >= request.getStartDate()
                            && task.getTaskDeadlineTime() <= request.getEndDate();
                    boolean startedBeforeRangeAndStillOpen = task.getTaskDeadlineTime() < request.getStartDate();
                    return withinRange || startedBeforeRangeAndStillOpen;
                })


                //                 {
//                    // This logic is incomplete for the assignment.
//                    // It should check against startDate and endDate.
//                    // For now, it just returns all tasks for the assignees.
//                    return true;
//                })
                .collect(Collectors.toList());


        return taskMapper.modelListToDtoList(filteredTasks);
    }

    @Override
    public void addComment(Long taskId, String comment, String commentedBy) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        commentRepository.addComment(new TaskComment(null, taskId, comment, commentedBy, System.currentTimeMillis()));

        activityRepository.addActivity(new TaskActivity(
                null,
                taskId,
                "Comment added by " + commentedBy,
                System.currentTimeMillis()
        ));
    }


}




