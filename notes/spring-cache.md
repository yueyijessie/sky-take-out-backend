## Spring Cache
- 常用注解
    - `@EnableCaching` 开启缓存注解功能，通常加在启动类上
      ```java
      @Slf4j
      @SpringBootApplication
      @EnableCaching // 开启缓存注解功能
      public class CacheDemoApplication {
        public static void main(String[] args) {
          SpringApplication.run(CacheDemoApplication.class,args);
          log.info("项目启动成功...");
        }
      }
      ```

    - `@Cacheable` 在方法执行前先查询缓存中是否有数据，如果有数据，则直接返回缓存数据；如果没有缓存数据，调用方法并将方法返回值放到缓存中
      ```java
      @GetMapping
      // key中的id对应的是形参id，这里不能使用result，key的生成也是userCache::1
      @Cacheable( cacheNames = "userCache", key = "#id")
      public User getById(Long id){
        User user = userMapper.getById(id);
        return user;
      }
      ```
      
    - `@CachePut` 将方法的返回值放到缓存中
      ```java
      @PostMapping
      // 使用spring cache缓存数据，key的生成：userCache::1
      // key是使用spring EL动态获取的，user与参数user保持一致，或者从result返回结果中获取（result指的是return回来的user）
      // @CachePut(cacheNames = "userCache", key = "#result.id")
      @CachePut(cacheNames = "userCache", key = "#user.id")
      public User save(@RequestBody User user){
        userMapper.insert(user);
        return user;
      }
      ```
      
    - `@CacheEvict` 将一条或多条数据从缓存中删除
      ```java
      @DeleteMapping
      @CacheEvict(cacheNames = "userCache", key = "#id") // 删除对应id的缓存
      public void deleteById(Long id){
        userMapper.deleteById(id);
      }
    
      @DeleteMapping("/delAll")
      @CacheEvict(cacheNames = "userCache", allEntries = true) // userCache下所有键值对都会删除
      public void deleteAll(){
        userMapper.deleteAll();
      }
      ```