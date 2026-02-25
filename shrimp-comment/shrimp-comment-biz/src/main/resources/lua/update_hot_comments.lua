-- 入参说明：
-- KEYS[1]: ZSet 的键
-- ARGV: 每个评论的数据，格式为 member1, score1, member2, score2 ...

local zsetKey = KEYS[1]
local batchSize = #ARGV / 2
local maxSize = 500
-- 判断zSet存不存在
local hasKey = redis.call("exists", zsetKey)
if hasKey == 0 then
    return -1
end
-- 存在
-- 遍历参数列表
for i = 1, batchSize do
    -- 提取参数
    local commentId = ARGV[1 + 2 * (i - 1)]
    local score = ARGV[2 + 2 * (i - 1)]

    local size = redis.call('zcard', zsetKey)
    --判断zset大小个数
    if size < maxSize then
        --如果没有满足，则直接添加
        redis.call('zadd',zsetKey, score, commentId)
    else
        local minEntry = redis.call('zrange', zsetKey, 0, 0, 'withscores')
        --如果已经满足500了，则比较
        if score > minEntry[2] then
            redis.call("zrem", zsetKey, minEntry[1])
            redis.call("zadd", zsetKey, score, commentId)
        end
    end

end

