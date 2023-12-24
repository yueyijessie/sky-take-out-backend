## Redis保存的是key-value, value的5种常用的数据类型
- `string`
- `hash`，类似于java中的hashmap
- `list`列表，可以有重复元素，类似于java的linkedlist
- `set`集合，无序，没有重复元素，类似于java的hashset
- `sorted set / zset`有序集合，每个元素有一个score，根据score升序排序，没有重复元素

## 常用命令
- 字符串操作命令
  - `set key value` 设置指定key的值
  - `get key` 获取指定key的值
  - `setex key seconds value` 设置指定key的值，并将key的过期时间设定为seconds秒
  - `setnx key value` 只有在key不存在时，设置key的值


- Hash操作命令
    - `hset key field value` 新建key，它的值是（field和value）
    - `hget key field` 获取指定key中field的值
    - `hdel key field` 删除指定key的field字段
    - `hkeys key` 获取全部字段
    - `hvals key` 获取全部值


- List操作命令
    - `lpush key value1 value2` 新建key，值是list，value1和value2插入到list的**头部**
      - `lpush mylist a b c` 会形成一个`c,b,a`的list
    - `lrange key start stop` 获取list的范围内的元素，start和stop索引表示
      - 查询全部start和stop为（0，-1） e.g.`lrange mylist 0 -1`
    - `rpop key` 移除并获取list最后一个元素
      - 删除最右侧的元素，`rpop mylist`会删除元素a
    - `llen key` 获取list长度


- Set操作命令
    - `sadd key member1 member2` 新建key，值是set，并添加一个或多个元素
    - `smembers key` 返回集合中全部成员
    - `scard key` 获取集合的成员数
    - `sinter key1 key2` 返回给定所有集合的交集
    - `sunion key1 key2` 返回所有给定集合的并集
    - `srem key member1 member2` 删除集合中一个或多个成员


- Zset操作命令
  - `zadd key score1 member1 score2 member2` 向有序集合添加一个或多个成员
  - `zrange key start stop [withscores]` 通过索引区间返回有序集合中指定区间内的成员
  - `zincrby key increment member` 有序集合中对指定成员的分数加上增量increment
  - `zrem key member1 member2` 删除有序集合中的一个或多个成员


- 通用命令
  - `keys pattern` 查找符合给定pattern的key
    - `keys set*` 匹配set开头的key
  - `exists key` 检查key是否存在
  - `type key` 返回key储存值的类型
  - `del key` 删除