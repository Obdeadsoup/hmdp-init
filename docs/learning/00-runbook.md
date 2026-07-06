# 阶段 0：启动手册

## 本阶段目标

- 明确本项目启动需要哪些环境。
- 跑通后端最小闭环。
- 知道前端 nginx 是怎么把 `/api` 代理到后端的。

## 环境结论

本次在当前工作区里实际检查到：

- Spring Boot 版本：`2.7.4`
- Maven 版本：`3.9.14`
- 当前 Java 运行时：`17.0.18`
- `hmdp.sql` 中共有 11 张核心表

说明：

- `pom.xml` 里编译目标还是 Java 8 风格，但项目在当前机器上用 JDK 17 编译通过了。
- 对学习来说暂时没问题，后面先专注业务链路，不急着做 JDK / Spring Boot 升级。

## 涉及文件

- `pom.xml`
- `src/main/resources/application.yaml`
- `hmdp.sql`
- `src/main/resources/nginx-1.18.0/conf/nginx.conf`
- `README.md`

## 推荐配置方式

为了避免把密码和地址写死在仓库里，`application.yaml` 已经改成了环境变量占位写法。

### MySQL

```text
HMDP_DB_HOST
HMDP_DB_PORT
HMDP_DB_NAME
HMDP_DB_USERNAME
HMDP_DB_PASSWORD
```

### Redis

```text
HMDP_REDIS_HOST
HMDP_REDIS_PORT
HMDP_REDIS_PASSWORD
```

如果你本机就是默认环境，也可以只设置最必要的变量，比如：

```powershell
$env:HMDP_DB_PASSWORD="123456"
$env:HMDP_REDIS_HOST="192.168.150.101"
```

如果你的 Redis 就跑在本机，`HMDP_REDIS_HOST` 可以不设，默认会走 `localhost:6379`。

## 启动步骤

### 1. 导入数据库

先在 MySQL 中创建数据库：

```sql
CREATE DATABASE hmdp;
```

然后导入：

```bash
mysql -uroot -p hmdp < hmdp.sql
```

### 2. 编译项目

```bash
mvn -q -DskipTests compile
```

本次已经验证通过，说明代码骨架没有编译错误。

### 3. 启动后端

PowerShell 示例：

```powershell
$env:HMDP_DB_PASSWORD="123456"
$env:HMDP_REDIS_HOST="192.168.150.101"
mvn spring-boot:run
```

后端默认监听：

```text
http://127.0.0.1:8081
```

### 4. 验证基础接口

优先验证最简单接口：

```bash
curl http://127.0.0.1:8081/shop-type/list
```

本次已经实际拿到返回值，说明：

- Spring Boot 启动成功
- MySQL 连通成功
- 基础查询链路可用

### 5. 启动前端 nginx

前端资源目录：

```text
src/main/resources/nginx-1.18.0
```

配置文件里约定了：

- nginx 监听 `8080`
- `/` 提供前端静态页面
- `/api/**` 反向代理到 `http://127.0.0.1:8081`

在 Windows 下可进入该目录后运行：

```powershell
.\nginx.exe
```

停止：

```powershell
.\nginx.exe -s stop
```

然后访问：

```text
http://127.0.0.1:8080
```

补充说明：这次在 Codex 工具里直接拉起 `nginx.exe` 时遇到了“拒绝访问”，所以这里没有替你完成最终的前端页面实测。但 nginx 配置已经读过，代理关系是清楚的；你在本机手动运行时，如果也遇到权限问题，优先检查 Windows 安全策略、目录执行权限、是否被系统拦截。

## 核心概念

### 为什么先验证 `/shop-type/list`

因为它是最小闭环：

- 不涉及登录
- 不涉及 Redis 复杂逻辑
- 不涉及事务
- 只需要 Controller -> Service -> Mapper -> MySQL

如果这个接口都不通，就没必要急着做后面的缓存和秒杀。

### nginx 在这里做什么

它不是后端框架的一部分，而是一个前端静态资源服务器 + 反向代理。

你可以理解成：

1. 浏览器访问 `8080`
2. nginx 把页面文件返回给浏览器
3. 页面里的 AJAX 请求打到 `/api/...`
4. nginx 再把 `/api/...` 转发给 `8081` 的 Spring Boot

## 常见坑

1. 当前目录没有 `.git`，所以现在看不到真实分支信息。
2. 不设置环境变量就启动，可能导致数据库密码或 Redis 地址不匹配。
3. MySQL 库建了，但没有导入 `hmdp.sql`。
4. Redis 服务没开，或者 Redis 不在 `localhost:6379`。
5. 后端已经占用了 `8081`，再次启动时会端口冲突。
6. Windows 下 `nginx.exe` 可能被安全策略拦截。

## 验证记录

这次已经实际验证到：

- `mvn -q -DskipTests compile` 通过
- `127.0.0.1:3306` 可连通
- 当前机器的 `127.0.0.1:6379` 不通
- 当前配置里的 `192.168.150.101:6379` 可连通
- `GET /shop-type/list` 返回正常 JSON

## 面试表达

可以这样讲：

“我接手项目时先没有急着写业务，而是先做环境和链路验证：确认 Spring Boot、Maven、JDK 版本，导入 MySQL 表结构，检查 Redis 连通性，再挑一个最简单的查询接口验证 Controller-Service-Mapper 到数据库的链路。这样后面做登录、缓存和秒杀时，每个问题我都能先判断是环境问题、配置问题还是业务问题。” 
