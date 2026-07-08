# 黑马点评常见排错手册

遇到错误时，先不要重装环境。按“错误类型 -> 关键日志 -> 最可能原因 -> 最小修复”来排查。

## 1. 编译错误

### 典型现象

```text
Compilation failure
cannot find symbol
method does not override or implement a method from a supertype
```

### 排查顺序

1. 看第一条 `cannot find symbol`，不要被后面一堆错误吓到。
2. 检查 import 是否缺失。
3. 检查接口里是否声明了方法，实现类是否 `@Override`。
4. 检查类名、方法名、参数类型是否一致。

### 最小修复

执行：

```bash
mvn -q -DskipTests compile
```

每次只修第一处错误，再重新编译。

## 2. 启动错误

### 端口被占用

关键日志：

```text
Port 8081 was already in use
```

Windows 查看：

```powershell
Get-NetTCPConnection -LocalPort 8081 -State Listen
```

处理：

```powershell
Stop-Process -Id 进程ID -Force
```

### Bean 注入失败

关键日志：

```text
No qualifying bean of type
```

常见原因：

1. 类忘记加 `@Service`、`@Component`、`@Configuration`。
2. 包路径不在启动类扫描范围内。
3. 接口有多个实现但没有指定注入哪个。

## 3. 数据库错误

### 密码错误或没传密码

关键日志：

```text
Access denied for user 'root'@'localhost' (using password: NO)
```

含义：程序没有拿到数据库密码。

修复：

```powershell
$env:HMDP_DB_PASSWORD="你的密码"
mvn spring-boot:run
```

### 数据库不存在

关键日志：

```text
Unknown database 'hmdp'
```

修复：

```sql
CREATE DATABASE hmdp;
```

然后导入 `hmdp.sql`。

### 表不存在

关键日志：

```text
Table 'hmdp.tb_shop' doesn't exist
```

含义：库建了，但 SQL 没导入或导入到别的库。

## 4. Redis 错误

### Redis 连接不上

关键日志：

```text
Unable to connect to Redis server
Connection refused
```

检查：

```powershell
Test-NetConnection 127.0.0.1 -Port 6379
```

如果 Redis 在虚拟机或远程机器：

```powershell
$env:HMDP_REDIS_HOST="192.168.150.101"
```

### Stream 消费组不存在

关键日志：

```text
NOGROUP No such key 'stream.orders' or consumer group 'g1'
```

修复：

```bash
redis-cli
XGROUP CREATE stream.orders g1 0 MKSTREAM
```

如果已经存在会报错，可以忽略。

## 5. 接口错误

### 401 未登录

常见原因：

1. 没带 `authorization` header。
2. token 拼错。
3. Redis 登录态过期。
4. 登录接口被错误拦截。

检查：

```bash
hgetall login:token:你的token
ttl login:token:你的token
```

### 服务器异常

项目的 `WebExceptionAdvice` 会把运行时异常包装成：

```json
{"success":false,"errorMsg":"服务器异常"}
```

这时一定要看后端控制台日志，不要只看接口响应。

## 6. 逻辑错误

### 缓存更新后数据没变

检查：

1. 更新接口是否真的更新数据库。
2. 更新后是否删除 `cache:shop:{id}`。
3. 前端是否请求了正确 id。

### 点赞状态不对

检查：

```bash
zscore blog:liked:博客id 用户id
select liked from tb_blog where id = 博客id;
```

数据库点赞数和 Redis 点赞集合要一起看。

### 秒杀重复下单

检查：

```sql
select user_id, voucher_id, count(*)
from tb_voucher_order
group by user_id, voucher_id
having count(*) > 1;
```

建议加唯一索引兜底：

```sql
alter table tb_voucher_order add unique key uk_user_voucher(user_id, voucher_id);
```

## 7. 问 ChatGPT 时的最佳提问模板

```text
我在黑马点评项目的阶段 X 遇到了错误。
我改过的文件：
1. ...
2. ...

执行的命令：
...

接口请求：
...

关键报错：
...

请按 AGENT.md 的排错模式帮我判断错误类型、最可能原因和最小修复。
```

