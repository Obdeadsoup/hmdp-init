# 阶段 2 参考答案：登录、会话与拦截器

## 概念题参考答案

1. Session 默认存在单台服务器内存里，多实例时用户请求打到另一台机器就找不到 Session，除非做 Session 共享。
2. token 是前端持有的随机字符串；Redis key 是 `login:token:{token}`；UserDTO 是服务端保存的安全用户信息。
3. `User` 可能包含密码、隐私字段和数据库内部字段；登录态只需要 id、昵称、头像等最小信息。
4. 刷新拦截器负责“有 token 就解析并续期”，登录拦截器负责“必须登录的接口没用户就拒绝”，职责分开更清楚。
5. Tomcat 线程会复用，如果不清理 ThreadLocal，下一个请求可能读到上一个用户的信息。

## 代码阅读题参考答案

1. 拦截器在请求进入 Controller 前把用户保存进 ThreadLocal，同一请求线程中的 Controller/Service 都可以通过 `UserHolder.getUser()` 获取。
2. `LOGIN_CODE_KEY` 保存验证码；`LOGIN_CODE_TTL` 是验证码过期时间；`LOGIN_USER_KEY` 保存 token 对应用户；`LOGIN_USER_TTL` 是登录态过期时间。

## 小改造参考

```java
@GetMapping("/me")
public Result me() {
    UserDTO user = UserHolder.getUser();
    return Result.ok(user);
}
```

## 面试表达参考

“用户先用手机号请求验证码，服务端生成验证码并存入 Redis，key 是 `login:code:{phone}`。用户提交手机号和验证码后，服务端校验验证码，查不到用户就自动注册。登录成功后生成 token，把 UserDTO 转成 Hash 存入 Redis，key 是 `login:token:{token}`，并把 token 返回前端。后续请求前端在 header 中携带 token，拦截器解析 token、查询 Redis、刷新 TTL，并把用户保存到 ThreadLocal。”

