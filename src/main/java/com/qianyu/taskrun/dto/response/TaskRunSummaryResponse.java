package com.qianyu.taskrun.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskRunSummaryResponse {

    private final String shopId;
    private final long runningCount;
    private final long successCount;
    private final long failedCount;
    private final long successRowCount;
    private final LocalDateTime latestSuccessFinishedAt;
}
