-- 操作的 Key
local key = KEYS[1]

-- 准备批量添加数据的参数
local zaddArgs = {}

--缓存的数据是zset类型，自动去重，可以看成多次批量插入幂等，但是为了保持编码良好习惯,避免重复插入
local exists = redis.call("exists",key)
if exists == 1 then
    return 0
end

-- 遍历 ARGV 参数，将分数和值按顺序插入到 zaddArgs 变量中
for i = 1, #ARGV - 1, 2 do
    table.insert(zaddArgs, ARGV[i])      -- 分数（关注时间）
    table.insert(zaddArgs, ARGV[i+1])    -- 值（关注的用户ID）
end

-- 调用 ZADD 批量插入数据
redis.call('ZADD', key, unpack(zaddArgs))

-- 设置 ZSet 的过期时间
local expireTime = ARGV[#ARGV] -- 最后一个参数为过期时间
redis.call('EXPIRE', key, expireTime)

return 0