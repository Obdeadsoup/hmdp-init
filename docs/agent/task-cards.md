# Agent 任务卡模板

你可以把每个功能拆成一张任务卡。任务卡越清楚，Agent 越不容易跑偏。

## 任务卡模板

```md
# 任务：标题

## 当前阶段

阶段 X：模块名称

## 目标

一句话说明本次要实现什么。

## 本次不做

- ...
- ...

## 相关文件

- ...
- ...

## 实现步骤

1. ...
2. ...
3. ...

## 验证方式

- 编译：
- 接口：
- MySQL：
- Redis：

## 完成标准

- [ ] ...
- [ ] ...

## 需要 Agent 特别注意

- 不要改无关文件。
- 不要引入新依赖。
- 不要跳过验证。
```

## 示例：发送验证码

```md
# 任务：实现 Redis 验证码发送

## 当前阶段

阶段 2：登录、会话与拦截器

## 目标

实现 `/user/code`，完成手机号校验、生成验证码、写入 Redis、打印日志。

## 本次不做

- 不实现登录。
- 不实现拦截器。
- 不改前端。

## 相关文件

- `UserController`
- `IUserService`
- `UserServiceImpl`
- `RegexUtils`
- `RedisConstants`

## 实现步骤

1. 在 `IUserService` 声明 `sendCode`。
2. 在 `UserServiceImpl` 注入 `StringRedisTemplate`。
3. 校验手机号。
4. 生成 6 位验证码。
5. 写入 Redis，TTL 2 分钟。
6. Controller 调用 Service。

## 验证方式

- 编译：`mvn -q -DskipTests compile`
- 接口：`POST /user/code?phone=13800138000`
- Redis：`get login:code:13800138000`

## 完成标准

- [ ] 手机号错误返回失败。
- [ ] 手机号正确返回成功。
- [ ] Redis 能看到验证码。
- [ ] TTL 正确。

## 需要 Agent 特别注意

- 不要真实发送短信。
- 验证码可以打印到日志。
- 不要实现登录。
```

## 示例：商铺详情缓存

```md
# 任务：实现商铺详情普通缓存和空值缓存

## 当前阶段

阶段 3：商铺查询与 Redis 缓存

## 目标

让 `/shop/{id}` 先查 Redis，未命中查 MySQL，查不到时缓存空值。

## 本次不做

- 不做互斥锁。
- 不做逻辑过期。
- 不抽取 CacheClient。

## 相关文件

- `ShopController`
- `IShopService`
- `ShopServiceImpl`
- `RedisConstants`

## 实现步骤

1. `IShopService` 增加 `queryById(Long id)`。
2. Controller 改为调用 Service。
3. Service 查 Redis。
4. 命中 JSON 返回。
5. 命中空值返回不存在。
6. 未命中查 MySQL。
7. MySQL 查不到写空值。
8. MySQL 查到写 JSON。

## 验证方式

- 编译：`mvn -q -DskipTests compile`
- 接口：`GET /shop/1`
- Redis：`get cache:shop:1`
- 穿透验证：`GET /shop/999999`，再看 `get cache:shop:999999`

## 完成标准

- [ ] 第一次请求会写缓存。
- [ ] 第二次请求命中缓存。
- [ ] 不存在 id 写空值。
- [ ] 空值 TTL 较短。
```

