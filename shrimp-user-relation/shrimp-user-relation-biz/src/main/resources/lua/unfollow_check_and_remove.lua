
local key = KEYS[1]
local unFollowUserId = ARGV[1]
--判断当前key是否存在
local exists = redis.call("exists",key)
if exists == 0 then
    return -1;
end
--去执行删除操作
local result = redis.call("zrem",key,unFollowUserId)
--result = 0
if result == 0 then
    return -2;
end
--result = 1
return 0;