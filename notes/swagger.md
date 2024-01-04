## Swagger与Knife4j
- Swagger可以生成接口文档, 在线接口调试
- Knife4j是为Java MVC框架集成Swagger生成Api文档的插件



- 使用方法:
  1. 导入maven坐标: `knife4j-spring-boot-starter`
  2. 配置类中加入knife4j相关配置: [WebMvcConfiguration.java](sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java)中的docket和docketuser方法
  3. 设置静态资源映射,否则接口文档页面无法访问: [WebMvcConfiguration.java](sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java)中的addResourceHandlers方法(可以设置访问的页面路径)


- 常用注解(提高文档可读性)
  - `@Api` 用在类上, 例如controller, 表示对类的说明
  - `@ApiModel` 用在类上, 例如entity, DTO, VO
  - `@ApiModelProperty` 用在属性上, 描述属性信息
  - `@ApiOperation` 用在方法上, 例如controller的方法, 说明方法的用途和作用