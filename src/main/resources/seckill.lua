-- KEYS[1] 库存key
-- KEYS[2] 已下单用户Set
-- KEYS[3] 开始时间key
-- KEYS[4] 结束时间key
-- KEYS[5] Stream key

-- ARGV[1] userId
-- ARGV[2] voucherId
-- ARGV[3] orderId
-- ARGV[4] 当前时间戳

local stock=tonumber(redis.call('GET',KEYS[1]))
local beginTime=tonumber(redis.call('get',KEYS[3]))
local endTime=tonumber(redis.call('get',KEYS[4]))
local now=tonumber(ARGV[4])

-- 秒杀数据为初始化
if stock==nil
        or beginTime==nil
        or endTime==nil then
    return 5
end

-- 秒杀活动未开始
if now<beginTime then
    return 3
end

-- 秒杀活动已结束
if now>endTime then
    return 4
end

-- 库存不足
if stock<=0 then
    return 1
end

-- 检查一人一单
if redis.call(
        'sismember',
        KEYS[2],
        ARGV[1]
)==1 then
    return 2
end

-- 扣Redis库存
redis.call('incrby',KEYS[1],-1)
-- 写Redis记录用户已取得资格
redis.call('sadd',KEYS[2],ARGV[1])
-- 订单消息写入Stream
redis.call(
    'XADD',
    KEYS[5],
    '*',
    'id',ARGV[3],
    'userId',ARGV[1],
    'voucherId',ARGV[2]
)

return 0