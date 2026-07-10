你现在位于一个已经通过 IntelliJ IDEA 创建好的标准 Spring Boot 空项目根目录中。

请直接在当前仓库内完成代码开发，也不要破坏当前项目已有的包名、Spring Boot 版本、Java 版本和构建方式。

# 一、任务目标

实现一个“采集任务监控服务”，完成以下三个 REST API：

1. 新增或更新任务上报
2. 分页查询任务列表
3. 查询指定店铺的任务汇总

请优先保证：

- 项目能够启动
- 接口行为正确
- 相同 taskId 不产生重复数据
- 参数校验明确
- 测试可以重复运行
- 代码结构清晰、不过度设计

---

# 二、开始开发前先检查项目

请先检查当前仓库中的：

- pom.xml
- Spring Boot 版本
- Java 版本
- 当前基础包名
- 已有依赖
- 已有目录结构
- application.yml 或 application.properties
- 是否已经存在测试类
- 当前 Git 工作区状态

然后根据现有项目进行增量开发。

要求：

1. 不要随意修改现有 groupId、artifactId、基础包名。
2. 不要把 Spring Boot 版本改成其他大版本。
3. 如果项目使用 Maven，就继续使用 Maven。
4. 如果项目已有 Lombok，可以使用；如果没有，请使用Lombok来减少代码量。
5. 当前项目已配置MySQL数据库，连接信息在application.yml，数据库名叫task_run_database。
6. 当前项目请使用MyBatis-Plus持久化方案
8. 开发前先简要输出你识别到的项目情况和计划，再开始修改文件。

---

# 三、业务背景

业务系统会为多个店铺执行数据采集任务。

每次任务运行时或完成后，会向本服务上报：

- 任务编号
- 店铺编号
- 数据来源平台
- 当前状态
- 开始时间
- 完成时间
- 采集行数
- 错误信息

服务需要保存任务记录，并提供查询和汇总能力。

---

# 四、数据模型

创建采集任务运行记录模型，建议名称：

- Java 类：TaskRun
- 数据表：task_run

至少包含以下字段：

| 字段         | Java 类型建议 | 说明                          |
| ------------ | ------------- | ----------------------------- |
| id           | Long          | 数据库主键，自增              |
| taskId       | String        | 唯一任务编号，业务唯一键      |
| shopId       | String        | 店铺编号                      |
| platform     | String        | 数据来源，例如 TAOBAO、DOUYIN |
| status       | TaskStatus    | RUNNING、SUCCESS、FAILED      |
| startedAt    | LocalDateTime | 开始时间                      |
| finishedAt   | LocalDateTime | 完成时间，可为空              |
| rowCount     | Long          | 采集行数                      |
| errorMessage | String        | 错误信息，可为空              |
| updatedAt    | LocalDateTime | 最后更新时间，由服务端生成    |

要求：

1. taskId 必须有数据库唯一约束。
2. status 使用枚举，不要在业务代码中散落字符串比较。
3. updatedAt 由服务端生成，不接受客户端传入。
4. rowCount 使用 Long 或 Integer 均可，但必须能够校验非负。
5. errorMessage 长度应合理，可以使用较长字符串或 TEXT。
6. 不要直接使用 Entity 作为 Controller 的请求对象和响应对象。
7. 创建独立的 Request DTO 和 Response DTO。

任务状态枚举：

- RUNNING
- SUCCESS
- FAILED

---

# 五、接口一：新增或更新任务上报

接口：

POST /api/task-runs

## 5.1 请求体

示例：

{
  "taskId": "TASK-001",
  "shopId": "SHOP-001",
  "platform": "TAOBAO",
  "status": "SUCCESS",
  "startedAt": "2026-07-10T09:00:00",
  "finishedAt": "2026-07-10T09:10:00",
  "rowCount": 1200,
  "errorMessage": null
}

创建类似以下请求 DTO：

TaskRunReportRequest

包含：

- taskId
- shopId
- platform
- status
- startedAt
- finishedAt
- rowCount
- errorMessage

## 5.2 Upsert 规则

这是基于 taskId 的新增或更新操作。

如果 taskId 不存在：

