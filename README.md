# 采集任务监控服务

基于 Spring Boot 的任务运行监控服务，支持任务上报（upsert）、分页查询和店铺汇总。

## 技术栈

- Java 17
- Spring Boot 4.1.0
- MyBatis-Plus 3.5.16
- MySQL
- H2（测试）
- Maven Wrapper

## 功能

1. **任务上报** `POST /api/task-runs`  
   按 `taskId` 新增或更新，相同任务不会重复落库。
2. **分页查询** `GET /api/task-runs`  
   支持 `shopId`、`status` 筛选，按更新时间倒序。
3. **店铺汇总** `GET /api/task-runs/summary`  
   统计 RUNNING / SUCCESS / FAILED 数量、成功采集行数、最近成功完成时间。

## 快速开始

### 1. 准备数据库

先创建数据库 `task_run_database`，再执行建表脚本：

```sql
-- src/main/resources/db/mysql/schema.sql
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，填写真实连接信息：

```env
DB_URL=jdbc:mysql://your-mysql-host:3306/task_run_database?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

> `.env` 已被 git 忽略，不会提交到 GitHub。  
> Spring Boot 不会自动读取 `.env`，启动前需要导出为系统环境变量，或在 IDEA Run Configuration 中配置。

### 3. 启动服务

PowerShell：

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $k,$v = $_.Split('=',2)
  Set-Item -Path "Env:$k" -Value $v
}
.\mvnw.cmd spring-boot:run
```

服务默认地址：`http://localhost:8080`

## 接口示例

更完整的请求见根目录 [`requests.http`](requests.http)。

### 上报 RUNNING

```http
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
```

### 更新为 SUCCESS

```http
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
```

### 分页查询

```http
GET http://localhost:8080/api/task-runs?page=1&size=10
GET http://localhost:8080/api/task-runs?page=1&size=10&shopId=SHOP-001&status=SUCCESS
```

### 店铺汇总

```http
GET http://localhost:8080/api/task-runs/summary?shopId=SHOP-001
```

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

常见状态码：

- `200` 成功
- `400` 参数/业务校验失败
- `500` 服务端异常

## 测试

```bash
./mvnw test
```

Windows：

```bat
.\mvnw.cmd test
```

测试使用 H2 内存库，不会访问真实 MySQL。

## 项目结构

```text
src/main/java/com/qianyu/
  taskrun/
    controller/     # REST 接口
    service/        # 业务逻辑与 upsert
    repository/     # MyBatis-Plus Mapper
    entity/         # 数据库实体
    dto/            # 请求/响应 DTO
    enums/          # TaskStatus
  common/           # 统一响应与异常处理
  config/           # MyBatis-Plus 配置
src/main/resources/
  db/mysql/schema.sql
  mapper/TaskRunRepository.xml
  application.yml
```

## 说明

- `taskId` 有数据库唯一约束，相同任务只会保留一条记录。
- `updatedAt` 由服务端生成，不接受客户端传入。
- 默认不会自动建表，首次部署需手动执行 `schema.sql`。
- 不要把真实数据库密码提交到仓库，只保留 `.env.example`。
