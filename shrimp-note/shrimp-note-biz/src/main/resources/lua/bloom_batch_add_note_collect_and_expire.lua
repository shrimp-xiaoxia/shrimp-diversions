-- 操作的 Key
local key = KEYS[1]

local exists = redis.call("exists", key)

for i = 1, #ARGV - 1 do
    redis.call("BF.ADD", key, ARGV[i])
end

-- 最后一个参数为过期时间
local expireTime = ARGV[#ARGV]
-- 设置过期时间
if exists == 0 then
    redis.call("EXPIRE", key, expireTime)
end
return 0
