package com.qianyu.taskrun;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "DELETE FROM task_run", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TaskRunApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 首次上报应新增任务() throws Exception {
        report("""
                {
                  "taskId": "TASK-001",
                  "shopId": "SHOP-001",
                  "platform": "TAOBAO",
                  "status": "RUNNING",
                  "startedAt": "2026-07-10T09:00:00",
                  "finishedAt": null,
                  "rowCount": 0,
                  "errorMessage": null
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("TASK-001"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.id").doesNotExist());

        assertThat(countByTaskId("TASK-001")).isEqualTo(1);
    }

    @Test
    void 相同taskId重复上报应更新原记录() throws Exception {
        report(runningRequest("TASK-UPSERT", "SHOP-001")).andExpect(status().isOk());
        jdbcTemplate.update(
                "UPDATE task_run SET updated_at = ? WHERE task_id = ?",
                Timestamp.valueOf("2026-01-01 00:00:00"),
                "TASK-UPSERT"
        );

        report("""
                {
                  "taskId": "TASK-UPSERT",
                  "shopId": "SHOP-002",
                  "platform": "DOUYIN",
                  "status": "SUCCESS",
                  "startedAt": "2026-07-10T09:00:00",
                  "finishedAt": "2026-07-10T09:10:00",
                  "rowCount": 1200,
                  "errorMessage": null
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.rowCount").value(1200))
                .andExpect(jsonPath("$.data.finishedAt").value("2026-07-10T09:10:00"));

        assertThat(countByTaskId("TASK-UPSERT")).isEqualTo(1);
        var row = jdbcTemplate.queryForMap(
                "SELECT shop_id, platform, status, finished_at, row_count, updated_at FROM task_run WHERE task_id = ?",
                "TASK-UPSERT"
        );
        assertThat(row.get("shop_id")).isEqualTo("SHOP-002");
        assertThat(row.get("platform")).isEqualTo("DOUYIN");
        assertThat(row.get("status")).isEqualTo("SUCCESS");
        assertThat(((Number) row.get("row_count")).longValue()).isEqualTo(1200L);
        assertThat(((Timestamp) row.get("finished_at")).toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 7, 10, 9, 10));
        assertThat(((Timestamp) row.get("updated_at")).toLocalDateTime())
                .isAfter(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void 分页和筛选应返回稳定排序结果() throws Exception {
        insertTask("TASK-1", "SHOP-001", "SUCCESS", 10, "2026-07-10 10:00:00", "2026-07-10 10:01:00");
        insertTask("TASK-2", "SHOP-001", "FAILED", 0, "2026-07-10 10:00:00", "2026-07-10 10:02:00");
        insertTask("TASK-3", "SHOP-002", "SUCCESS", 30, "2026-07-10 10:00:00", "2026-07-10 10:02:00");
        insertTask("TASK-4", "SHOP-001", "SUCCESS", 40, "2026-07-10 10:00:00", "2026-07-10 10:03:00");

        mockMvc.perform(get("/api/task-runs").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.records[0].taskId").value("TASK-4"))
                .andExpect(jsonPath("$.data.records[1].taskId").value("TASK-3"));

        mockMvc.perform(get("/api/task-runs").param("shopId", "SHOP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(get("/api/task-runs").param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(get("/api/task-runs")
                        .param("shopId", "SHOP-001")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].taskId").value("TASK-4"))
                .andExpect(jsonPath("$.data.records[1].taskId").value("TASK-1"));
    }

    @Test
    void 汇总接口应正确统计店铺任务() throws Exception {
        insertTask("SUM-R1", "SHOP-SUM", "RUNNING", 0, null, "2026-07-10 09:00:00");
        insertTask("SUM-R2", "SHOP-SUM", "RUNNING", 0, null, "2026-07-10 09:01:00");
        insertTask("SUM-S1", "SHOP-SUM", "SUCCESS", 100, "2026-07-10 09:10:00", "2026-07-10 09:10:00");
        insertTask("SUM-S2", "SHOP-SUM", "SUCCESS", 250, "2026-07-10 09:30:00", "2026-07-10 09:30:00");
        insertTask("SUM-F1", "SHOP-SUM", "FAILED", 20, "2026-07-10 09:20:00", "2026-07-10 09:20:00");
        insertTask("OTHER", "SHOP-OTHER", "SUCCESS", 999, "2026-07-10 10:00:00", "2026-07-10 10:00:00");

        mockMvc.perform(get("/api/task-runs/summary").param("shopId", "SHOP-SUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shopId").value("SHOP-SUM"))
                .andExpect(jsonPath("$.data.runningCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.successRowCount").value(350))
                .andExpect(jsonPath("$.data.latestSuccessFinishedAt").value("2026-07-10T09:30:00"));

        mockMvc.perform(get("/api/task-runs/summary").param("shopId", "SHOP-EMPTY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runningCount").value(0))
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.successRowCount").value(0))
                .andExpect(jsonPath("$.data.latestSuccessFinishedAt").value((Object) null));
    }

    @Test
    void 非法请求应统一返回400() throws Exception {
        List<String> invalidBodies = List.of(
                runningRequest(" ", "SHOP-001"),
                runningRequestWithRowCount(-1),
                """
                {"taskId":"BAD-SUCCESS","shopId":"SHOP-001","platform":"TAOBAO","status":"SUCCESS","startedAt":"2026-07-10T09:00:00","finishedAt":null,"rowCount":1,"errorMessage":null}
                """,
                """
                {"taskId":"BAD-FAILED","shopId":"SHOP-001","platform":"TAOBAO","status":"FAILED","startedAt":"2026-07-10T09:00:00","finishedAt":"2026-07-10T09:10:00","rowCount":1,"errorMessage":" "}
                """
        );

        for (String body : invalidBodies) {
            report(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.data").value((Object) null));
        }

        report("""
                {"taskId":"BAD-STATUS","shopId":"SHOP-001","platform":"TAOBAO","status":"UNKNOWN","startedAt":"2026-07-10T09:00:00","rowCount":0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status只能是RUNNING、SUCCESS或FAILED"));

        mockMvc.perform(get("/api/task-runs").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 超长字段应在写入数据库前返回400() throws Exception {
        String tooLongTaskId = "T".repeat(101);

        report(runningRequest(tooLongTaskId, "SHOP-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("taskId长度不能超过100"));
    }

    @Test
    void 标识字段首尾空格应规范化保存和查询() throws Exception {
        report("""
                {"taskId":" TASK-TRIM ","shopId":" SHOP-TRIM ","platform":" TAOBAO ","status":"RUNNING","startedAt":"2026-07-10T09:00:00","finishedAt":null,"rowCount":0,"errorMessage":null}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("TASK-TRIM"))
                .andExpect(jsonPath("$.data.shopId").value("SHOP-TRIM"))
                .andExpect(jsonPath("$.data.platform").value("TAOBAO"));

        assertThat(countByTaskId("TASK-TRIM")).isEqualTo(1);
        mockMvc.perform(get("/api/task-runs").param("shopId", " SHOP-TRIM "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/task-runs/summary").param("shopId", " SHOP-TRIM "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runningCount").value(1));
    }

    @Test
    void 状态时间和必填字段校验应返回400() throws Exception {
        List<String> invalidBodies = List.of(
                """
                {"taskId":"BAD-SHOP","shopId":" ","platform":"TAOBAO","status":"RUNNING","startedAt":"2026-07-10T09:00:00","rowCount":0}
                """,
                """
                {"taskId":"BAD-PLATFORM","shopId":"SHOP-001","platform":" ","status":"RUNNING","startedAt":"2026-07-10T09:00:00","rowCount":0}
                """,
                """
                {"taskId":"BAD-STATUS-NULL","shopId":"SHOP-001","platform":"TAOBAO","status":null,"startedAt":"2026-07-10T09:00:00","rowCount":0}
                """,
                """
                {"taskId":"BAD-START","shopId":"SHOP-001","platform":"TAOBAO","status":"RUNNING","startedAt":null,"rowCount":0}
                """,
                """
                {"taskId":"BAD-COUNT","shopId":"SHOP-001","platform":"TAOBAO","status":"RUNNING","startedAt":"2026-07-10T09:00:00","rowCount":null}
                """,
                """
                {"taskId":"BAD-RUNNING","shopId":"SHOP-001","platform":"TAOBAO","status":"RUNNING","startedAt":"2026-07-10T09:00:00","finishedAt":"2026-07-10T09:10:00","rowCount":0}
                """,
                """
                {"taskId":"BAD-FAILED-TIME","shopId":"SHOP-001","platform":"TAOBAO","status":"FAILED","startedAt":"2026-07-10T09:00:00","finishedAt":null,"rowCount":0,"errorMessage":"失败"}
                """,
                """
                {"taskId":"BAD-TIME-ORDER","shopId":"SHOP-001","platform":"TAOBAO","status":"SUCCESS","startedAt":"2026-07-10T09:10:00","finishedAt":"2026-07-10T09:00:00","rowCount":0}
                """
        );

        for (String body : invalidBodies) {
            report(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Test
    void 分页边界和非法筛选参数应返回明确结果() throws Exception {
        insertTask("PAGE-1", "SHOP-PAGE", "RUNNING", 0, null, "2026-07-10 09:00:00");

        mockMvc.perform(get("/api/task-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
        mockMvc.perform(get("/api/task-runs").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());
        mockMvc.perform(get("/api/task-runs").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/task-runs").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/task-runs").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status只能是RUNNING、SUCCESS或FAILED"));
    }

    @Test
    void 没有成功任务时汇总成功字段应为零和null() throws Exception {
        insertTask("NO-SUCCESS-R", "SHOP-NO-SUCCESS", "RUNNING", 0, null, "2026-07-10 09:00:00");
        insertTask("NO-SUCCESS-F", "SHOP-NO-SUCCESS", "FAILED", 10, "2026-07-10 09:10:00", "2026-07-10 09:10:00");

        mockMvc.perform(get("/api/task-runs/summary").param("shopId", "SHOP-NO-SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runningCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.successRowCount").value(0))
                .andExpect(jsonPath("$.data.latestSuccessFinishedAt").value((Object) null));

        mockMvc.perform(get("/api/task-runs/summary"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/task-runs/summary").param("shopId", " "))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions report(String body) throws Exception {
        return mockMvc.perform(post("/api/task-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String runningRequest(String taskId, String shopId) {
        return """
                {"taskId":"%s","shopId":"%s","platform":"TAOBAO","status":"RUNNING","startedAt":"2026-07-10T09:00:00","finishedAt":null,"rowCount":0,"errorMessage":null}
                """.formatted(taskId, shopId);
    }

    private String runningRequestWithRowCount(long rowCount) {
        return """
                {"taskId":"BAD-ROW","shopId":"SHOP-001","platform":"TAOBAO","status":"RUNNING","startedAt":"2026-07-10T09:00:00","finishedAt":null,"rowCount":%d,"errorMessage":null}
                """.formatted(rowCount);
    }

    private int countByTaskId(String taskId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_run WHERE task_id = ?",
                Integer.class,
                taskId
        );
    }

    private void insertTask(
            String taskId,
            String shopId,
            String status,
            long rowCount,
            String finishedAt,
            String updatedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO task_run
                    (task_id, shop_id, platform, status, started_at, finished_at, row_count, error_message, updated_at)
                VALUES (?, ?, 'TAOBAO', ?, '2026-07-10 09:00:00', ?, ?, ?, ?)
                """,
                taskId,
                shopId,
                status,
                finishedAt == null ? null : Timestamp.valueOf(finishedAt),
                rowCount,
                "FAILED".equals(status) ? "采集失败" : null,
                Timestamp.valueOf(updatedAt)
        );
    }
}
