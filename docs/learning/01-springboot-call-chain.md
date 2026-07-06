# 阶段 1：Spring Boot 后端基础骨架

## 本阶段目标

通过一个最简单接口，真正看懂 Spring Boot 后端请求从浏览器到数据库的完整链路。你要能独立解释 Controller、Service、Mapper、Entity、DTO 的职责，并能写一个小型只读接口。

## 本次不做

- 不做登录。
- 不做 Redis 缓存。
- 不做复杂业务。
- 不改数据库结构。

## 文件地图

- `src/main/java/com/hmdp/HmDianPingApplication.java`：启动类。
- `src/main/java/com/hmdp/controller/ShopTypeController.java`：商铺类型接口。
- `src/main/java/com/hmdp/controller/ShopController.java`：商铺查询接口。
- `src/main/java/com/hmdp/service/IShopTypeService.java`：商铺类型 Service 接口。
- `src/main/java/com/hmdp/service/impl/ShopTypeServiceImpl.java`：商铺类型 Service 实现。
- `src/main/java/com/hmdp/mapper/ShopTypeMapper.java`：MyBatis-Plus Mapper。
- `src/main/java/com/hmdp/entity/ShopType.java`：数据库表映射。
- `src/main/java/com/hmdp/dto/Result.java`：统一响应对象。
- `src/main/resources/application.yaml`：数据库、Redis、端口配置。

## 核心概念

### 1. `@SpringBootApplication`

这个注解是 Spring Boot 项目的启动入口。它会触发三件大事：

- 组件扫描：找到 `@Controller`、`@Service`、`@Configuration` 等类。
- 自动配置：根据依赖自动配置 Tomcat、Spring MVC、数据源等。
- 启动内嵌 Tomcat：让项目可以响应 HTTP 请求。

### 2. Controller-Service-Mapper 分层

你可以把一次查询想象成排队办事：

- Controller 是窗口，负责接收请求、返回结果。
- Service 是业务工作人员，负责判断和组织逻辑。
- Mapper 是数据库访问员，只负责查库和改库。
- Entity 是数据库记录在 Java 里的样子。
- DTO 是接口层传给前端的样子。

### 3. MyBatis-Plus 通用 CRUD

`ServiceImpl<ShopTypeMapper, ShopType>` 已经帮你实现了很多基础方法，例如：

- `getById(id)`
- `list()`
- `save(entity)`
- `updateById(entity)`
- `query().eq(...).list()`

所以 `ShopTypeServiceImpl` 虽然是空的，也能完成基础查询。

## 学习步骤

### 步骤 1：追踪 `/shop-type/list`

打开 `ShopTypeController`，找到：

```java
@GetMapping("list")
public Result queryTypeList() {
    List<ShopType> typeList = typeService
            .query().orderByAsc("sort").list();
    return Result.ok(typeList);
}
```

你要能解释：

- `@GetMapping("list")` 对应 `/shop-type/list`
- `typeService.query()` 来自 MyBatis-Plus
- `orderByAsc("sort")` 表示按 `sort` 升序
- `Result.ok(typeList)` 统一包装返回值

验证：

```bash
curl http://127.0.0.1:8081/shop-type/list
```

### 步骤 2：给店铺类型查询补一个 Service 方法

现在 Controller 里直接写了查询链式代码。为了让分层更清楚，把查询移动到 Service。

在 `IShopTypeService` 增加：

```java
Result queryTypeList();
```

在 `ShopTypeServiceImpl` 增加：

```java
@Override
public Result queryTypeList() {
    List<ShopType> typeList = query().orderByAsc("sort").list();
    return Result.ok(typeList);
}
```

然后把 `ShopTypeController` 改成：

```java
@GetMapping("list")
public Result queryTypeList() {
    return typeService.queryTypeList();
}
```

这一小步的意义：Controller 变薄，业务查询细节放到 Service。

### 步骤 3：给商铺详情接口加参数校验

打开 `ShopController#queryShopById`，现在是：

```java
return Result.ok(shopService.getById(id));
```

推荐改成：

```java
if (id == null || id <= 0) {
    return Result.fail("店铺 id 不合法");
}
Shop shop = shopService.getById(id);
if (shop == null) {
    return Result.fail("店铺不存在");
}
return Result.ok(shop);
```

这一小步训练的是：后端不能默认前端永远传对参数。

验证：

```bash
curl http://127.0.0.1:8081/shop/1
curl http://127.0.0.1:8081/shop/-1
curl http://127.0.0.1:8081/shop/999999
```

### 步骤 4：写一个只读接口练手

目标：根据用户 id 查询用户公开信息。

当前 `UserController` 已经有：

```java
@GetMapping("/info/{id}")
public Result info(@PathVariable("id") Long userId)
```

你可以给它补参数校验：

```java
if (userId == null || userId <= 0) {
    return Result.fail("用户 id 不合法");
}
```

这一步不涉及登录，只是练习路径参数和数据库查询。

## 验证方式

每改完一个小点都跑：

```bash
mvn -q -DskipTests compile
```

接口验证：

```bash
curl http://127.0.0.1:8081/shop-type/list
curl http://127.0.0.1:8081/shop/1
curl http://127.0.0.1:8081/user/info/1
```

## 常见坑

1. Controller 里写太多业务，后期会越来越乱。
2. 忘记 `@RestController`，返回值可能被当成页面名。
3. `@PathVariable` 名字和路径里的变量名不一致。
4. 误以为 Service 实现类空着就不能用，实际上 MyBatis-Plus 已经提供了基础能力。
5. 只测正常参数，不测非法参数。

## 本阶段你要掌握

- `@SpringBootApplication`
- `@RestController`
- `@RequestMapping`
- `@GetMapping`
- `@PathVariable`
- `@RequestParam`
- `@RequestBody`
- `Result` 统一响应
- MyBatis-Plus 的 `ServiceImpl` 和 `BaseMapper`

