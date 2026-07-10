# Nginx 前端演示配置说明
#
# 本项目前端由 Nginx 提供静态页面，后端 API 反代到 Spring Boot。
#
# 目录约定：
# - 前端静态资源：src/main/resources/static
# - Nginx 安装目录（本机）：C:\nginx
# - Nginx 配置文件：C:\nginx\conf\nginx.conf
#
# 启动顺序：
# 1. 先启动 Spring Boot（8080）
# 2. 再启动 Nginx（80）
#
# 常用命令（管理员 PowerShell）：
#   cd C:\nginx
#   .\nginx.exe                 # 启动
#   .\nginx.exe -s reload       # 重载配置
#   .\nginx.exe -s stop         # 停止
#
# 访问地址：
#   前端页面：http://localhost/
#   API 示例：http://localhost/api/task-runs?page=1&size=10
#   后端直连：http://localhost:8080/api/task-runs?page=1&size=10
