# 阶段 2 练习题：登录、会话与拦截器

## 概念题

1. Session 登录为什么在单体项目里简单，但在多实例部署时不方便？
2. Redis Token 登录中，token、Redis key、UserDTO 三者分别是什么？
3. 为什么登录态中不应该直接保存完整 `User` Entity？
4. `RefreshTokenInterceptor` 和 `LoginInterceptor` 为什么要分成两个？
5. ThreadLocal 为什么必须在请求结束后清理？

## 代码阅读题

1. 阅读 `UserHolder`，说明它为什么能让 Service 拿到当前用户。
2. 阅读 `RedisConstants` 中登录相关常量，说明每个 key 的用途。

## 小改造任务

实现 `/user/me`：从 `UserHolder` 取当前用户并返回。

## 面试表达题

用 1 分钟讲清楚 Redis Token 登录的完整流程。