- 新增一条记录。

如果 taskId 已经存在：

- 不得新增第二条记录。
- 更新原记录的业务字段。
- taskId 保持不变。
- 使用本次完整请求覆盖以下字段：
  - shopId
  - platform
  - status
  - startedAt
  - finishedAt
  - rowCount
  - errorMessage
- updatedAt 更新为当前服务器时间。

要求：

1. Repository 提供根据 taskId 查询的方法。
2. 数据库层必须设置 taskId 唯一约束。
3. Service 方法建议使用事务。
4. 不能仅依赖“先查询再插入”保证唯一性，数据库唯一约束必须存在。
5. 本阶段不要求实现复杂分布式锁。

## 5.3 参数校验

至少实现以下校验：

- taskId 不能为空或全为空格。
- shopId 不能为空或全为空格。
- platform 不能为空或全为空格。
- status 不能为空。
- startedAt 不能为空。
- rowCount 不能为空。
- rowCount 不能小于 0。
- finishedAt 如果存在，不能早于 startedAt。

状态相关校验：

### RUNNING

- finishedAt 应为空。
- 如果传入 finishedAt，返回明确的 400 错误。

### SUCCESS

- finishedAt 必须存在。

### FAILED

- finishedAt 必须存在。
- errorMessage 建议不能为空；本题中将其作为必填业务校验。

业务校验放在 Service 或独立 Validator 中，不要把所有逻辑堆积在 Controller。

## 5.4 返回结果

统一返回格式，例如：

{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "TASK-001",
    "shopId": "SHOP-001",
    "platform": "TAOBAO",
    "status": "SUCCESS",
    "startedAt": "2026-07-10T09:00:00",
    "finishedAt": "2026-07-10T09:10:00",
    "rowCount": 1200,
    "errorMessage": null,
    "updatedAt": "2026-07-10T09:10:03"
  }
}

可以创建通用响应类：

ApiResponse<T>

至少包含：

- code
- message
- data

HTTP 状态码应正确：

- 正常新增或更新：200
- 参数错误：400
- 服务端异常：500

---

# 六、接口二：分页查询任务列表

接口：

GET /api/task-runs

## 6.1 查询参数

支持：

- page：页码，对外从 1 开始，默认 1
- size：每页数量，默认 10
- shopId：可选
- status：可选

示例：

GET /api/task-runs?page=1&size=10

GET /api/task-runs?page=1&size=10&shopId=SHOP-001

GET /api/task-runs?page=1&size=10&status=FAILED

GET /api/task-runs?page=1&size=10&shopId=SHOP-001&status=SUCCESS

## 6.2 查询规则

1. 支持只按 shopId 筛选。
2. 支持只按 status 筛选。
3. 支持 shopId 和 status 组合筛选。
4. 两个筛选参数都不传时，查询全部。
5. 所有结果按 updatedAt 倒序。
6. 如果 updatedAt 相同，建议再按 id 倒序，保证顺序稳定。
7. 对外 page 从 1 开始。
8. 如果底层分页组件从 0 开始，需要在 Controller 或 Service 中转换。

## 6.3 分页校验

- page 必须大于等于 1。
- size 必须大于等于 1。
- size 最大限制为 100，超过时返回 400。
- status 非法时返回 400，不能返回空列表伪装成功。

## 6.4 推荐响应

{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "taskId": "TASK-001",
        "shopId": "SHOP-001",
        "platform": "TAOBAO",
        "status": "SUCCESS",
        "startedAt": "2026-07-10T09:00:00",
        "finishedAt": "2026-07-10T09:10:00",
        "rowCount": 1200,
        "errorMessage": null,
        "updatedAt": "2026-07-10T09:10:03"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 1,
    "totalPages": 1
  }
}

创建明确的分页响应 DTO，例如：

PageResponse<T>

包含：

- records
- page
- size
- total
- totalPages

不要把 JPA Page、MyBatis Page 或其他框架对象直接返回给客户端。

---

# 七、接口三：查询店铺任务汇总

接口：

GET /api/task-runs/summary?shopId=SHOP-001

## 7.1 参数要求

