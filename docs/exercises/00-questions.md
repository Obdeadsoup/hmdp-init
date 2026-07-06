# 阶段 0 练习题：环境与项目地图

## 概念题

1. `pom.xml` 在 Maven 项目里负责什么？
2. `application.yaml` 里通常放哪些配置？为什么不建议写真实密码？
3. `Controller -> Service -> Mapper -> MySQL` 这条链路每一层分别负责什么？
4. nginx 在本项目里承担什么角色？
5. 为什么推荐先验证 `/shop-type/list`，而不是一上来验证登录或秒杀？

## 代码阅读题

1. 阅读 `HmDianPingApplication`，说明 `@SpringBootApplication` 和 `@MapperScan` 的作用。
2. 阅读 `ShopTypeController#queryTypeList`，写出它的完整请求路径和数据库表名。

## 小改造任务

把你的本机启动命令记录到一个本地笔记中，包含 MySQL 密码、Redis 地址、启动后端、启动 nginx、验证接口。

## 面试表达题

用 1 分钟说明你接手一个 Spring Boot 项目时，如何先判断它能不能在本机跑起来。

