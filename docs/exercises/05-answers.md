# 阶段 5 参考答案：Bitmap 与 GEO

## 概念题参考答案

1. 一个月最多 31 天，每天只需要 1 个 bit 表示是否签到，空间非常小，读写也快。
2. bit 偏移量从 0 开始，所以第 1 天是 offset 0，第 2 天是 offset 1。
3. 取出本月到今天的 bit 后，从低位开始判断，连续的 1 表示连续签到天数，遇到 0 就停止。
4. member 推荐存 shopId，方便根据 Redis 返回结果再查数据库详情。
5. 附近商铺一般按类型查询，把同一类型放一个 key 可以减少无关数据扫描。

## 代码阅读题参考答案

1. `Shop` 中通常有 `x`、`y` 表示经纬度，`distance` 用于接收 Redis GEO 查询出的距离，不一定是数据库持久字段。
2. `USER_SIGN_KEY` 设计为 `sign:{userId}:yyyyMM`；`SHOP_GEO_KEY` 设计为 `shop:geo:{typeId}`。

## 小改造参考

签到：

```java
Long userId = UserHolder.getUser().getId();
LocalDateTime now = LocalDateTime.now();
String key = RedisConstants.USER_SIGN_KEY + userId + now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
stringRedisTemplate.opsForValue().setBit(key, now.getDayOfMonth() - 1, true);
return Result.ok();
```

连续签到统计核心：

```java
Long num = result.get(0);
int count = 0;
while (num != null && (num & 1) == 1) {
    count++;
    num >>>= 1;
}
return Result.ok(count);
```

## 面试表达参考

“我按用户和月份设计签到 key，例如 `sign:101:202605`。每月第几天就写对应 bit，第 1 天写 offset 0。统计连续签到时，用 `BITFIELD` 一次取出从 1 号到今天的 bit，得到一个整数，然后从低位开始判断，遇到 1 就计数，遇到 0 就停止，这样就能得到从今天往前的连续签到天数。”

