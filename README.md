# sky-take-out-backend


- Swagger接口文档 ([WebMvcConfiguration.java](sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java))
- 扩展Spring MVC消息转换器, 对日期类型统一进行格式化处理 ([WebMvcConfiguration.java](sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java))
- 全局异常处理器 ([GlobalExceptionHandler.java](sky-server/src/main/java/com/sky/handler/GlobalExceptionHandler.java))
- AOP和反射实现公共字段自动填充 ([AutoFillAspect.java](sky-server/src/main/java/com/sky/aspect/AutoFillAspect.java))
- Interceptor拦截器 ([JwtTokenAdminInterceptor.java](sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java), [WebMvcConfiguration.java](sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java))
- 阿里云OSS文件上传 ([AliOssUtil.java](sky-common/src/main/java/com/sky/utils/AliOssUtil.java))
- Redis设置店铺状态,缓存菜品和套餐数据 ([RedisConfiguration.java](sky-server/src/main/java/com/sky/config/RedisConfiguration.java))
- 微信小程序登录与支付(已注释)
- Spring Task定时处理订单状态 ([OrderTask.java](sky-server/src/main/java/com/sky/task/OrderTask.java))
- WebSocket来单提醒和催单提醒 ([WebSocketServer.java](sky-server/src/main/java/com/sky/websocket/WebSocketServer.java), [WebSocketConfiguration.java](sky-server/src/main/java/com/sky/config/WebSocketConfiguration.java))
- Apache POI导出运营数据Excel ([ReportServiceImpl.java](sky-server/src/main/java/com/sky/service/impl/ReportServiceImpl.java))
