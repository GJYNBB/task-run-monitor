package com.qianyu.taskrun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qianyu.taskrun.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("task_run")
public class TaskRun {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String shopId;
    private String platform;
    private TaskStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long rowCount;
    private String errorMessage;
    private LocalDateTime updatedAt;
}
