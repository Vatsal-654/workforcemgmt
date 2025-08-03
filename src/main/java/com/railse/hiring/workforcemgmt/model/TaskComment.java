package com.railse.hiring.workforcemgmt.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskComment {
    private Long id;
    private Long taskId;
    private String commenter; // or userId
    private String comment;
    private Long timestamp;
}
