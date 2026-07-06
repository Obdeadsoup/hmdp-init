# 阶段 1 练习题：Spring Boot 基础链路

## 概念题

1. `@RestController` 和普通 `@Controller` 有什么区别？
2. `@PathVariable` 和 `@RequestParam` 分别适合什么场景？
3. 为什么不建议在 Controller 中直接写数据库访问逻辑？
4. MyBatis-Plus 的 `ServiceImpl<M,T>` 提供了哪些能力？
5. `Result` 统一返回对象有什么好处？

## 代码阅读题

1. 阅读 `ShopController#queryShopByType`，说明 `Page<Shop>`、`eq("type_id", typeId)`、`page(...)` 分别做什么。
2. 阅读 `Result`，说明成功响应和失败响应分别包含哪些字段。

## 小改造任务

给 `ShopController#queryShopById` 增加参数校验和“店铺不存在”的失败返回。

## 面试表达题

用 1 分钟讲清楚一个 Spring Boot 查询接口从收到请求到返回 JSON 的过程。

