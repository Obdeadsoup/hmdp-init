# 黑马点评后端学习文档使用说明

这套文档把原本需要一轮轮对话推进的学习过程，整理成可以在工作区里直接阅读和执行的课程包。你可以把它当成一本“带项目代码的后端入门实战书”：先看概念，再按步骤改代码，然后用接口、数据库和 Redis 验证结果。

## 推荐学习方式

1. 先读 `docs/learning/00-project-map.md` 和 `docs/learning/00-runbook.md`，确认项目能启动。
2. 按 `docs/learning/README.md` 的顺序学习阶段 1 到阶段 8。
3. 每学完一个阶段，先做 `docs/exercises/*-questions.md`，不要马上看答案。
4. 做完后再对照 `docs/exercises/*-answers.md`。
5. 如果某一步卡住，把报错、你改过的文件、执行过的命令发给 ChatGPT，让它按 `AGENT.md` 的排错流程帮你定位。

## 文档目录

- `docs/learning/README.md`：总课程路线。
- `docs/learning/00-completion-scope.md`：学完后能得到什么版本，以及哪些内容属于扩展。
- `docs/learning/01-springboot-call-chain.md`：Spring Boot Web 最小闭环。
- `docs/learning/02-login-session-token.md`：登录、Session、Redis Token、拦截器。
- `docs/learning/03-cache-patterns.md`：商铺查询、缓存穿透、击穿、逻辑过期。
- `docs/learning/04-social-feed.md`：点赞、关注、共同关注、Feed 流。
- `docs/learning/05-redis-advanced.md`：签到 Bitmap、GEO 附近商铺。
- `docs/learning/06-seckill-basic.md`：秒杀基础版、事务、乐观锁、一人一单。
- `docs/learning/07-seckill-advanced.md`：分布式锁、Lua、Redis Stream 异步下单。
- `docs/learning/08-engineering-deploy-interview.md`：测试、部署、文档、简历和面试表达。
- `docs/learning/09-agent-assisted-development.md`：通过本项目学习 Agent 协作开发。
- `docs/learning/10-advanced-optimization-roadmap.md`：后续功能增强、工程优化和进阶路线。
- `docs/api/hmdp.http`：接口调试模板。
- `docs/troubleshooting.md`：常见错误排查手册。
- `docs/interview/hmdp-interview.md`：项目面试问答。
- `docs/agent/README.md`：Agent 开发任务板和协作规范。
- `docs/agent/prompt-templates.md`：可直接复制给 ChatGPT / Codex 的提示词模板。

## 做完后会得到什么

如果你按阶段 1 到阶段 8 的教程逐步实现并验证，最终会得到一个“学习版完整黑马点评后端项目”，覆盖：

- Spring Boot 后端基础接口。
- 验证码登录、Redis Token 登录态、拦截器、ThreadLocal。
- 商铺查询、商铺类型查询、Redis 缓存、缓存穿透/击穿处理。
- 博客列表、博客详情、点赞、关注、共同关注、Feed 流。
- Bitmap 签到、GEO 附近商铺。
- 优惠券秒杀基础版和 Lua + Redis Stream 异步秒杀进阶版。
- 接口测试、排错、部署、面试表达。

它不是生产级电商系统，但已经足够作为后端开发学习项目、Redis 实战项目和简历项目基础版。后续生产化增强放在 `docs/learning/10-advanced-optimization-roadmap.md`。

## Redis 内容覆盖情况

这套教学文档已经覆盖后端开发里非常核心的一组 Redis 实战内容：

- String：验证码、普通缓存、空值缓存、库存缓存。
- Hash：Token 登录态。
- Set：关注关系、共同关注、一人一单记录。
- Sorted Set：博客点赞排行、Feed 流收件箱。
- Bitmap：每日签到、连续签到统计。
- GEO：附近商铺查询。
- Lua：秒杀资格判断的原子性。
- Stream：异步秒杀订单消息、ACK、Pending List。
- 分布式锁：手写 Redis 锁和 Redisson 方案。

后续如果你想继续深挖 Redis，可以按文档里的优化路线补：缓存一致性延迟双删、布隆过滤器、热点 key 监控、Redis Cluster、Redisson 看门狗源码、消息可靠性补偿。

## 每阶段固定动作

每个阶段都建议按这个顺序来：

1. 看“本阶段目标”，知道这一步要解决什么问题。
2. 看“文件地图”，先定位代码，不急着改。
3. 看“核心概念”，先知道为什么要这样设计。
4. 按“实现步骤”一点点改，每完成一步就编译。
5. 按“验证方式”测试接口、数据库或 Redis。
6. 做练习题，再看参考答案。

## 重要约定

- 不要一次性把所有代码贴进去。每个阶段至少拆成 2 到 5 次小提交或小保存点。
- 不要从 `master` 分支整段复制。可以参考思路，但要先自己按文档写。
- 不要把真实密码、服务器 IP、密钥提交到仓库。配置优先使用环境变量。
- 遇到错误时，先看 `docs/troubleshooting.md`，再问 ChatGPT。

## 当前推荐起点

你已经完成了阶段 0 的项目地图和启动验证。下一步从这里开始：

```text
docs/learning/01-springboot-call-chain.md
```
