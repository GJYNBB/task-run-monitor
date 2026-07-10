package com.qianyu.taskrun.dto.response;

import com.qianyu.taskrun.enums.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskRunResponse {

    private final String taskId;
    private final String shopId;
    private final String platform;
    private final TaskStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final Long rowCount;
    private final String errorMessage;
    private final LocalDateTime updatedAt;
}
