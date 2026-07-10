package com.qianyu.taskrun.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class TaskRunConstraintViolationClassifier {

    private static final String TASK_ID_CONSTRAINT = "uk_task_run_task_id";

    public boolean isTaskIdUniqueConflict(DuplicateKeyException exception) {
        boolean matchingSqlCode = false;
        boolean matchingConstraint = false;

        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains(TASK_ID_CONSTRAINT)) {
                matchingConstraint = true;
            }
            if (cause instanceof SQLException sqlException) {
                matchingSqlCode = sqlException.getErrorCode() == 1062
                        || "23505".equals(sqlException.getSQLState());
            }
        }
        return matchingSqlCode && matchingConstraint;
    }
}
