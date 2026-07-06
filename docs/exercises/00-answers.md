# 阶段 0 参考答案：环境与项目地图

## 概念题参考答案

1. `pom.xml` 描述项目坐标、父工程、依赖、插件和构建方式。Maven 会根据它下载依赖并编译打包项目。
2. `application.yaml` 放端口、数据源、Redis、日志等运行配置。真实密码不应该提交到仓库，否则会泄露本地或服务器凭证，推荐用环境变量。
3. Controller 接 HTTP 请求；Service 写业务逻辑；Mapper 访问数据库；MySQL 保存持久化数据。
4. nginx 提供前端静态页面，并把 `/api` 请求反向代理到后端 `8081`。
5. `/shop-type/list` 不依赖登录、事务、复杂 Redis，只需要最基础的查询链路，适合作为最小验证点。

## 代码阅读题参考答案

1. `@SpringBootApplication` 启动 Spring Boot 自动配置和组件扫描；`@MapperScan("com.hmdp.mapper")` 让 MyBatis 扫描 Mapper 接口并注册为 Bean。
2. 完整路径是 `GET /shop-type/list`，对应表是 `tb_shop_type`，实体类是 `ShopType`。

## 小改造参考

PowerShell 示例：

```powershell
$env:HMDP_DB_PASSWORD="123456"
$env:HMDP_REDIS_HOST="192.168.150.101"
mvn spring-boot:run
curl http://127.0.0.1:8081/shop-type/list
```

## 面试表达参考

“我会先看 README、pom 和配置文件，确认 JDK、Maven、数据库、Redis 版本和端口；然后导入 SQL，启动依赖服务；接着先编译项目，再启动后端；最后选择一个最简单的不需要登录的查询接口验证 Controller 到数据库的链路。这样可以先排除环境和配置问题。”

