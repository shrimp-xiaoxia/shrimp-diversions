
local key = KEYS[1]
local userId = ARGV[1]
local timestamp = ARGV[2]

-- 使用 EXISTS 命令检查 ZSET 粉丝列表是否存在
local exists = redis.call("exists",key)
if exists == 0 then
    return -1
end

local size = redis.call("zcard",key)
-- 若超过 5000 个粉丝，则移除最早关注的粉丝
if size >= 5000 then
    redis.call("zpopmin",key)
end
-- 添加新的粉丝关系
redis.call("zadd",key,timestamp,userId)

return 0