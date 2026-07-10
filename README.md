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

## 重要说明（必读）

### 1. `.env` 不会被 Spring Boot 自动加载

本项目提供了 `.env.example` 作为配置模板，也建议本地复制为 `.env` 保存真实凭据。

但 **Spring Boot 不会自动读取 `.env` 文件**。

必须由使用者手动把变量放到进程环境中，例如：

- **IDEA**：Run/Debug Configuration → Environment variables
- **Shell / PowerShell**：启动前 `export` / `Set-Item Env:...`
- **进程管理器 / 部署平台**：在运行环境中注入环境变量

应用实际读取的是：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

如果只创建了 `.env` 文件、却没有导出这些变量，服务启动后无法连接数据库。

### 2. MySQL 建表脚本不会自动执行

`application.yml` 中配置了：

```yaml
spring:
  sql:
    init:
      mode: never
```

因此：

- 项目中的 MySQL 建表脚本 **不会在启动时自动执行**
- 脚本位置：[`src/main/resources/db/mysql/schema.sql`](src/main/resources/db/mysql/schema.sql)
- 脚本结构正确，包含 `task_run` 表、主键和 `task_id` 唯一约束
- **使用者必须自己找到并手动执行该脚本**

如果未先建表，接口调用会出现类似错误：

```text
Table 'task_run_database.task_run' doesn't exist
```

## 快速开始

### 1. 创建数据库并手动建表

1. 在 MySQL 中创建数据库：`task_run_database`
2. **手动执行** 建表脚本：

```text
src/main/resources/db/mysql/schema.sql
```

可用 IDEA Database、MySQL 客户端或命令行执行。  
注意：因为 `spring.sql.init.mode=never`，这一步 **不会被应用自动完成**。

### 2. 配置环境变量（不是只写 .env 文件）

```bash
cp .env.example .env
```

编辑 `.env`，填写真实连接信息：

```env
DB_URL=jdbc:mysql://your-mysql-host:3306/task_run_database?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

`.env` 已被 git 忽略，不会提交到 GitHub。

然后 **必须导出为环境变量**，例如 PowerShell：

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $k,$v = $_.Split('=',2)
  Set-Item -Path "Env:$k" -Value $v
}
```

或在 IDEA Run Configuration 中直接配置：

```text
DB_URL=...
DB_USERNAME=...
DB_PASSWORD=...
```

### 3. 启动服务

PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

服务默认地址：`http://localhost:8080`

### 4. 打开演示前端

#### 方式 A：Spring Boot 直接提供（开发）

浏览器访问：

```text
http://localhost:8080/
```

#### 方式 B：Nginx 提供前端 + 反代 API（推荐演示）

1. 确保 Spring Boot 已在 `8080` 运行  
2. 使用配置：[`deploy/nginx/nginx.conf`](deploy/nginx/nginx.conf)  
3. 启动 Nginx 后访问：

```text
http://localhost/
```

- 前端静态页：Nginx 提供  
- `/api/*`：Nginx 反代到 `http://127.0.0.1:8080`  
- 详细说明见：[`deploy/nginx/README.md`](deploy/nginx/README.md)

页面可直接演示：

- 任务上报（RUNNING / SUCCESS / FAILED）
- 相同 taskId 更新
- 分页与筛选
- 店铺汇总统计

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
- **Spring Boot 不会自动加载 `.env`**，必须在 IDEA、Shell 或部署环境中手动导出 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`。
- **建表脚本不会自动执行**。当前配置为 `spring.sql.init.mode=never`，请手动执行 [`src/main/resources/db/mysql/schema.sql`](src/main/resources/db/mysql/schema.sql)。
- 不要把真实数据库密码提交到仓库，只保留 `.env.example`。
