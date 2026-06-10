---@diagnostic disable: undefined-global

-- 参数列表：优惠券 id、用户 id、订单 id。
local voucher_id = ARGV[1]
local user_id = ARGV[2]
local order_id = ARGV[3]

-- 库存 key：seckill:stock:{voucherId}；下单用户集合 key：seckill:order:{voucherId}。
local SECKILL_STOCK_KEY = "seckill:stock:" .. voucher_id
local SECKILL_ORDER_KEY = "seckill:order:" .. voucher_id

-- 1. 判断库存是否充足。
local stock = tonumber(redis.call("get", SECKILL_STOCK_KEY))
if stock == nil or stock <= 0 then
    return 1
end

-- 2. 判断用户是否已经下过单，保证“一人一单”。
local order = redis.call("sismember", SECKILL_ORDER_KEY, user_id)
if order == 1 then
    return 2
end

-- 3. 预扣 Redis 库存，并记录用户已下单标记。
redis.call("incrby", SECKILL_STOCK_KEY, -1)
redis.call("sadd", SECKILL_ORDER_KEY, user_id)

-- 4. 把订单消息写入 Redis List 主队列，后台线程异步创建数据库订单。
redis.call("rpush", "queue:order", cjson.encode({
    id = order_id,
    userId = user_id,
    voucherId = voucher_id
}))

return 0