- shopId 必填。
- shopId 不能为空或全为空格。
- 缺失时返回 400。

## 7.2 汇总内容

返回指定店铺的：

1. RUNNING 状态任务数量。
2. SUCCESS 状态任务数量。
3. FAILED 状态任务数量。
4. 所有 SUCCESS 任务的 rowCount 总和。
5. 最近一次 SUCCESS 任务的 finishedAt。

推荐响应 DTO：

TaskRunSummaryResponse

字段：

- shopId
- runningCount
- successCount
- failedCount
- successRowCount
- latestSuccessFinishedAt

示例：

{
  "code": 200,
  "message": "success",
  "data": {
    "shopId": "SHOP-001",
    "runningCount": 2,
    "successCount": 5,
    "failedCount": 1,
    "successRowCount": 8600,
    "latestSuccessFinishedAt": "2026-07-10T09:10:00"
  }
}

## 7.3 空数据规则

如果该店铺没有任何任务：

{
  "shopId": "SHOP-001",
  "runningCount": 0,
  "successCount": 0,
  "failedCount": 0,
  "successRowCount": 0,
  "latestSuccessFinishedAt": null
}

如果有任务但没有成功任务：

- successCount 返回 0
- successRowCount 返回 0
- latestSuccessFinishedAt 返回 null

注意处理数据库 SUM 返回 null 的情况。

---

# 八、代码结构要求

基于当前项目的基础包名创建合理结构，建议：

controller
  TaskRunController

service
  TaskRunService
  TaskRunServiceImpl

repository
  TaskRunRepository

entity
  TaskRun

dto.request
  TaskRunReportRequest

dto.response
  TaskRunResponse
  TaskRunSummaryResponse
  PageResponse
  ApiResponse

enums
  TaskStatus

exception
  BusinessException
  GlobalExceptionHandler

不要为了这个小项目引入复杂的 DDD、CQRS、事件总线或大量抽象层。

职责要求：

## Controller

只负责：

- 接收 HTTP 参数
- Bean Validation
- 调用 Service
- 返回响应

## Service

负责：

- Upsert
- 业务规则校验
- 分页查询
- 汇总统计
- Entity 与 DTO 转换

## Repository

负责：

- taskId 查询
- 条件分页
- 统计查询

可以根据现有技术栈合理调整类名，但必须保持职责清晰。

---

# 九、异常处理

实现统一异常处理，使用：

@RestControllerAdvice

至少处理：

1. Bean Validation 参数错误。
2. 请求参数类型转换错误。
3. 非法枚举值。
4. 自定义业务异常。
5. 数据库唯一约束异常。
6. 未预期异常。

错误响应保持统一，例如：

{
  "code": 400,
  "message": "status 只能是 RUNNING、SUCCESS 或 FAILED",
  "data": null
}

要求：

- 参数错误返回 HTTP 400。
- 不要把完整异常堆栈返回给客户端。
- 服务端日志中保留必要异常信息。
- 错误信息使用中文，便于本项目演示。
- 不要对所有异常都返回 HTTP 200。

---



---

# 十、自动化测试

完成代码后，至少编写以下测试。

优先使用：

- Spring Boot Test
- MockMvc
- JUnit 5

如果当前项目已有测试风格，沿用现有方式。

## 测试一：首次上报新增任务

验证：

- POST 返回成功。
- 返回 taskId 正确。
- 数据库中存在一条任务。

## 测试二：相同 taskId 重复上报

步骤：

1. 第一次上报 RUNNING。
2. 第二次使用相同 taskId 上报 SUCCESS。
3. 查询数据库。

验证：

- 数据库中该 taskId 只有一条记录。
- status 更新为 SUCCESS。
- rowCount 更新为第二次的值。
- finishedAt 更新成功。
- updatedAt 已更新。

## 测试三：分页和筛选

准备多条不同 shopId、不同 status 的记录。

验证：

- 分页结果正确。
- shopId 筛选正确。
- status 筛选正确。
- 组合筛选正确。
- updatedAt 倒序正确。

## 测试四：汇总接口

验证：

- runningCount 正确。
- successCount 正确。
- failedCount 正确。
- successRowCount 正确。
- latestSuccessFinishedAt 正确。

