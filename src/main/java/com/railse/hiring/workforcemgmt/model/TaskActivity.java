package com.railse.hiring.workforcemgmt.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskActivity {
    private Long id;
    private Long taskId;
    private String action; // e.g., "Priority set to HIGH", "Task created"
    private Long timestamp;
}
