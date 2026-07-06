# 阶段 3：商铺查询与 Redis 缓存

## 本阶段目标

给商铺详情和商铺类型加 Redis 缓存，并一步步解决缓存穿透、缓存击穿、缓存一致性问题。学完后你要能讲清 Cache Aside 模式，以及为什么缓存不是简单的 `get/set`。

## 文件地图

- `ShopController`：商铺详情、更新、按类型查询。
- `ShopTypeController`：商铺类型列表。
- `IShopService`、`ShopServiceImpl`：商铺核心查询与更新逻辑。
- `IShopTypeService`、`ShopTypeServiceImpl`：商铺类型缓存。
- `RedisConstants`：缓存 key 和 TTL 常量。
- 建议新增 `CacheClient`：封装缓存工具方法。
- `RedisData`：逻辑过期包装对象，项目中已提供。

## 核心概念

### 1. Cache Aside 模式

最常见的缓存模式：

1. 查询时先查缓存。
2. 缓存命中，直接返回。
3. 缓存未命中，查数据库。
4. 数据库查到后写入缓存。
5. 更新时先更新数据库，再删除缓存。

注意：更新后通常是“删缓存”，不是“改缓存”。因为删除缓存能让下一次查询重新从数据库加载最新值。

### 2. 缓存穿透

请求的数据在缓存和数据库都不存在，比如一直查 `shop:999999`。如果每次都打数据库，就会形成穿透。

解决方式：缓存空值。数据库查不到时，缓存一个空字符串，设置较短 TTL。

### 3. 缓存击穿

某个热点 key 过期瞬间，大量请求同时查数据库。

解决方式：

- 互斥锁：只让一个线程重建缓存。
- 逻辑过期：热点数据不过期，后台异步重建。

### 4. 缓存雪崩

大量 key 同时过期，数据库压力暴涨。

解决方式：TTL 加随机值，或热点 key 分散过期时间。

## 实现步骤 A：普通缓存

### 步骤 1：Service 接口定义

在 `IShopService`：

```java
Result queryById(Long id);

Result updateShop(Shop shop);
```

### 步骤 2：Controller 调用 Service

`ShopController#queryShopById`：

```java
return shopService.queryById(id);
```

`ShopController#updateShop`：

```java
return shopService.updateShop(shop);
```

### 步骤 3：实现查询缓存

在 `ShopServiceImpl` 注入：

```java
@Resource
private StringRedisTemplate stringRedisTemplate;
```

实现：

```java
@Override
public Result queryById(Long id) {
    String key = RedisConstants.CACHE_SHOP_KEY + id;
    String shopJson = stringRedisTemplate.opsForValue().get(key);
    if (StrUtil.isNotBlank(shopJson)) {
        Shop shop = JSONUtil.toBean(shopJson, Shop.class);
        return Result.ok(shop);
    }
    Shop shop = getById(id);
    if (shop == null) {
        return Result.fail("店铺不存在");
    }
    stringRedisTemplate.opsForValue().set(
            key,
            JSONUtil.toJsonStr(shop),
            RedisConstants.CACHE_SHOP_TTL,
            TimeUnit.MINUTES
    );
    return Result.ok(shop);
}
```

## 实现步骤 B：缓存空值解决穿透

普通缓存的问题：如果数据库不存在，攻击者反复查同一个不存在的 id，还是会打数据库。

改造逻辑：

```java
if (shopJson != null) {
    return Result.fail("店铺不存在");
}
```

数据库查不到时：

```java
stringRedisTemplate.opsForValue().set(
        key,
        "",
        RedisConstants.CACHE_NULL_TTL,
        TimeUnit.MINUTES
);
return Result.fail("店铺不存在");
```

完整判断顺序：

1. `StrUtil.isNotBlank(shopJson)`：命中真实数据。
2. `shopJson != null`：命中空值。
3. `shopJson == null`：缓存未命中，查数据库。

## 实现步骤 C：更新数据库后删除缓存

```java
@Override
@Transactional
public Result updateShop(Shop shop) {
    Long id = shop.getId();
    if (id == null) {
        return Result.fail("店铺 id 不能为空");
    }
    updateById(shop);
    stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
    return Result.ok();
}
```

为什么先更新数据库再删缓存？

因为数据库是主数据源。缓存只是副本。先确保数据库成功，再让缓存失效。

## 实现步骤 D：互斥锁解决击穿

思路：

1. 缓存没命中。
2. 尝试获取 Redis 锁。
3. 获取成功，查数据库并重建缓存。
4. 获取失败，睡一会儿重试。
5. 最后释放自己的锁。

加锁：

```java
private boolean tryLock(String key) {
    Boolean flag = stringRedisTemplate.opsForValue()
            .setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
    return BooleanUtil.isTrue(flag);
}
```

解锁：

```java
private void unlock(String key) {
    stringRedisTemplate.delete(key);
}
```

查询方法用递归或循环重试。初学阶段建议先用简单递归，理解后再优化。

## 实现步骤 E：抽取 `CacheClient`

当普通缓存、缓存空值、互斥锁越来越多时，可以新增：

```text
src/main/java/com/hmdp/utils/CacheClient.java
```

它至少提供：

```java
public void set(String key, Object value, Long time, TimeUnit unit)
public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit)
public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit)
public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit)
```

抽取的意义不是“显得高级”，而是避免每个缓存业务都重复写空值、TTL、JSON 转换、锁逻辑。

## 实现步骤 F：逻辑过期

逻辑过期适合热点数据。

`RedisData` 结构：

```java
private LocalDateTime expireTime;
private Object data;
```

流程：

1. Redis 中的 key 不设置物理过期。
2. value 里带一个业务过期时间。
3. 查询时发现没过期，直接返回。
4. 查询时发现已过期，先返回旧数据。
5. 同时尝试获取锁，让后台线程重建缓存。

优点：热点 key 不会突然失效，用户请求不用等待数据库。

缺点：短时间可能读到旧数据，所以更适合允许短暂不一致的热点场景。

## 实现步骤 G：商铺类型缓存

`/shop-type/list` 很适合缓存，因为它变化少、查询频繁。

推荐 key：

```text
cache:shop:type:list
```

流程和普通缓存一样：

1. 查 Redis。
2. 命中返回。
3. 未命中查数据库。
4. 写入 Redis。

## 验证方式

```bash
curl http://127.0.0.1:8081/shop/1
curl http://127.0.0.1:8081/shop/999999
```

Redis 检查：

```bash
keys cache:shop:*
get cache:shop:1
ttl cache:shop:1
get cache:shop:999999
```

更新后检查缓存是否被删：

```bash
curl -X PUT http://127.0.0.1:8081/shop ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":1,\"name\":\"测试店铺\"}"
```

## 常见坑

1. 空字符串用 `StrUtil.isNotBlank` 判断会走未命中，所以还要判断 `shopJson != null`。
2. 更新数据库后忘记删除缓存。
3. 锁没有 TTL，服务宕机后死锁。
4. 逻辑过期误用到所有数据，导致旧数据长期存在。
5. JSON 反序列化时类型不匹配。

## 本阶段你要掌握

- Cache Aside。
- 缓存穿透、击穿、雪崩。
- 空值缓存。
- 互斥锁。
- 逻辑过期。
- 缓存一致性的基本策略。

