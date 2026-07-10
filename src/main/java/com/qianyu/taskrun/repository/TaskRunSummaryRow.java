package com.qianyu.taskrun.repository;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskRunSummaryRow {

    private Long runningCount;
    private Long successCount;
    private Long failedCount;
    private Long successRowCount;
    private LocalDateTime latestSuccessFinishedAt;
}
