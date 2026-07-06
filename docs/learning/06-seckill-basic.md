# 阶段 6：优惠券秒杀基础版

## 本阶段目标

实现优惠券秒杀的基础版本，并理解高并发下为什么会出现超卖、一人多单、事务失效。这个阶段先用数据库事务和乐观锁解决核心问题，不急着上 Lua 和异步。

## 文件地图

- `VoucherController`、`VoucherServiceImpl`：新增秒杀券。
- `VoucherOrderController`、`VoucherOrderServiceImpl`：秒杀下单入口。
- `Voucher`：普通优惠券。
- `SeckillVoucher`：秒杀券库存、开始时间、结束时间。
- `VoucherOrder`：订单。
- `SeckillVoucherMapper.xml`：扣库存 SQL 可在这里定制。
- `VoucherMapper.xml`：券查询 SQL。
- `UserHolder`：当前登录用户。

## 核心概念

### 1. 普通券和秒杀券

`tb_voucher` 保存券的基础信息，例如标题、副标题、支付金额。

`tb_seckill_voucher` 保存秒杀特有信息：

- voucher_id
- stock
- begin_time
- end_time

这种设计叫“主表 + 扩展表”。

### 2. 超卖

如果多个线程同时读到库存为 1，然后都创建订单，就会卖出多份。

解决：扣库存时加条件：

```sql
where voucher_id = ? and stock > 0
```

这就是乐观锁思想：更新时再确认条件仍然成立。

### 3. 一人一单

同一个用户不能重复买同一张秒杀券。

基础做法：

1. 先查订单表是否存在。
2. 不存在才创建。
3. 最好给数据库加唯一索引兜底。

推荐唯一索引：

```sql
alter table tb_voucher_order add unique key uk_user_voucher(user_id, voucher_id);
```

### 4. 事务

下单至少包含两件事：

1. 扣库存。
2. 创建订单。

这两步必须同成功或同失败，所以需要 `@Transactional`。

## 实现步骤 A：朴素下单

目标：`POST /voucher-order/seckill/{id}`

在 `IVoucherOrderService`：

```java
Result seckillVoucher(Long voucherId);
```

`VoucherOrderController`：

```java
@PostMapping("seckill/{id}")
public Result seckillVoucher(@PathVariable("id") Long voucherId) {
    return voucherOrderService.seckillVoucher(voucherId);
}
```

`VoucherOrderServiceImpl` 基础流程：

```java
SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
    return Result.fail("秒杀尚未开始");
}
if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
    return Result.fail("秒杀已经结束");
}
if (voucher.getStock() < 1) {
    return Result.fail("库存不足");
}
```

## 实现步骤 B：乐观锁扣库存

朴素写法容易超卖。推荐：

```java
boolean success = seckillVoucherService.update()
        .setSql("stock = stock - 1")
        .eq("voucher_id", voucherId)
        .gt("stock", 0)
        .update();
if (!success) {
    return Result.fail("库存不足");
}
```

注意：`tb_seckill_voucher` 的主键字段可能不是 `voucher_id`，所以用 `eq("voucher_id", voucherId)` 更稳。

## 实现步骤 C：创建订单

```java
Long userId = UserHolder.getUser().getId();
VoucherOrder voucherOrder = new VoucherOrder();
long orderId = redisIdWorker.nextId("order");
voucherOrder.setId(orderId);
voucherOrder.setUserId(userId);
voucherOrder.setVoucherId(voucherId);
save(voucherOrder);
return Result.ok(orderId);
```

如果你还没实现 `RedisIdWorker`，可以先用数据库自增或临时雪花算法工具。正式进入秒杀阶段建议实现 Redis 全局 ID。

## 实现步骤 D：一人一单

加判断：

```java
int count = query()
        .eq("user_id", userId)
        .eq("voucher_id", voucherId)
        .count();
if (count > 0) {
    return Result.fail("用户已经购买过一次");
}
```

这一段要放在事务里。

## 实现步骤 E：事务和自调用问题

推荐把真正创建订单抽成方法：

```java
@Transactional
public Result createVoucherOrder(Long voucherId) {
    // 一人一单
    // 扣库存
    // 创建订单
}
```

但是要注意：同一个类里直接 `this.createVoucherOrder(voucherId)`，事务不会生效，因为绕过了 Spring 代理。

解决方式之一：

1. 引入 `AopContext.currentProxy()`。
2. 开启暴露代理。

或者更适合初学阶段：先把 `@Transactional` 标在 `seckillVoucher` 方法上，保证流程跑通，再专题理解代理。

## 实现步骤 F：用锁补一人一单并发问题

同一个用户并发请求时，两个线程可能都查到没有订单。

基础版可以用用户 id 加锁：

```java
synchronized (userId.toString().intern()) {
    return createVoucherOrder(voucherId);
}
```

这个锁只在单 JVM 内有效。多实例部署时要进入阶段 7 的分布式锁。

## 验证方式

接口：

```bash
curl -X POST http://127.0.0.1:8081/voucher-order/seckill/1 -H "authorization: token"
```

数据库：

```sql
select * from tb_seckill_voucher where voucher_id = 1;
select * from tb_voucher_order where voucher_id = 1;
```

并发测试可以用 JMeter：

- 线程数：100
- 循环次数：1
- 请求：`POST /voucher-order/seckill/{id}`
- Header：`authorization: token`

## 常见坑

1. 先查库存再扣库存，没有在更新条件里判断 `stock > 0`。
2. `@Transactional` 写在 private 方法上，不生效。
3. 同类自调用导致事务不生效。
4. 一人一单只靠先查，缺少数据库唯一索引兜底。
5. 用 JVM 锁后以为多实例也安全。

## 本阶段你要掌握

- 秒杀券表和普通券表关系。
- 超卖。
- 乐观锁。
- 一人一单。
- `@Transactional` 生效条件。
- JVM 锁的边界。

