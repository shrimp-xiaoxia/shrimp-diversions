-- 操作的 key
local zsetKey = KEYS[1]
local args = {}
-- 检查zset是否存在
if redis.call('exists', zsetKey) == 1 then
    return 0; -- 如存在，直接 return 0
end

for i = 1, #ARGV - 1, 2 do
    table.insert(args, ARGV[i])
    table.insert(args, ARGV[i + 1])
end

redis.call("zadd", zsetKey, unpack(args))

local expiredTime = ARGV[#ARGV]
redis.call("expire", zsetKey, expiredTime)