package com.qianyu.taskrun.controller;

import com.qianyu.common.api.ApiResponse;
import com.qianyu.common.api.PageResponse;
import com.qianyu.taskrun.dto.request.TaskRunReportRequest;
import com.qianyu.taskrun.dto.response.TaskRunResponse;
import com.qianyu.taskrun.dto.response.TaskRunSummaryResponse;
import com.qianyu.taskrun.enums.TaskStatus;
import com.qianyu.taskrun.service.TaskRunService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task-runs")
@RequiredArgsConstructor
public class TaskRunController {

    private final TaskRunService taskRunService;

    @PostMapping
    public ApiResponse<TaskRunResponse> report(@Valid @RequestBody TaskRunReportRequest request) {
        return ApiResponse.success(taskRunService.report(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<TaskRunResponse>> page(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "page必须大于等于1") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size必须大于等于1")
            @Max(value = 100, message = "size不能大于100") int size,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) TaskStatus status
    ) {
        return ApiResponse.success(taskRunService.page(page, size, shopId, status));
    }

    @GetMapping("/summary")
    public ApiResponse<TaskRunSummaryResponse> summary(
            @RequestParam @NotBlank(message = "shopId不能为空") String shopId
    ) {
        return ApiResponse.success(taskRunService.summary(shopId));
    }
}
