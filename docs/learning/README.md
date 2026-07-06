# 黑马点评后端学习路线

这份路线对应一个从 0.5 到能独立讲清项目的学习过程。每个阶段都围绕一个真实后端能力展开，目标不是“把代码写完”，而是让你能解释、实现、验证、排错。

## 路线总览

| 阶段 | 文件 | 主题 | 最终能力 |
|---|---|---|---|
| 0 | `00-project-map.md`、`00-runbook.md` | 环境与项目地图 | 能启动项目，知道目录职责 |
| 1 | `01-springboot-call-chain.md` | Spring Boot 基础链路 | 能写和追踪简单查询接口 |
| 2 | `02-login-session-token.md` | 登录与拦截器 | 能实现验证码登录和 Token 登录 |
| 3 | `03-cache-patterns.md` | Redis 缓存 | 能处理穿透、击穿、缓存一致性 |
| 4 | `04-social-feed.md` | 社交与 Feed | 能用 Redis Set/ZSet 做点赞关注 |
| 5 | `05-redis-advanced.md` | Bitmap 与 GEO | 能实现签到和附近商铺 |
| 6 | `06-seckill-basic.md` | 秒杀基础 | 能解释并解决超卖和一人一单 |
| 7 | `07-seckill-advanced.md` | Lua、锁、Stream | 能设计异步秒杀链路 |
| 8 | `08-engineering-deploy-interview.md` | 工程化与表达 | 能测试、部署、写简历和面试表达 |
| 9 | `09-agent-assisted-development.md` | Agent 协作开发 | 能用 AI 辅助拆任务、审查、排错 |
| 10 | `10-advanced-optimization-roadmap.md` | 后续优化 | 能规划生产化增强路线 |

## 如何判断自己能进入下一阶段

每阶段结束前，你至少要做到：

1. 能画出本阶段的调用链。
2. 能说清涉及的 3 到 5 个核心概念。
3. 能独立跑通验证接口。
4. 能解释至少一个常见坑。
5. 能完成对应练习题，再对照答案修正。

## 延伸路线

阶段 1 到 8 是项目主线。阶段 9 和 10 是延伸路线：

- 阶段 9 帮你把这个项目变成 Agent 协作开发训练场。
- 阶段 10 帮你规划继续优化项目的路线，适合做简历增强和长期打磨。

## 建议提交节奏

如果你把项目放回 Git 仓库中，建议这样提交：

```bash
git checkout -b learn/main
git checkout -b feat/login-token
git add .
git commit -m "learn: explain and implement login token"
```

每个阶段的功能不要混在一个提交里。后端学习最怕“代码看着都在，但自己不知道哪一步让它跑起来的”。
