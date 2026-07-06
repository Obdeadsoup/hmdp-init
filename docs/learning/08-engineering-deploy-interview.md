# 阶段 8：工程化、测试、部署与面试表达

## 本阶段目标

从“功能能跑”升级到“项目能维护、能部署、能排错、能讲清”。这一阶段不追求复杂架构，而是把学习成果整理成可展示的项目。

## 文件地图

- `pom.xml`：测试依赖、插件。
- `src/test/java`：单元测试和集成测试。
- `src/main/resources/application.yaml`：基础配置。
- 建议新增 `application-dev.yaml`、`application-prod.yaml`。
- 建议新增 `docker-compose.yml`。
- `README-learning.md`：学习入口。
- `docs/api/hmdp.http`：接口测试集合。
- `docs/interview/hmdp-interview.md`：面试问答。

## 核心概念

### 1. 测试分层

- 单元测试：只测一个类或一个方法，依赖尽量 mock。
- 集成测试：启动 Spring 容器，真实连数据库或 Redis。
- 接口测试：从 HTTP 层验证完整链路。

学习项目至少要有接口测试记录。核心 Service 可以逐步补测试。

### 2. 配置分环境

开发环境、测试环境、生产环境不应该共用同一份配置。

推荐：

```text
application.yaml
application-dev.yaml
application-prod.yaml
```

`application.yaml` 只保留公共配置和激活环境：

```yaml
spring:
  profiles:
    active: dev
```

敏感信息继续用环境变量。

### 3. Docker Compose

学习阶段可以用 Docker Compose 一键启动 MySQL 和 Redis，减少环境差异。

示例：

```yaml
version: "3.8"
services:
  mysql:
    image: mysql:8.0
    container_name: hmdp-mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: hmdp
    ports:
      - "3306:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7
    container_name: hmdp-redis
    ports:
      - "6379:6379"
```

## 实现步骤 A：补接口测试集合

在 `docs/api/hmdp.http` 中按阶段记录：

- 商铺类型列表
- 商铺详情
- 发送验证码
- 登录
- 当前用户
- 点赞
- 关注
- 签到
- 秒杀

每个接口记录：

1. URL
2. Method
3. Header
4. Body
5. 预期响应

## 实现步骤 B：补核心测试

建议先测这些：

1. `RegexUtils`：手机号校验。
2. `RedisIdWorker`：生成 id 是否递增、不重复。
3. `CacheClient`：缓存空值、普通缓存。
4. 秒杀 Service：库存不足、重复下单。

示例：

```java
@SpringBootTest
class HmDianPingApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

先保证 Spring 容器能启动，再逐步加业务测试。

## 实现步骤 C：整理部署步骤

部署文档至少包含：

1. JDK 版本。
2. Maven 打包命令。
3. MySQL 初始化方式。
4. Redis 初始化方式。
5. 后端启动命令。
6. nginx 前端启动方式。
7. 常见端口：`8081`、`8080`、`3306`、`6379`。

打包：

```bash
mvn clean package -DskipTests
```

启动 jar：

```bash
java -jar target/hmdp-1.0-SNAPSHOT.jar
```

## 实现步骤 D：整理 README

项目 README 建议包含：

- 项目简介
- 技术栈
- 功能模块
- 本地启动
- 核心接口
- Redis 数据结构设计
- 秒杀链路
- 常见问题
- 学习收获

## 实现步骤 E：简历表达

不要写：

```text
使用 Spring Boot + Redis 完成黑马点评项目。
```

可以写：

```text
基于 Spring Boot、MyBatis-Plus、Redis 实现点评类后端系统，完成验证码登录、Redis Token 登录态、商铺缓存、点赞关注、Feed 流、Bitmap 签到、GEO 附近商铺和优惠券秒杀。针对缓存穿透使用空值缓存，针对热点缓存击穿使用互斥锁和逻辑过期，秒杀模块使用乐观锁、分布式锁、Lua 脚本和 Redis Stream 优化库存扣减与异步下单链路。
```

## 实现步骤 F：面试准备

每个模块准备 3 层表达：

1. 业务做了什么。
2. 技术怎么实现。
3. 遇到什么问题，怎么权衡。

例子：

“商铺详情查询先查 Redis，命中直接返回；未命中再查 MySQL 并写回缓存。为了防止缓存穿透，数据库查不到时会缓存空字符串并设置短 TTL。更新商铺时先更新数据库再删除缓存，保证下一次查询能重新加载最新数据。”

## 常见坑

1. 项目能跑但没有接口测试记录，面试时讲不出证据。
2. README 只写技术栈，不写自己解决的问题。
3. 配置文件提交真实密码。
4. Docker 启动了 MySQL，但忘记导入 `hmdp.sql`。
5. 秒杀压测只看接口成功率，不看数据库订单和 Redis 状态。

## 本阶段你要掌握

- 基础测试意识。
- 配置分环境。
- Docker Compose 本地依赖。
- README 和接口文档。
- 项目亮点表达。
- 面试追问应对。

