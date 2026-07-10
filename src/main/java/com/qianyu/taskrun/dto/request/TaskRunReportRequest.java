package com.qianyu.taskrun.dto.request;

import com.qianyu.taskrun.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskRunReportRequest {

    @NotBlank(message = "taskId不能为空")
    @Size(max = 100, message = "taskId长度不能超过100")
    private String taskId;

    @NotBlank(message = "shopId不能为空")
    @Size(max = 100, message = "shopId长度不能超过100")
    private String shopId;

    @NotBlank(message = "platform不能为空")
    @Size(max = 50, message = "platform长度不能超过50")
    private String platform;

    @NotNull(message = "status不能为空")
    private TaskStatus status;

    @NotNull(message = "startedAt不能为空")
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @NotNull(message = "rowCount不能为空")
    @PositiveOrZero(message = "rowCount不能小于0")
    private Long rowCount;

    private String errorMessage;
}
