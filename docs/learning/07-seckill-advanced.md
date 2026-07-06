# 阶段 7：分布式锁、Lua 与 Redis Stream 异步秒杀

## 本阶段目标

把基础秒杀升级成更接近真实高并发系统的版本：用 Redis/Lua 做资格判断，用 Stream 异步下单，用分布式锁解决多实例一人一单问题。

## 文件地图

- `VoucherOrderServiceImpl`：秒杀主流程和异步消费者。
- `IVoucherOrderService`：秒杀接口定义。
- `SeckillVoucherServiceImpl`：秒杀券库存。
- 建议新增 `RedisIdWorker`：全局唯一 ID。
- 建议新增 `SimpleRedisLock`：手写 Redis 锁用于学习。
- 建议新增 `seckill.lua`：Lua 秒杀资格判断脚本。
- `pom.xml`：阶段后期可加入 Redisson。

## 核心概念

### 1. 为什么 JVM 锁不够

`synchronized` 只能锁住当前 JVM。项目部署两台机器时，每台机器都有自己的锁，用户仍然可能重复下单。

分布式锁要把锁放到所有实例都能访问的地方，比如 Redis。

### 2. Redis 分布式锁

基础命令：

```text
SET lock:order:1 value NX EX 10
```

含义：

- `NX`：不存在才设置。
- `EX 10`：10 秒自动过期。
- `value`：线程唯一标识，用于防止误删别人的锁。

释放锁必须判断 value 是自己的，再删除。

### 3. Lua 原子性

Redis 执行 Lua 脚本时是原子的。秒杀资格判断适合放进 Lua：

1. 判断库存是否充足。
2. 判断用户是否买过。
3. 扣 Redis 库存。
4. 记录用户已下单。
5. 写入 Stream 消息。

这些操作必须一口气完成，中间不能被其他请求插入。

### 4. Redis Stream

Stream 可以当消息队列：

- `XADD`：写消息。
- `XREADGROUP`：消费组读消息。
- `XACK`：确认消息处理完成。
- Pending List：处理失败但未 ACK 的消息。

## 实现步骤 A：Redis 全局 ID

新增 `RedisIdWorker`：

思路：

- 时间戳部分：当前时间减去固定起始时间。
- 序列号部分：Redis 自增。
- 拼成 64 位 long。

关键逻辑：

```java
long timestamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
return timestamp << COUNT_BITS | count;
```

## 实现步骤 B：手写 Redis 锁

新增 `SimpleRedisLock`，先用于学习。

加锁：

```java
Boolean success = stringRedisTemplate.opsForValue()
        .setIfAbsent(KEY_PREFIX + name, ID_PREFIX + Thread.currentThread().getId(), timeoutSec, TimeUnit.SECONDS);
return Boolean.TRUE.equals(success);
```

释放锁要用 Lua：

```lua
if(redis.call('get', KEYS[1]) == ARGV[1]) then
    return redis.call('del', KEYS[1])
end
return 0
```

原因：判断和删除必须原子，否则会误删其他线程刚加的锁。

## 实现步骤 C：Redisson 改造

手写锁有很多风险：

- 不可重入。
- 没有自动续期。
- 主从切换可能丢锁。
- 实现复杂。

加入依赖：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.17.7</version>
</dependency>
```

配置：

```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();
    config.useSingleServer().setAddress("redis://127.0.0.1:6379");
    return Redisson.create(config);
}
```

使用：

```java
RLock lock = redissonClient.getLock("lock:order:" + userId);
boolean isLock = lock.tryLock();
try {
    if (!isLock) {
        return Result.fail("不允许重复下单");
    }
    return createVoucherOrder(voucherId);
} finally {
    lock.unlock();
}
```

## 实现步骤 D：Lua 秒杀脚本

新增：

```text
src/main/resources/seckill.lua
```

脚本思路：

```lua
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
redis.call('xadd', 'stream.orders', '*',
    'userId', userId,
    'voucherId', voucherId,
    'id', orderId)
return 0
```

返回值约定：

- `0`：有购买资格。
- `1`：库存不足。
- `2`：重复下单。

## 实现步骤 E：接口只做资格判断

Java 里加载脚本：

```java
private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

static {
    SECKILL_SCRIPT = new DefaultRedisScript<>();
    SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
    SECKILL_SCRIPT.setResultType(Long.class);
}
```

下单接口：

```java
long orderId = redisIdWorker.nextId("order");
Long result = stringRedisTemplate.execute(
        SECKILL_SCRIPT,
        Collections.emptyList(),
        voucherId.toString(),
        userId.toString(),
        String.valueOf(orderId)
);
if (result != 0) {
    return Result.fail(result == 1 ? "库存不足" : "不能重复下单");
}
return Result.ok(orderId);
```

此时接口不直接写数据库，而是把消息放进 Stream。

## 实现步骤 F：后台线程消费 Stream

启动后创建单线程消费者：

```java
private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

@PostConstruct
private void init() {
    SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
}
```

消费者循环：

1. `XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >`
2. 读到消息，转成 `VoucherOrder`。
3. 调用 `handleVoucherOrder`。
4. 成功后 `XACK`。
5. 异常时处理 Pending List。

## 实现步骤 G：处理 Pending List

如果消息读到了但处理失败，没有 ACK，它会进入 Pending List。

处理方式：

```text
XREADGROUP GROUP g1 c1 COUNT 1 STREAMS stream.orders 0
```

从 `0` 开始读 Pending 消息，重新处理并 ACK。

## 初始化 Redis 数据

秒杀开始前要把库存放进 Redis：

```bash
set seckill:stock:1 100
xgroup create stream.orders g1 0 mkstream
```

如果组已存在，会报错，这是正常的。生产代码要捕获或提前初始化。

## 验证方式

```bash
redis-cli
get seckill:stock:1
smembers seckill:order:1
xrange stream.orders - +
xpending stream.orders g1
```

并发压测重点看：

- Redis 库存是否小于 0。
- 数据库订单是否重复。
- Stream 是否有大量 pending。

## 常见坑

1. Lua 里 key 拼错。
2. Redis 库存没有提前初始化。
3. 没创建 Stream 消费组。
4. 订单异步后接口返回成功，但后台失败，没有处理 Pending。
5. `@Transactional` 在异步线程里调用方式不对。
6. 只用 Redis 判断一人一单，没有数据库唯一索引兜底。

## 本阶段你要掌握

- JVM 锁和分布式锁的区别。
- Redis 锁的加锁、过期、释放。
- Redisson 的价值。
- Lua 原子性。
- Redis Stream 消息队列。
- ACK 和 Pending List。
- 异步下单的最终一致性。

