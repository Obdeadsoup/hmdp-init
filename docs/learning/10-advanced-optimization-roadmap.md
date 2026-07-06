# 阶段 10：后续优化与进阶路线

## 本阶段目标

当你按阶段 1 到阶段 8 做完后，项目已经可以作为学习版完整项目。这个阶段告诉你：如果想把它继续打磨成更强的简历项目、Agent 开发练习项目、接近真实工程的后端项目，可以往哪些方向优化。

这些任务不是必须一次性做完。建议挑 3 到 5 个最能体现能力的方向深入。

## 路线总览

| 优化方向 | 难度 | 推荐程度 | 适合展示的能力 |
|---|---:|---:|---|
| 配置分环境 + Docker Compose | 低 | 高 | 工程化启动 |
| 接口测试集合 + 自动化测试 | 中 | 高 | 质量意识 |
| OpenAPI / Swagger 文档 | 低 | 中 | 接口规范 |
| 统一异常和错误码 | 中 | 高 | API 设计 |
| 参数校验 | 低 | 高 | 稳定性 |
| 登录安全增强 | 中 | 高 | 安全意识 |
| Redis 缓存进阶 | 中 | 高 | Redis 深度 |
| 秒杀可靠性增强 | 高 | 高 | 高并发设计 |
| 限流防刷 | 中 | 高 | 风控意识 |
| 可观测性 | 中 | 中 | 排错能力 |
| CI/CD | 中 | 中 | 工程交付 |
| 管理后台接口 | 中 | 中 | 业务完整性 |
| Agent 开发流程化 | 中 | 高 | AI 协作能力 |

## 方向 1：配置分环境

### 目标

把开发、本地测试、部署配置分开。

### 建议改造

新增：

```text
src/main/resources/application-dev.yaml
src/main/resources/application-prod.yaml
```

`application.yaml` 保留：

```yaml
spring:
  profiles:
    active: dev
```

### 学习点

- Spring Profile。
- 环境变量。
- 配置隔离。
- 敏感信息保护。

### 验收

- 本地能用 dev 启动。
- prod 不写真实密码，只读环境变量。

## 方向 2：Docker Compose 一键依赖

### 目标

让 MySQL 和 Redis 不再依赖手动安装。

### 建议文件

```text
docker-compose.yml
docker/mysql/init/hmdp.sql
```

### 学习点

- Docker 容器。
- 数据卷。
- 端口映射。
- 初始化 SQL。

### 验收

```bash
docker compose up -d
```

启动后：

- MySQL 可连。
- Redis 可连。
- 项目能启动。

## 方向 3：接口规范与参数校验

### 目标

让接口更稳定，不让错误参数进入业务层。

### 建议改造

- 引入 `spring-boot-starter-validation`。
- DTO 上加 `@NotBlank`、`@Pattern`、`@NotNull`。
- Controller 参数加 `@Valid`。
- 统一处理校验异常。

### 示例

