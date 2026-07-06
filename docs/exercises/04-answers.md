# 阶段 4 参考答案：点赞、关注与 Feed

## 概念题参考答案

1. Sorted Set 既能判断是否点赞，又能按点赞时间排序查询前几个点赞用户；普通 Set 没有 score，不能按时间排序。
2. 每个用户关注的人可以放入一个 Set，共同关注就是两个 Set 求交集。
3. 拉模式是查看时临时查关注人的内容；推模式是发布时把内容推到粉丝收件箱。推模式适合读多写少。
4. Feed 流会不断插入新内容，传统分页容易重复或漏数据；滚动分页用时间戳和 offset 更稳定。
5. 通常先让数据库更新成功，再写 Redis，避免 Redis 显示已点赞但数据库没变。更严谨要考虑失败补偿。

## 代码阅读题参考答案

1. `minTime` 是下一页查询的最大时间戳，`offset` 是同一时间戳下已经跳过的数量，用于避免重复。
2. 常见补充字段有用户昵称、头像、是否点赞、距离等展示字段，它们不是博客表本身的核心字段。

## 小改造参考

核心代码：

```java
String key = RedisConstants.BLOG_LIKED_KEY + id;
Long userId = UserHolder.getUser().getId();
Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
if (score == null) {
    boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
    if (success) {
        stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
    }
} else {
    boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
    if (success) {
        stringRedisTemplate.opsForZSet().remove(key, userId.toString());
    }
}
return Result.ok();
```

## 面试表达参考

“每个用户关注的人用 Redis Set 保存，key 是 `follows:{userId}`，value 是被关注用户 id。查询共同关注时，用当前用户 key 和目标用户 key 做 `SINTER` 求交集，得到共同关注的用户 id 列表，再批量查用户表并转成 UserDTO 返回。”

