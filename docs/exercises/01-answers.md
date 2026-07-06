# 阶段 1 参考答案：Spring Boot 基础链路

## 概念题参考答案

1. `@RestController` 等于 `@Controller + @ResponseBody`，返回对象会被序列化成 JSON；普通 `@Controller` 默认返回页面视图。
2. `@PathVariable` 适合 `/shop/1` 这种路径变量；`@RequestParam` 适合 `/shop?typeId=1` 这种查询参数。
3. Controller 应保持轻量，只负责接参和返回；数据库逻辑放 Service/Mapper，便于复用、测试和维护。
4. `ServiceImpl` 提供 `getById`、`list`、`save`、`updateById`、`removeById`、`query`、`update` 等通用 CRUD。
5. 前端可以统一判断 `success`，失败时统一读取 `errorMsg`，成功时读取 `data`，接口风格更稳定。

## 代码阅读题参考答案

1. `Page<Shop>` 表示分页对象；`eq("type_id", typeId)` 生成 where 条件；`page(...)` 执行分页查询并返回当前页记录。
2. 成功响应通常是 `success=true`，`data` 有值；失败响应是 `success=false`，`errorMsg` 有值。

## 小改造参考

```java
@GetMapping("/{id}")
public Result queryShopById(@PathVariable("id") Long id) {
    if (id == null || id <= 0) {
        return Result.fail("店铺 id 不合法");
    }
    Shop shop = shopService.getById(id);
    if (shop == null) {
        return Result.fail("店铺不存在");
    }
    return Result.ok(shop);
}
```

## 面试表达参考

“浏览器请求进入内嵌 Tomcat 后，由 Spring MVC 根据路径找到对应 Controller 方法；Controller 进行参数绑定，调用 Service；Service 组织业务逻辑并通过 Mapper 访问 MySQL；查询结果封装成 Entity 或 DTO；最后使用统一的 Result 返回，由 Jackson 序列化成 JSON。”

