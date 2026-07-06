# 阶段 8 参考答案：工程化与面试表达

## 概念题参考答案

1. 单元测试只测小范围代码；集成测试启动 Spring 容器并连接部分真实依赖；接口测试从 HTTP 层验证完整链路。
2. 开发、测试、生产的数据库、Redis、日志级别、端口可能不同，分环境能避免互相影响。
3. Docker Compose 可以一键启动 MySQL、Redis 等依赖，减少本机环境差异。
4. README 应包含项目简介、技术栈、启动步骤、数据库初始化、核心接口、Redis 设计、常见问题和项目亮点。
5. 面试官关心你解决了什么问题、为什么这样设计、遇到什么坑，只写技术栈无法体现能力。

## 代码阅读题参考答案

1. `spring-boot-starter-test` 是测试依赖；`spring-boot-maven-plugin` 用于 Spring Boot 打包运行。
2. 数据库 url、username、password，Redis host、port、password 都适合用环境变量。

## 小改造参考

接口测试记录至少写清：

- 请求方法和 URL。
- Header，尤其是 `authorization`。
- JSON 请求体。
- 成功响应。
- 失败响应。
- 对应数据库或 Redis 检查命令。

## 面试表达参考

“这是一个点评类后端项目，基于 Spring Boot、MyBatis-Plus、MySQL 和 Redis 实现。业务包括验证码登录、商铺查询、探店博客、点赞关注、Feed 流、签到、附近商铺和优惠券秒杀。我重点学习了 Redis 在不同场景下的数据结构选择：Hash 保存登录态，String 做缓存，Set 做关注关系，Sorted Set 做点赞和 Feed，Bitmap 做签到，GEO 做附近商铺。缓存模块处理了穿透、击穿和一致性问题；秒杀模块从数据库乐观锁版本演进到 Lua 原子判断和 Redis Stream 异步下单，解决高并发下的超卖和重复下单问题。”