```java
public class LoginFormDTO {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

### 学习点

- Bean Validation。
- 统一异常处理。
- 错误信息设计。

## 方向 4：统一错误码

### 目标

现在 `Result.fail("xxx")` 只有字符串。可以增加业务错误码，便于前端和排错。

### 建议设计

```java
public enum ErrorCode {
    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40100, "未登录"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    SYSTEM_ERROR(50000, "系统异常");
}
```

### 学习点

- API 设计。
- 前后端协作。
- 错误可观测性。

## 方向 5：登录安全增强

### 可做优化

1. 验证码发送限流：同一手机号 60 秒内只能发一次。
2. 验证码错误次数限制。
3. token 刷新时滑动过期。
4. 登出时删除 Redis token。
5. 敏感接口强制登录。

### Redis key 示例

```text
login:code:phone
login:code:limit:phone
login:code:error:phone
login:token:token
```

### 学习点

- 登录安全。
- Redis 计数器。
- TTL 防刷。

## 方向 6：Redis 缓存进阶

### 可做优化

1. TTL 随机化，缓解雪崩。
2. 布隆过滤器，拦截明显不存在的商铺 id。
3. 热点 key 预热。
4. 缓存重建线程池隔离。
5. 缓存命中率日志。
6. 延迟双删对比。

### 学习点

- 缓存一致性权衡。
- 热点 key。
- Redis 内存和过期策略。
- 布隆过滤器误判率。

## 方向 7：秒杀可靠性增强

### 当前学习版链路

接口线程执行 Lua，写入 Redis Stream，后台线程异步落库。

### 可做优化

1. Redis Stream 消息重试次数。
2. 失败订单补偿表。
3. 订单状态机：待创建、创建成功、创建失败。
4. 定时任务扫描异常订单。
5. 数据库唯一索引强兜底。
6. 秒杀开始前库存预热。
7. 秒杀结束后库存回收或对账。

### 学习点

- 消息可靠性。
- 最终一致性。
- 幂等。
- 补偿机制。

## 方向 8：限流和防刷

### 可做优化

1. IP 限流。
2. 用户限流。
3. 接口级限流。
4. 秒杀接口令牌桶。
5. 黑名单。

### 简单 Redis 计数器

```text
rate:user:{userId}:{api}:{minute}
```

一分钟内超过阈值就拒绝。

### 学习点

- 滑动窗口。
- 令牌桶。
- 漏桶。
- Redis 原子计数。

## 方向 9：可观测性

### 可做优化

1. 统一请求日志。
2. 慢接口日志。
3. SQL 慢查询分析。
4. Redis key 命中率日志。
5. 秒杀消息积压监控。
6. 业务指标：登录次数、下单成功数、缓存命中数。

### 学习点

- 日志结构化。
- 指标监控。
- 排错闭环。

## 方向 10：自动化测试

### 建议补测试

- `RegexUtilsTest`
- `CacheClientTest`
- `RedisIdWorkerTest`
- `UserServiceLoginTest`
- `ShopServiceCacheTest`
- `VoucherOrderServiceSeckillTest`

### 学习点

- JUnit 5。
- SpringBootTest。
- Mock。
- 测试数据准备。
- 并发测试思路。

## 方向 11：OpenAPI / Swagger

### 目标

生成可浏览的接口文档。

### 学习点

- 接口描述。
- DTO 字段含义。
- 前后端协作。

注意：先把业务跑通，再加接口文档工具，不要为了文档工具分散主线。

## 方向 12：管理后台接口

### 可做功能

1. 商铺管理。
2. 商铺类型管理。
3. 优惠券管理。
4. 秒杀活动管理。
5. 用户封禁。
6. 博客审核。

### 学习点

- RBAC 权限。
- 管理端和用户端接口隔离。
- 审核流。

## 方向 13：Agent 协作开发增强

### 可做优化

1. 为每个阶段建立任务卡。
2. 为每个功能建立验收清单。
3. 让 Agent 每次改动后生成“学习复盘”。
4. 让 Agent 生成代码审查报告。
5. 让 Agent 维护接口测试记录。
6. 让 Agent 生成面试追问题库。

### 学习点

- Prompt Engineering。
- Agent 任务拆解。
- AI 代码审查。
- AI 辅助排错。
- 人机协作边界。

## 推荐进阶顺序

如果你想把项目打磨成简历增强版，推荐顺序：

1. 配置分环境。
2. Docker Compose。
3. 接口测试集合。
4. 参数校验和统一错误码。
5. 登录安全增强。
6. Redis 缓存进阶。
7. 秒杀可靠性增强。
8. 限流防刷。
9. 面试文档和 README。
10. Agent 协作开发规范。

## 不推荐一开始做的事

- 直接升级 Spring Boot 3。
- 一上来上微服务。
- 引入 Kafka、Elasticsearch、Nacos、Sentinel 一大堆组件。
- 重写前端。
- 大规模重构目录。

原因很简单：你现在的核心目标是学透后端主链路。组件越多，不代表水平越高。能讲清一个功能为什么这样设计，才是后端能力的根。

## 最终项目表达

你可以把项目拆成三个层次讲：

### 基础能力

Spring Boot 分层、MyBatis-Plus、MySQL、接口调试、统一返回、异常处理。

### Redis 实战

登录态、缓存、点赞、关注、Feed、签到、GEO、分布式锁、Lua、Stream。

### 工程优化

配置分环境、Docker Compose、接口测试、统一错误码、限流防刷、可观测性、Agent 协作开发。

