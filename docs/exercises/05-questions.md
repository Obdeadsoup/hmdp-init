# 阶段 5 练习题：Bitmap 与 GEO

## 概念题

1. Bitmap 为什么适合做签到？
2. 为什么第 1 天签到要写 bit offset 0？
3. 连续签到为什么可以用位运算统计？
4. Redis GEO 的 member 推荐存什么？
5. 附近商铺查询为什么要按类型分别建 GEO key？

## 代码阅读题

1. 阅读 `Shop`，找出经纬度字段和距离字段。
2. 阅读 `RedisConstants`，说明 `USER_SIGN_KEY` 和 `SHOP_GEO_KEY` 的 key 设计。

## 小改造任务

实现每日签到接口和连续签到统计接口。

## 面试表达题

用 1 分钟讲清楚 Bitmap 连续签到的实现。

