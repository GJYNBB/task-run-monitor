package com.qianyu.taskrun.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qianyu.taskrun.entity.TaskRun;
import com.qianyu.taskrun.enums.TaskStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskRunRepository extends BaseMapper<TaskRun> {

    TaskRun selectByTaskId(@Param("taskId") String taskId);

    int updateBusinessFieldsByTaskId(TaskRun taskRun);

    IPage<TaskRun> selectPageByCondition(
            Page<TaskRun> page,
            @Param("shopId") String shopId,
            @Param("status") TaskStatus status
    );

    TaskRunSummaryRow selectSummaryByShopId(@Param("shopId") String shopId);
}
