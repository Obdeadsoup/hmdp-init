# 阶段 5：签到、Bitmap 与 GEO

## 本阶段目标

把 Redis 当成数据结构工具箱，而不只是缓存。你会用 Bitmap 实现每日签到和连续签到统计，用 GEO 实现附近商铺查询。

## 文件地图

- `UserController`、`UserServiceImpl`：签到接口。
- `ShopController`、`ShopServiceImpl`：附近商铺。
- `Shop`：包含经纬度字段。
- `RedisConstants`：`USER_SIGN_KEY`、`SHOP_GEO_KEY`。
- `UserHolder`：获取当前登录用户。

## 核心概念

### 1. Bitmap

Bitmap 本质是字符串上的 bit 位操作。用它做签到非常省空间。

假设用户 101 在 2026 年 5 月签到：

```text
sign:101:202605
```

第 1 天对应 bit 0，第 2 天对应 bit 1，第 31 天对应 bit 30。

签到就是：

```text
SETBIT sign:101:202605 22 1
```

### 2. 连续签到

连续签到统计思路：

1. 取出本月从 1 号到今天的所有 bit。
2. 从今天往前数。
3. 遇到 1 就累计。
4. 遇到 0 就停止。

### 3. GEO

Redis GEO 用来保存经纬度，并按距离查询附近位置。

key 设计：

```text
shop:geo:{typeId}
```

同一类型的商铺放在一个 GEO 集合里，member 是 shopId。

## 实现步骤 A：每日签到

目标：`POST /user/sign`

在 `IUserService` 增加：

```java
Result sign();

Result signCount();
```

实现思路：

```java
Long userId = UserHolder.getUser().getId();
LocalDateTime now = LocalDateTime.now();
String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
int dayOfMonth = now.getDayOfMonth();
stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
return Result.ok();
```

Controller：

```java
@PostMapping("/sign")
public Result sign() {
    return userService.sign();
}
```

## 实现步骤 B：连续签到统计

目标：`GET /user/sign/count`

关键命令是 `BITFIELD`：

```java
List<Long> result = stringRedisTemplate.opsForValue().bitField(
        key,
        BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0)
);
```

然后从低位往前统计连续 1：

```java
Long num = result.get(0);
if (num == null || num == 0) {
    return Result.ok(0);
}
int count = 0;
while (true) {
    if ((num & 1) == 0) {
        break;
    }
    count++;
    num >>>= 1;
}
return Result.ok(count);
```

为什么从低位开始？因为 `BITFIELD` 取出来后，今天对应最低位，昨天对应次低位。

## 实现步骤 C：导入商铺 GEO 数据

建议写一个测试方法或临时工具类，把数据库商铺按类型分组，然后批量写入 Redis。

流程：

1. 查询所有商铺。
2. 按 `typeId` 分组。
3. 每组写入 `shop:geo:{typeId}`。

核心代码：

```java
List<Shop> shops = shopService.list();
Map<Long, List<Shop>> map = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));
for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
    String key = RedisConstants.SHOP_GEO_KEY + entry.getKey();
    List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();
    for (Shop shop : entry.getValue()) {
        locations.add(new RedisGeoCommands.GeoLocation<>(
                shop.getId().toString(),
                new Point(shop.getX(), shop.getY())
        ));
    }
    stringRedisTemplate.opsForGeo().add(key, locations);
}
```

## 实现步骤 D：附近商铺查询

目标：`GET /shop/of/type?typeId=1&current=1&x=120.1&y=30.2`

规则：

- 如果没有传 `x`、`y`，走原来的数据库分页。
- 如果传了经纬度，走 Redis GEO 查询。

Redis 查询：

```java
GeoResults<RedisGeoCommands.GeoLocation<String>> results =
        stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .limit(end)
        );
```

分页处理：

1. Redis GEO 只能从 0 开始取，所以先取到 `end`。
2. 手动跳过前 `from` 条。
3. 拿 shopId 列表。
4. 查数据库并保持顺序。
5. 把距离设置到 `shop.setDistance(...)`。

## 验证方式

签到：

```bash
curl -X POST http://127.0.0.1:8081/user/sign -H "authorization: token"
curl http://127.0.0.1:8081/user/sign/count -H "authorization: token"
```

Redis：

```bash
getbit sign:用户id:202605 22
bitfield sign:用户id:202605 get u23 0
geopos shop:geo:1 1
geodist shop:geo:1 1 2 km
```

附近商铺：

```bash
curl "http://127.0.0.1:8081/shop/of/type?typeId=1&current=1&x=120.149993&y=30.334229"
```

## 常见坑

1. Bitmap 偏移量从 0 开始，所以日期要减 1。
2. 连续签到统计时左右位顺序想反。
3. GEO 坐标经纬度顺序写反。
4. Redis GEO 查询出来的顺序被数据库 `IN` 查询打乱。
5. 没有经纬度时也走 GEO，导致普通列表坏掉。

## 本阶段你要掌握

- Bitmap 的位操作。
- 连续签到的位运算。
- Redis GEO 的 key 设计。
- 附近商铺的分页和距离回填。

