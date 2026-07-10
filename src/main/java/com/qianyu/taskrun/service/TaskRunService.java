package com.qianyu.taskrun.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qianyu.common.api.PageResponse;
import com.qianyu.common.exception.BusinessException;
import com.qianyu.taskrun.dto.request.TaskRunReportRequest;
import com.qianyu.taskrun.dto.response.TaskRunResponse;
import com.qianyu.taskrun.dto.response.TaskRunSummaryResponse;
import com.qianyu.taskrun.entity.TaskRun;
import com.qianyu.taskrun.enums.TaskStatus;
import com.qianyu.taskrun.repository.TaskRunConstraintViolationClassifier;
import com.qianyu.taskrun.repository.TaskRunRepository;
import com.qianyu.taskrun.repository.TaskRunSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskRunService {

    private final TaskRunRepository taskRunRepository;
    private final TaskRunConstraintViolationClassifier constraintViolationClassifier;

    @Transactional
    public TaskRunResponse report(TaskRunReportRequest request) {
        validateBusinessRules(request);
        TaskRun taskRun = toEntity(request, LocalDateTime.now());
        TaskRun existing = taskRunRepository.selectByTaskId(taskRun.getTaskId());

        if (existing != null) {
            updateExisting(taskRun);
            return toResponse(taskRun);
        }

        try {
            taskRunRepository.insert(taskRun);
        } catch (DuplicateKeyException exception) {
            if (!constraintViolationClassifier.isTaskIdUniqueConflict(exception)
                    || taskRunRepository.updateBusinessFieldsByTaskId(taskRun) != 1) {
                throw exception;
            }
        }
        return toResponse(taskRun);
    }

    public PageResponse<TaskRunResponse> page(int page, int size, String shopId, TaskStatus status) {
        String normalizedShopId = StringUtils.hasText(shopId) ? shopId.trim() : null;
        IPage<TaskRun> result = taskRunRepository.selectPageByCondition(
                new Page<>(page, size),
                normalizedShopId,
                status
        );
        List<TaskRunResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, page, size, result.getTotal(), result.getPages());
    }

    public TaskRunSummaryResponse summary(String shopId) {
        String normalizedShopId = shopId.trim();
        TaskRunSummaryRow summary = taskRunRepository.selectSummaryByShopId(normalizedShopId);
        return TaskRunSummaryResponse.builder()
                .shopId(normalizedShopId)
                .runningCount(valueOrZero(summary.getRunningCount()))
                .successCount(valueOrZero(summary.getSuccessCount()))
                .failedCount(valueOrZero(summary.getFailedCount()))
                .successRowCount(valueOrZero(summary.getSuccessRowCount()))
                .latestSuccessFinishedAt(summary.getLatestSuccessFinishedAt())
                .build();
    }

    private void validateBusinessRules(TaskRunReportRequest request) {
        LocalDateTime finishedAt = request.getFinishedAt();
        if (finishedAt != null && finishedAt.isBefore(request.getStartedAt())) {
            throw new BusinessException("完成时间不能早于开始时间");
        }
        switch (request.getStatus()) {
            case RUNNING -> {
                if (finishedAt != null) {
                    throw new BusinessException("运行中的任务不能包含完成时间");
                }
            }
            case SUCCESS -> {
                if (finishedAt == null) {
                    throw new BusinessException("成功任务必须包含完成时间");
                }
            }
            case FAILED -> {
                if (finishedAt == null) {
                    throw new BusinessException("失败任务必须包含完成时间");
                }
                if (!StringUtils.hasText(request.getErrorMessage())) {
                    throw new BusinessException("失败任务必须包含错误信息");
                }
            }
        }
    }

    private TaskRun toEntity(TaskRunReportRequest request, LocalDateTime updatedAt) {
        TaskRun taskRun = new TaskRun();
        taskRun.setTaskId(request.getTaskId().trim());
        taskRun.setShopId(request.getShopId().trim());
        taskRun.setPlatform(request.getPlatform().trim());
        taskRun.setStatus(request.getStatus());
        taskRun.setStartedAt(request.getStartedAt());
        taskRun.setFinishedAt(request.getFinishedAt());
        taskRun.setRowCount(request.getRowCount());
        taskRun.setErrorMessage(request.getErrorMessage());
        taskRun.setUpdatedAt(updatedAt);
        return taskRun;
    }

    private void updateExisting(TaskRun taskRun) {
        if (taskRunRepository.updateBusinessFieldsByTaskId(taskRun) != 1) {
            throw new IllegalStateException("任务记录更新失败");
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private TaskRunResponse toResponse(TaskRun taskRun) {
        return TaskRunResponse.builder()
                .taskId(taskRun.getTaskId())
                .shopId(taskRun.getShopId())
                .platform(taskRun.getPlatform())
                .status(taskRun.getStatus())
                .startedAt(taskRun.getStartedAt())
                .finishedAt(taskRun.getFinishedAt())
                .rowCount(taskRun.getRowCount())
                .errorMessage(taskRun.getErrorMessage())
                .updatedAt(taskRun.getUpdatedAt())
                .build();
    }
}
