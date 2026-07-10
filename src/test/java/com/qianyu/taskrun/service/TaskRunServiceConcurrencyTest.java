package com.qianyu.taskrun.service;

import com.qianyu.taskrun.dto.request.TaskRunReportRequest;
import com.qianyu.taskrun.entity.TaskRun;
import com.qianyu.taskrun.enums.TaskStatus;
import com.qianyu.taskrun.repository.TaskRunConstraintViolationClassifier;
import com.qianyu.taskrun.repository.TaskRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskRunServiceConcurrencyTest {

    @Mock
    private TaskRunRepository taskRunRepository;

    private TaskRunService taskRunService;

    @BeforeEach
    void setUp() {
        taskRunService = new TaskRunService(
                taskRunRepository,
                new TaskRunConstraintViolationClassifier()
        );
    }

    @Test
    void taskId唯一冲突应回退为更新() {
        when(taskRunRepository.selectByTaskId("TASK-CONCURRENT")).thenReturn(null);
        when(taskRunRepository.insert(any(TaskRun.class))).thenThrow(taskIdConflict());
        when(taskRunRepository.updateBusinessFieldsByTaskId(any(TaskRun.class))).thenReturn(1);

        taskRunService.report(runningRequest());

        verify(taskRunRepository).updateBusinessFieldsByTaskId(any(TaskRun.class));
    }

    @Test
    void 非taskId唯一冲突不应被误判为upsert() {
        when(taskRunRepository.selectByTaskId("TASK-CONCURRENT")).thenReturn(null);
        DuplicateKeyException otherConflict = new DuplicateKeyException(
                "other constraint",
                new SQLException("uk_other_constraint", "23505")
        );
        when(taskRunRepository.insert(any(TaskRun.class))).thenThrow(otherConflict);

        assertThatThrownBy(() -> taskRunService.report(runningRequest()))
                .isSameAs(otherConflict);

        verify(taskRunRepository, never()).updateBusinessFieldsByTaskId(any(TaskRun.class));
    }

    private DuplicateKeyException taskIdConflict() {
        return new DuplicateKeyException(
                "taskId conflict",
                new SQLException("uk_task_run_task_id", "23505")
        );
    }

    private TaskRunReportRequest runningRequest() {
        TaskRunReportRequest request = new TaskRunReportRequest();
        request.setTaskId("TASK-CONCURRENT");
        request.setShopId("SHOP-001");
        request.setPlatform("TAOBAO");
        request.setStatus(TaskStatus.RUNNING);
        request.setStartedAt(LocalDateTime.of(2026, 7, 10, 9, 0));
        request.setRowCount(0L);
        return request;
    }
}
