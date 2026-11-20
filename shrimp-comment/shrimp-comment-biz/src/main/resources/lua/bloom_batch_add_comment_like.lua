-- 批量添加用户点赞过的评论 ID

-- 操作的Redis KEY
local key = KEYS[1]
local exist = redis.call("exists", key)
-- 遍历参数列表，添加评论 ID
for i = 1, #ARGV - 1 do
    redis.call("BF.ADD", key, ARGV[i])
end
-- 设置过期时间
local expireSecond = ARGV[#ARGV]
if exist == 0 then
    redis.call("expire", key, expireSecond)
end
return 0