## 测试五：非法请求

至少验证：

- 非法 status 返回 400。
- taskId 为空返回 400。
- rowCount 为负数返回 400。
- SUCCESS 没有 finishedAt 返回 400。
- FAILED 没有 errorMessage 返回 400。
- page 为 0 返回 400。

要求所有测试可以通过一条命令重复运行。

Maven 项目：

./mvnw test

如果没有 Maven Wrapper，则：

mvn test

Gradle 项目：

./gradlew test

---

# 十一、请求示例

在项目根目录创建 requests.http

requests.http 至少包含：

1. RUNNING 状态首次上报。
2. 相同 taskId 更新为 SUCCESS。
3. 分页查询。
4. shopId 筛选。
5. status 筛选。
6. 店铺汇总查询。
7. 一个非法参数请求。

示例：

### 第一次上报
POST http://localhost:8080/api/task-runs
Content-Type: application/json

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

### 更新为成功
POST http://localhost:8080/api/task-runs
Content-Type: application/json

{
  "taskId": "TASK-001",
  "shopId": "SHOP-001",
  "platform": "TAOBAO",
  "status": "SUCCESS",
  "startedAt": "2026-07-10T09:00:00",
  "finishedAt": "2026-07-10T09:10:00",
  "rowCount": 1200,
  "errorMessage": null
}

### 分页查询
GET http://localhost:8080/api/task-runs?page=1&size=10

### 组合筛选
GET http://localhost:8080/api/task-runs?page=1&size=10&shopId=SHOP-001&status=SUCCESS

### 汇总查询
GET http://localhost:8080/api/task-runs/summary?shopId=SHOP-001

---

# 十二、实现约束

必须遵守：

1. 不要只输出建议，直接修改当前仓库。
2. 不要新建另一个 Spring Boot 项目。
3. 不要重写已有项目配置，优先增量修改。
4. 不要实现前端。
5. 不要实现登录认证。
6. 不要引入 Redis、MQ、微服务等无关组件。
7. 不要使用 Map 作为主要接口返回模型。
8. 不要直接返回 Entity。
9. 不要把全部业务逻辑写在 Controller。
10. 不要忽略异常处理。
11. 不要伪造测试结果。
12. 不要在测试失败时声称已经完成。
13. 不要提交密码、Token、Cookie、密钥等敏感信息。
14. 不要为了追求复杂度使用不必要的设计模式。
15. 所有新增代码需要与项目现有 Java 版本兼容。

---

# 十三、执行与验证流程

请按照以下顺序执行：

1. 审查当前项目。
2. 输出简短的开发计划。
3. 补充必要依赖。
4. 创建数据模型和枚举。
5. 创建 Repository。
6. 创建 DTO。
7. 创建 Service。
8. 创建 Controller。
9. 创建统一异常处理。
10. 编写自动化测试。
11. 创建 requests.http。
12. 编译项目。
13. 运行全部测试。
14. 如有失败，定位并修复。
15. 再次运行全部测试。
16. 检查 Git diff。
18. 输出最终开发报告。

---

# 十四、完成后的输出格式

开发完成后，请输出以下内容：

## 1. 实现概述

简要说明三个接口分别如何实现。

## 2. 修改文件清单

列出新增和修改的文件，并说明作用。

## 3. 数据库设计

说明：

- 表名
- 主键
- taskId 唯一约束
- status 存储方式
- updatedAt 生成方式

## 4. 接口清单

列出：

- POST /api/task-runs
- GET /api/task-runs
- GET /api/task-runs/summary

## 5. 测试结果

必须给出实际运行的命令和真实结果，例如：

mvn test

说明：

- 总测试数量
- 成功数量
- 失败数量

禁止编造未运行的结果。

## 6. 启动方式

给出项目实际可用的启动命令。

## 7. 请求示例位置

说明 requests.http 的位置。

## 8. 剩余问题

如实列出：

- 尚未处理的边界情况
- 当前实现的限制
- 未完成内容

如果全部核心要求已经完成，也要明确写出本阶段没有实现哪些扩展功能。

现在先审查当前仓库，然后开始开发。