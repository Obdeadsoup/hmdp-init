# 阶段 4：达人探店、点赞、关注与 Feed 流

## 本阶段目标

把博客、点赞、关注、共同关注、关注流串起来。这个阶段重点不是 SQL，而是学习 Redis 的 Set 和 Sorted Set 如何支撑社交业务。

## 文件地图

- `BlogController`、`BlogServiceImpl`：博客列表、详情、点赞、发布。
- `FollowController`、`FollowServiceImpl`：关注、取关、共同关注。
- `UserServiceImpl`、`UserInfoServiceImpl`：补全作者信息。
- `Blog`：博客实体。
- `Follow`：关注关系实体。
- `ScrollResult`：滚动分页返回对象。
- `RedisConstants`：`BLOG_LIKED_KEY`、`FEED_KEY`。
- `UserHolder`：获取当前登录用户。

## 核心概念

### 1. 点赞为什么用 Sorted Set

点赞需要两个能力：

- 判断用户是否点过赞。
- 查询最近点赞的前几个人。

Redis Sorted Set 同时满足：

- member 存用户 id。
- score 存点赞时间戳。
- `ZSCORE` 判断是否点赞。
- `ZRANGE` 查询前几名。

### 2. 关注为什么用 Set

关注关系需要求共同关注。Redis Set 天然支持交集：

```text
SINTER follows:1 follows:2
```

### 3. Feed 流为什么用推模式

关注流常见两种：

- 拉模式：查看时临时查所有关注人的博客，适合关注少。
- 推模式：发布博客时推送给粉丝收件箱，适合读多写少。

黑马点评采用推模式：用户发博客后，把博客 id 推到粉丝的 Sorted Set。

### 4. 滚动分页

传统分页 `page=1&pageSize=10` 在 Feed 流里容易重复或漏数据，因为新内容会插到前面。

滚动分页用：

- `maxTime`：本次查询的最大 score。
- `offset`：同一个 score 下已经跳过多少条。

## 实现步骤 A：热门博客

目标：`GET /blog/hot?current=1`

核心逻辑：

1. 按 `liked` 降序分页查博客。
2. 给每篇博客补作者信息。
3. 判断当前用户是否点赞。

示例：

```java
Page<Blog> page = query()
        .orderByDesc("liked")
        .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
List<Blog> records = page.getRecords();
records.forEach(blog -> {
    queryBlogUser(blog);
    isBlogLiked(blog);
});
return Result.ok(records);
```

`queryBlogUser` 根据 `blog.getUserId()` 查 `User`，设置 `blog.setName(...)` 和 `blog.setIcon(...)`。

## 实现步骤 B：博客详情

目标：`GET /blog/{id}`

流程：

1. 根据 id 查博客。
2. 查不到返回失败。
3. 补作者信息。
4. 判断当前用户是否点赞。
5. 返回博客。

## 实现步骤 C：点赞与取消点赞

目标：`PUT /blog/like/{id}`

流程：

1. 取当前登录用户 id。
2. 用 `ZSCORE blog:liked:{blogId} userId` 判断是否点过赞。
3. 没点过：数据库 `liked + 1`，Redis ZSet 添加用户。
4. 点过：数据库 `liked - 1`，Redis ZSet 移除用户。

关键代码思路：

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

## 实现步骤 D：查询前 5 个点赞用户

目标：`GET /blog/likes/{id}`

流程：

1. `ZRANGE blog:liked:{id} 0 4`
2. 转成用户 id 列表。
3. 查询用户。
4. 按 Redis 返回顺序排序。
5. 转成 `UserDTO` 返回。

注意：SQL `IN` 不保证顺序，需要用 `last("ORDER BY FIELD(id, ...)")` 或自己排序。

## 实现步骤 E：关注与取关

目标：`PUT /follow/{id}/{isFollow}`

数据库表 `tb_follow` 保存关注关系。

Redis Set 设计：

```text
follows:{userId}
```

关注：

1. 数据库插入 `Follow`。
2. Redis `SADD follows:{userId} followUserId`。

取关：

1. 数据库删除。
2. Redis `SREM follows:{userId} followUserId`。

## 实现步骤 F：判断是否关注

目标：`GET /follow/or/not/{id}`

先用数据库查，或者用 Redis Set：

```java
Boolean isMember = stringRedisTemplate.opsForSet()
        .isMember("follows:" + userId, followUserId.toString());
return Result.ok(BooleanUtil.isTrue(isMember));
```

## 实现步骤 G：共同关注

目标：`GET /follow/common/{id}`

流程：

1. 当前用户 key：`follows:{currentUserId}`
2. 目标用户 key：`follows:{targetUserId}`
3. Redis `SINTER` 求交集。
4. 查用户并返回 `UserDTO`。

## 实现步骤 H：发布博客并推送 Feed

目标：`POST /blog`

流程：

1. 当前用户 id 写入 `blog.userId`。
2. 保存博客到数据库。
3. 查询当前用户的粉丝：`follow_user_id = currentUserId`。
4. 遍历粉丝，把博客 id 写入他们的收件箱：

```text
feed:{fanUserId}
```

写入方式：

```java
stringRedisTemplate.opsForZSet()
        .add(RedisConstants.FEED_KEY + fanId, blog.getId().toString(), System.currentTimeMillis());
```

## 实现步骤 I：关注流滚动分页

目标：`GET /blog/of/follow?lastId=...&offset=...`

Redis 查询：

```java
Set<ZSetOperations.TypedTuple<String>> typedTuples =
        stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(
                key, 0, max, offset, 2
        );
```

然后：

1. 取出博客 id。
2. 计算本次最小时间戳 `minTime`。
3. 统计最小时间戳重复次数，作为下一次 offset。
4. 查询博客并保持顺序。
5. 返回 `ScrollResult`。

## 验证方式

点赞：

```bash
curl -X PUT http://127.0.0.1:8081/blog/like/1 -H "authorization: token"
```

Redis：

```bash
zrange blog:liked:1 0 -1 withscores
sinter follows:1 follows:2
zrange feed:1 0 -1 withscores
```

## 常见坑

1. 点赞数据库更新成功，但 Redis 写失败，数据会不一致。
2. 查询点赞用户时顺序丢失。
3. 关注时只写数据库，忘了同步 Redis Set。
4. Feed 流用普通分页导致重复数据。
5. 允许未登录用户调用点赞接口。

## 本阶段你要掌握

- Redis Set 和 Sorted Set。
- 点赞状态判断。
- 共同关注求交集。
- 推模式 Feed 流。
- 滚动分页的 `maxTime + offset`。

