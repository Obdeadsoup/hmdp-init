# 黑马点评项目面试问答

## 1. 项目整体

### Q：这个项目是做什么的？

A：这是一个点评类后端项目，包含用户登录、商铺查询、探店博客、点赞关注、Feed 流、签到、附近商铺和优惠券秒杀等功能。我主要通过它学习 Spring Boot 分层架构、MyBatis-Plus、Redis 缓存和高并发秒杀设计。

### Q：项目用了哪些技术？

A：后端使用 Spring Boot、Spring MVC、MyBatis-Plus、MySQL、Redis；前端是静态页面通过 nginx 代理到后端；Redis 用到了 String、Hash、Set、Sorted Set、Bitmap、GEO、Stream 等结构。

### Q：你怎么理解项目分层？

A：Controller 接收 HTTP 请求并返回统一结果；Service 编排业务逻辑；Mapper 访问数据库；Entity 对应数据库表；DTO 用于接口传输，避免把数据库实体直接暴露给前端。

## 2. 登录模块

### Q：为什么要从 Session 登录升级到 Redis Token 登录？

A：Session 简单，但默认保存在单台服务器内存里，多实例部署时不共享。Redis Token 把登录态保存到 Redis，多个服务实例都能读取，适合分布式部署。

### Q：Redis 登录态怎么设计？

A：验证码用 String，key 是 `login:code:{phone}`；登录态用 Hash，key 是 `login:token:{token}`，Hash 中保存 UserDTO 的 id、昵称、头像等字段，并设置 TTL。

### Q：为什么用 ThreadLocal？

A：拦截器解析 token 后，把当前用户保存到 ThreadLocal，同一个请求线程中的 Controller 和 Service 都能方便获取当前用户。请求结束后必须 remove，避免线程复用导致用户串号。

## 3. 缓存模块

### Q：商铺详情缓存流程是什么？

A：先查 Redis，命中直接返回；未命中查 MySQL，查到后写入 Redis 并设置 TTL；如果数据库也查不到，就缓存空字符串并设置短 TTL，防止缓存穿透。

### Q：缓存穿透怎么解决？

A：对不存在的数据缓存空值。例如查不到店铺时把 `cache:shop:{id}` 设置为空字符串，TTL 较短。后续请求会命中空值，不再打数据库。

### Q：缓存击穿怎么解决？

A：可以用互斥锁或逻辑过期。互斥锁保证只有一个线程重建缓存；逻辑过期则先返回旧数据，同时后台线程异步重建，适合热点数据。

### Q：更新商铺时如何保证缓存一致性？

A：采用 Cache Aside，先更新数据库，再删除缓存。缓存删除后，下次查询会重新从数据库加载最新数据。

## 4. 点赞关注和 Feed

### Q：点赞为什么用 Sorted Set？

A：Sorted Set 的 member 存用户 id，score 存点赞时间戳，既能判断是否点赞，又能按时间查询前几个点赞用户。

### Q：共同关注怎么实现？

A：每个用户关注的人保存到 Redis Set，key 是 `follows:{userId}`。共同关注就是当前用户和目标用户两个 Set 做交集，然后根据交集用户 id 查询用户信息。

### Q：Feed 流怎么实现？

A：发布博客时查询粉丝列表，把博客 id 写入每个粉丝的 Feed 收件箱，key 是 `feed:{fanId}`，用 Sorted Set 保存，score 是发布时间。查询时用 `maxTime + offset` 做滚动分页。

## 5. 签到和 GEO

### Q：Bitmap 签到怎么设计？

A：key 设计为 `sign:{userId}:yyyyMM`，每个月一个 Bitmap。第几天签到就把第几天对应的 bit 置为 1。统计连续签到时用 BITFIELD 取出本月到今天的 bit，再从低位开始连续统计 1。

### Q：附近商铺怎么实现？

A：按商铺类型把经纬度写入 Redis GEO，key 是 `shop:geo:{typeId}`，member 是 shopId。查询时根据用户坐标和半径搜索，拿到 shopId 和距离，再查数据库详情并按 Redis 返回顺序排序。

## 6. 秒杀模块

### Q：什么是超卖？你怎么解决？

A：超卖是卖出的数量超过库存。我的做法是在扣库存 SQL 中加入 `stock > 0` 条件，只有库存仍然大于 0 时才扣减，利用数据库更新的原子性防止库存扣成负数。

### Q：一人一单怎么解决？

A：基础版在事务中先查订单，再扣库存和创建订单，并用用户 id 加锁防止同一用户并发。更稳的是在数据库加唯一索引 `(user_id, voucher_id)` 兜底。多实例场景需要分布式锁或 Lua 资格判断。

### Q：为什么事务可能不生效？

A：常见原因包括方法不是 public、写在 private 方法上、同一个类内部自调用绕过 Spring 代理、异常被捕获吞掉、类不是 Spring Bean。

### Q：Lua 秒杀脚本做了什么？

A：Lua 在 Redis 中原子执行库存判断、一人一单判断、扣减 Redis 库存、记录用户已购买、写入 Stream 消息。这样接口线程只负责资格判断，数据库下单交给后台线程异步处理。

### Q：Redis Stream 的 Pending List 有什么用？

A：消费者读到消息但处理失败或没 ACK 时，消息会进入 Pending List。后台可以从 Pending List 重新读取处理，避免消息丢失。

## 7. 项目亮点表达

### 30 秒版本

这是一个点评类后端项目，我基于 Spring Boot、MyBatis-Plus、MySQL 和 Redis 实现了登录、商铺缓存、点赞关注、Feed、签到、GEO 附近商铺和秒杀。项目重点是 Redis 在不同业务场景下的数据结构选择，以及高并发秒杀中的超卖、一人一单、Lua 原子判断和异步下单。

### 1 分钟版本

这个项目从普通 CRUD 开始，逐步加入 Redis 和高并发设计。登录模块使用 Redis Hash 保存 Token 登录态，拦截器刷新 TTL 并通过 ThreadLocal 保存当前用户。商铺缓存采用 Cache Aside，并通过空值缓存、互斥锁和逻辑过期处理穿透和击穿。社交模块用 Sorted Set 做点赞排行和 Feed 流，用 Set 做关注和共同关注。签到用 Bitmap，附近商铺用 GEO。秒杀模块先用数据库乐观锁解决超卖，再用 Lua 和 Redis Stream 把资格判断和下单异步化，提高并发能力。

