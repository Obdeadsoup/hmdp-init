# 阶段 3 参考答案：Redis 缓存

## 概念题参考答案

1. 查询先查缓存，命中直接返回；未命中查数据库，查到后写入缓存。更新时先更新数据库，再删除缓存。
2. 穿透是查不存在的数据导致请求绕过缓存打数据库；击穿是热点 key 过期瞬间大量请求打数据库；雪崩是大量 key 同时失效导致数据库压力暴涨。
3. 缓存空值能让不存在的数据也命中缓存，避免反复查数据库。TTL 不宜太长，因为后续数据可能被创建，空值缓存会造成短暂查不到。
4. 数据库是主数据源，先保证数据库成功，再删除缓存，让下一次查询重新加载新值。
5. 逻辑过期适合热点、允许短暂旧数据的场景；不适合强一致、低频、必须实时的数据。

## 代码阅读题参考答案

1. `RedisData` 包含真实数据和逻辑过期时间，Redis key 本身可以不过期，查询时由业务判断是否过期。
2. `CACHE_SHOP_KEY` 是店铺缓存 key 前缀；`LOCK_SHOP_KEY` 是重建店铺缓存时互斥锁 key 前缀。

## 小改造参考

关键判断：

```java
String shopJson = stringRedisTemplate.opsForValue().get(key);
if (StrUtil.isNotBlank(shopJson)) {
    return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
}
if (shopJson != null) {
    return Result.fail("店铺不存在");
}
Shop shop = getById(id);
if (shop == null) {
    stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
    return Result.fail("店铺不存在");
}
stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
return Result.ok(shop);
```

## 面试表达参考

“商铺详情先查 Redis，如果命中真实 JSON 就返回；如果命中空字符串，说明数据库也不存在，直接返回不存在；只有 Redis 完全没有这个 key 时才查 MySQL。MySQL 查不到会把空字符串写入 Redis，并设置较短 TTL，比如 2 分钟。这样恶意请求不存在的 id 时也会命中 Redis，不会每次打到数据库。”

