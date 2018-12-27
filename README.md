# 简单即时聊天平台

* 作者: Kevin Hongs
* 邮箱: kevin.hongs@gmail.com
* 授权: MIT License

因受够了第三方聊天平台, 繁复的对接模式, 这也要钱那也要钱的服务, 决定另起炉灶搞一个简单的聊天平台. 采用 WebSocket 为连接方式, 通过 Java 队列进行消息分发. 本系统可隔离对接多个站点——故称为"平台", 但性能应该还不怎么行, 主要服务对象为受众较少的网站和应用. 现阶段目标在于解决有无问题.

本项目分 3 个阶段进行, 第一阶段实现功能完备和稳定的实时聊天, 含单聊和群聊; 第二阶段增加客服功能, 可管理和分配客服人员; 第三阶段增加智能客服, 通过学习自动完成常规的客服问答, 并在无法解决时转接人工. 而拆分架构, 设立负载均衡和分布式管道, 提高联通效率, 支持水平扩展, 这些暂不在考虑范围内. 这将长期作为一个微型服务, 容易部署、易于改写才是目标.

## 部署方式

本项目代码依赖 HongsCORE,HongsCRAP 两个项目, 故需先获取并构建这两个系统. 完整流程如下:

```bash
# 构建 HongsCORE
git clone https://github.com/ihongs/HongsCORE.git
cd HongsCORE
mvn package
cd ..

# 构建 HongsCRAP
git clone https://github.com/ihongs/HongsCRAP.git
cd HongsCRAP
mvn package
cd ..

# 构建、设置并启动
git clone https://github.com/ihongs/HongsMasque.git
cd HongsMasque
mvn package
cd hongs-masque-web/target/HongsMasque
# 初始设置
sh bin/hdo system.setup --DEBUG 0
# 启动服务
sh bin/hdo server.start --DEBUG 1
```

可以将构建的 `hongs-masque-web/target/HongsMasque` 拷贝出来, 这是最终的应用目录. 也可以发邮件到 kevin.hongs@gmail.com 索要构建好的包(JDK 请自行下载和安装).

Windows 下进入此项目文件夹, 双击 `setup.bat` 即可完成设置, 双击 `start.bat` 立即启动服务; 注意: Windows 下如需关闭服务程序, 务必要在命令窗口按 `Ctrl+C` 中止进程, 不要直接关闭命令窗口. 后者导致下次无法启动, 可尝试删除 `var/server/8080.pid` 后重新启动.

浏览器打开 <http://localhost:8080/> 可进入后台.
