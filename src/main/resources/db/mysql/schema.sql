CREATE TABLE IF NOT EXISTS task_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id VARCHAR(100) NOT NULL,
    shop_id VARCHAR(100) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    row_count BIGINT NOT NULL,
    error_message TEXT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_task_run_task_id UNIQUE (task_id),
    INDEX idx_task_run_shop_status_updated (shop_id, status, updated_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
