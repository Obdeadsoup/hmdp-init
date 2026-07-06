# 阶段 0：项目地图

## 本阶段目标

- 先把黑马点评 `init` 项目的骨架看懂。
- 明确一次请求是怎么从浏览器走到 MySQL / Redis 的。
- 给后续阶段准备统一的“地图”，避免一上来就在代码里迷路。

## 涉及文件

- `src/main/java/com/hmdp/HmDianPingApplication.java`
- `src/main/java/com/hmdp/controller/*`
- `src/main/java/com/hmdp/service/*`
- `src/main/java/com/hmdp/service/impl/*`
- `src/main/java/com/hmdp/mapper/*`
- `src/main/java/com/hmdp/entity/*`
- `src/main/java/com/hmdp/dto/*`
- `src/main/java/com/hmdp/config/*`
- `src/main/java/com/hmdp/utils/*`
- `src/main/resources/application.yaml`
- `src/main/resources/mapper/*`
- `src/main/resources/nginx-1.18.0/*`
- `hmdp.sql`

## 项目目录职责

### 1. 启动入口

- `HmDianPingApplication`
  - Spring Boot 启动类。
  - `@SpringBootApplication` 会触发自动配置、组件扫描、Spring 容器启动。
  - `@MapperScan("com.hmdp.mapper")` 会把 MyBatis Mapper 接口注册成 Bean。

### 2. controller 层

- 作用：接收 HTTP 请求，做轻量参数处理，调用 service，返回 `Result`。
- 例子：
  - `ShopTypeController`
  - `ShopController`
  - `UserController`

### 3. service 接口层

- 作用：定义业务能力。
- 例子：
  - `IShopTypeService`
  - `IShopService`
  - `IUserService`

### 4. service.impl 实现层

- 作用：写核心业务逻辑。
- 当前 `init` 分支很多类只是继承了 MyBatis-Plus 的通用实现，还没开始补完整业务。
- 例子：
  - `ShopTypeServiceImpl`
  - `ShopServiceImpl`
  - `UserServiceImpl`

### 5. mapper 层

- 作用：和数据库交互。
- 这里大多继承 `BaseMapper<T>`，直接拿到基础 CRUD。
- 例子：
  - `ShopTypeMapper`
  - `ShopMapper`
  - `UserMapper`

### 6. entity 层

- 作用：和数据库表一一对应的实体类。
- 例子：
  - `ShopType` 对应 `tb_shop_type`
  - `Shop` 对应 `tb_shop`
  - `User` 对应 `tb_user`

### 7. dto 层

- 作用：接口传输对象，不一定和数据库表结构一致。
- 例子：
  - `Result`：统一返回对象
  - `LoginFormDTO`：登录入参
  - `UserDTO`：登录态返回对象

### 8. config / utils

- `config`
  - 放 Spring / MyBatis / 异常处理等配置。
- `utils`
  - 放工具类、常量类、上下文类。
  - 后面登录和缓存阶段会重点看到：
    - `RedisConstants`
    - `RegexUtils`
    - `UserHolder`

### 9. resources

- `application.yaml`
  - Spring Boot 配置文件，放端口、数据源、Redis 等配置。
- `mapper/*.xml`
  - MyBatis XML SQL。
- `nginx-1.18.0`
  - 前端静态资源和 nginx 代理配置。

## 数据库表概览

当前 `hmdp.sql` 一共定义了 11 张核心表：

1. `tb_blog`
2. `tb_blog_comments`
3. `tb_follow`
4. `tb_seckill_voucher`
5. `tb_shop`
6. `tb_shop_type`
7. `tb_sign`
8. `tb_user`
9. `tb_user_info`
10. `tb_voucher`
11. `tb_voucher_order`

可以先把它们分成 5 组来记：

- 用户相关：`tb_user`、`tb_user_info`、`tb_sign`
- 商铺相关：`tb_shop`、`tb_shop_type`
- 社交相关：`tb_blog`、`tb_blog_comments`、`tb_follow`
- 优惠券相关：`tb_voucher`、`tb_seckill_voucher`
- 订单相关：`tb_voucher_order`

## 一条最小调用链

先看最简单接口：`GET /shop-type/list`

### 代码路径

1. 浏览器 / Postman 发请求到 `/shop-type/list`
2. `ShopTypeController#queryTypeList()` 接住请求
3. Controller 调用 `IShopTypeService`
4. 实际执行的是 `ShopTypeServiceImpl`
5. `ShopTypeServiceImpl` 继承 `ServiceImpl<ShopTypeMapper, ShopType>`
6. MyBatis-Plus 通过 `ShopTypeMapper` 查询 `tb_shop_type`
7. 查询结果封装成 `Result.ok(typeList)` 返回前端

### 这个链路说明了什么

- Controller 负责“接请求和回响应”
- Service 负责“组织业务”
- Mapper 负责“查库”
- Entity 负责“接数据库行数据”
- DTO / Result 负责“对外返回”

## 本阶段见到的关键概念

### Spring Boot 自动配置

你可以把它理解成：Spring Boot 根据 `pom.xml` 里的依赖和 `application.yaml` 里的配置，自动把很多基础设施 Bean 准备好，比如：

- Tomcat
- 数据源
- RedisTemplate / StringRedisTemplate
- Jackson JSON 序列化

### IoC / DI

- IoC：对象不再自己 new，而是交给 Spring 容器管理。
- DI：Spring 在需要的时候把依赖“注入”进去。

比如 `ShopTypeController` 里：

```java
@Resource
private IShopTypeService typeService;
```

意思就是：Controller 自己不 new `ShopTypeServiceImpl`，而是交给 Spring 统一提供。

### MyBatis-Plus 基础 CRUD

这个项目大量使用 MyBatis-Plus 的通用能力：

- `ServiceImpl<M, T>`：给 service 实现类提供通用 CRUD
- `BaseMapper<T>`：给 mapper 提供通用 CRUD
- `query()`：构造链式查询
- `orderByAsc("sort")`：按字段升序排序
- `list()`：执行查询并返回集合

## 当前阶段常见坑

1. 以为 `init` 分支已经有完整业务，其实很多接口还是 `TODO`。
2. 误把 `entity` 和 `dto` 当成同一种对象使用。
3. 一上来就盯复杂模块，忽略最简单的请求链路。
4. 只知道“能跑”，不知道请求经过了哪些层。
5. 直接从 `master` 抄代码，结果代码能跑但自己讲不清。

## 验证方式

### 后端最小验证

```bash
mvn -q -DskipTests compile
mvn spring-boot:run
```

接口验证：

```bash
curl http://127.0.0.1:8081/shop-type/list
```

### 预期现象

- 后端启动成功，监听 `8081`
- `/shop-type/list` 返回 `success=true` 的 JSON 数据

## 面试表达

可以这样讲：

“这个项目是一个典型的 Spring Boot 分层后端。入口类负责启动 Spring 容器和扫描 Mapper；Controller 对外提供 HTTP 接口；Service 层承接业务逻辑；Mapper 借助 MyBatis-Plus 访问 MySQL；Redis 在后续阶段用于登录态、缓存、点赞、签到和秒杀优化。项目的 init 分支只提供基础骨架，我是基于它逐步补完整个业务链路的，而不是直接套完整版代码。” 
