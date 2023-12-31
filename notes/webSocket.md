## WebSocket
- 基于TCP的网络协议,实现浏览器与服务器全双工通信
- 全双工通信:只需完成一次握手,就可以创建持久性的连接,并进行双向数据传输


- WebSocket与HTTP协议的异同
  - 相同点
    - 底层都是TCP连接
  - 不同点
    - HTTP是短连接,HTTP通信是单向的,基于请求响应模式(一个request请求,一个response)
    - WebSocket是长连接,支持双向通信,双向都可以主动发消息


- 应用场景
  - 视频弹幕
  - 网页聊天
  - 体育实况更新
  - 股票基金报价实时更新


- 使用步骤
  1. 准备WebSocket客户端
  2. 导入maven坐标：spring-boot-starter-websocket
  3. 导入WebSocket服务端组件WebSocketServer, 用于和客户端通信 
  4. 导入配置类WebSocketConfiguration,注册WebSocket的服务端组件