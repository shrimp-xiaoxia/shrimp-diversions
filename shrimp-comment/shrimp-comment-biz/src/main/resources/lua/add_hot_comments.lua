-- 操作的 key
local zsetKey = KEYS[1]
-- 获取传入的成员和分数表
local memberScores = ARGV
-- ZSet 最多缓存500条评论
local sizeLimit = 500

-- 检查zset是否存在
if redis.call('exists', zsetKey) == 0 then
    return -1; -- 如不存在，直接 return
end

-- 获取当前ZSet的大小
local currentSize = redis.call('zcard', zsetKey)
--遍历传入的成员和分数，添加到zset中
for i = 1, #memberScores, 2 do
    --评论ID
    local member = memberScores[i]
    local score = memberScores[i + 1]

    if currentSize < sizeLimit then
        --若是,则添加缓存
        redis.call('zadd', zsetKey, score, member)
        -- 更新 ZSet 大小
        currentSize = currentSize + 1
    else
        -- 否则，则达到最大限制，停止添加
        break
    end
end

return 0