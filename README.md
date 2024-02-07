# eagle
### 简介
eagle是一个开源项目，目的是希望给需要从 spring boot 3.0 开始构建权限系统的人提供一些帮助或灵感。项目使用 kotlin 开发，

**NOTE**：这是一个示例项目，仅提供参考


### 用到的技术/框架/工具
* gradle 8.x
* spring boot 3.x
* kotlin 1.9.x
* jdk 17

### 写在前面的话

为什么建这个仓库？主要是有下面几个原因
1. 在开始准备维护一个长期项目，需要用到 oauth2 来实现 token 颁发和各个服务间的鉴权。刚开始调研时发现spring boot 2.x用的oauth2.0那一套已经过时了，所以花了一些时间学习 **Spring Authorization Server**的使用，并将所
2. 希望在学习过程中学一学怎么维护好一个项目
3. 选用 kotlin 开发，单纯觉得 kotlin 写起来比 java 简洁

### 项目结构

| module | 描述               | readme                                                              |
|--------|------------------|---------------------------------------------------------------------|
| eagle-authorization-server | 服务授权；token发布、刷新； | [eagle-authorization-server](/eagle-authorization-server/README.md) |


### todolist

- [ ] authorization Server
- [ ] resource Server
- [ ] client 


