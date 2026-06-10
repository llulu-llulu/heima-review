---@diagnostic disable: undefined-global
---
--- Created by 库库鲁
--- DateTime: 2026/5/27 13:54
---
if redis.call("get", KEYS[1]) == ARGV[1] then
	return redis.call("del", KEYS[1])
else
	return 0
end

