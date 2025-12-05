local key = KEYS[1]

local exists = redis.call('exists', key)
if exists == 1 then
    return 0
end

local args = {}

for i = 1, #ARGV - 1, 2 do
    table.insert(args, ARGV[i])
    table.insert(args, ARGV[i + 1])
end

redis.call('zadd', key, unpack(args))
local expireSecond = ARGV[#ARGV]

redis.call('expire', key, expireSecond)