-- lua脚本：评论点赞布隆过滤器

--操作的Redis KEY
local key = KEYS[1]
-- 评论ID
local commentId = ARGV[1]
-- 判断该布隆过滤器存不存在
local exists = redis.call("exists", key)
if exists == 0 then
    return -1
end
-- 检验该评论是否被点赞过(1表示点赞过，0表示没有点赞)
local isLiked = redis.call("BF.EXISTS", key, commentId)
if isLiked == 1 then
    return 1
end
-- 没被点赞，则添加点赞数据
redis.call("BF.ADD", key, commentId)
return 0