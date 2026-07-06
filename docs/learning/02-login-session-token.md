# 阶段 2：登录、会话与拦截器

## 本阶段目标

先实现 Session 版验证码登录，再升级到 Redis Token 登录。你要理解登录态不是“用户输入过手机号”这么简单，而是服务端如何在后续请求中识别“你是谁”。

## 文件地图

- `UserController`：登录接口入口。
- `IUserService`：定义发送验证码、登录等用户业务。
- `UserServiceImpl`：实现手机号校验、验证码校验、注册、登录态保存。
- `LoginFormDTO`：登录请求体。
- `UserDTO`：登录态中保存的安全用户信息。
- `User`：数据库用户实体。
- `RegexUtils`：手机号格式校验。
- `UserHolder`：基于 ThreadLocal 保存当前请求用户。
- `LoginInterceptor`：拦截需要登录的接口。
- 建议新增 `RefreshTokenInterceptor`：刷新 Redis Token 有效期。
- 建议新增 `WebMvcConfig`：注册拦截器。

## 核心概念

### 1. Session 登录

Session 版登录的思路：

1. 用户请求验证码。
2. 服务端生成验证码，保存到 `HttpSession`。
3. 用户提交手机号和验证码。
4. 服务端从 Session 取验证码比对。
5. 登录成功后，把用户信息放进 Session。
6. 后续请求靠浏览器自动携带的 Cookie 找到 Session。

优点是简单。缺点是多台后端实例时 Session 不共享，不适合分布式。

### 2. Redis Token 登录

Redis Token 版登录的思路：

1. 验证码保存到 Redis：`login:code:{phone}`。
2. 登录成功后生成随机 token。
3. 用户信息保存到 Redis Hash：`login:token:{token}`。
4. token 返回给前端，前端后续请求放在 `authorization` 请求头。
5. 拦截器根据 token 到 Redis 查用户。
6. 查到用户后放入 `UserHolder`。

优点是多实例共享登录态，适合后续项目扩展。

### 3. ThreadLocal

`UserHolder` 里的 ThreadLocal 用来保存“当前请求的用户”。它只在当前线程可见，所以 Controller 和 Service 都可以方便取到当前登录用户。

一定要在请求结束后调用 `removeUser()`。否则 Tomcat 线程会复用，可能导致用户串号。

## 实现步骤 A：Session 版

### 步骤 1：定义 Service 方法

在 `IUserService` 中增加：

```java
Result sendCode(String phone, HttpSession session);

Result login(LoginFormDTO loginForm, HttpSession session);
```

### 步骤 2：实现发送验证码

在 `UserServiceImpl` 中实现：

```java
@Override
public Result sendCode(String phone, HttpSession session) {
    if (RegexUtils.isPhoneInvalid(phone)) {
        return Result.fail("手机号格式错误");
    }
    String code = RandomUtil.randomNumbers(6);
    session.setAttribute("code", code);
    log.debug("发送短信验证码成功，验证码：{}", code);
    return Result.ok();
}
```

需要用到 Hutool：

```java
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
```

并给类加 `@Slf4j`。

### 步骤 3：实现登录

核心流程：

```java
String phone = loginForm.getPhone();
if (RegexUtils.isPhoneInvalid(phone)) {
    return Result.fail("手机号格式错误");
}

Object cacheCode = session.getAttribute("code");
String code = loginForm.getCode();
if (cacheCode == null || !cacheCode.toString().equals(code)) {
    return Result.fail("验证码错误");
}

User user = query().eq("phone", phone).one();
if (user == null) {
    user = createUserWithPhone(phone);
}

session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
return Result.ok();
```

新增私有方法：

```java
private User createUserWithPhone(String phone) {
    User user = new User();
    user.setPhone(phone);
    user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
    save(user);
    return user;
}
```

### 步骤 4：Controller 调用 Service

`UserController` 中：

```java
@PostMapping("code")
public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
    return userService.sendCode(phone, session);
}

@PostMapping("/login")
public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
    return userService.login(loginForm, session);
}
```

## 实现步骤 B：Redis Token 版

Session 版跑通后，再改 Redis 版。

### 步骤 1：注入 `StringRedisTemplate`

在 `UserServiceImpl`：

```java
@Resource
private StringRedisTemplate stringRedisTemplate;
```

### 步骤 2：验证码保存到 Redis

```java
stringRedisTemplate.opsForValue().set(
        RedisConstants.LOGIN_CODE_KEY + phone,
        code,
        RedisConstants.LOGIN_CODE_TTL,
        TimeUnit.MINUTES
);
```

### 步骤 3：登录时从 Redis 取验证码

```java
String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
    return Result.fail("验证码错误");
}
```

### 步骤 4：保存登录态到 Redis Hash

```java
String token = UUID.randomUUID().toString(true);
UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
Map<String, Object> userMap = BeanUtil.beanToMap(
        userDTO,
        new HashMap<>(),
        CopyOptions.create()
                .setIgnoreNullValue(true)
                .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())
);
String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
return Result.ok(token);
```

注意：Redis Hash 的 value 最好转成字符串，否则 `StringRedisTemplate` 可能序列化失败。

### 步骤 5：刷新 Token 拦截器

建议新增 `RefreshTokenInterceptor`：

```java
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if (userMap.isEmpty()) {
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
```

### 步骤 6：登录拦截器

`LoginInterceptor` 只做一件事：需要登录的接口，如果 `UserHolder` 没有用户，就拦住。

```java
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
```

### 步骤 7：注册拦截器

新增 `WebMvcConfig`：

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/shop/**",
                        "/shop-type/**",
                        "/voucher/**",
                        "/upload/**",
                        "/blog/hot"
                ).order(1);

        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
    }
}
```

顺序很重要：刷新 Token 的拦截器先运行，把用户放进 `UserHolder`；登录拦截器后运行，检查用户是否存在。

### 步骤 8：实现 `/user/me`

```java
@GetMapping("/me")
public Result me() {
    UserDTO user = UserHolder.getUser();
    return Result.ok(user);
}
```

## 验证方式

发送验证码：

```bash
curl -X POST "http://127.0.0.1:8081/user/code?phone=13800138000"
```

登录：

```bash
curl -X POST "http://127.0.0.1:8081/user/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"phone\":\"13800138000\",\"code\":\"控制台打印的验证码\"}"
```

携带 token 获取当前用户：

```bash
curl "http://127.0.0.1:8081/user/me" -H "authorization: 登录返回的token"
```

Redis 检查：

```bash
redis-cli
keys login:*
hgetall login:token:你的token
ttl login:token:你的token
```

## 常见坑

1. `authorization` 请求头名字写错。
2. Redis Hash 的值没有转字符串。
3. 忘记刷新 Token TTL。
4. 忘记 `UserHolder.removeUser()`。
5. 两个拦截器顺序写反。
6. 登录接口被登录拦截器拦住。

## 本阶段你要掌握

- Session 和 Cookie 的关系。
- Redis Token 登录态的保存方式。
- DTO 为什么不能直接用 Entity 替代。
- 拦截器链路和顺序。
- ThreadLocal 的使用和清理。

