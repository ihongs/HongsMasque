# 简单即时聊天平台

* 作者: Kevin Hongs
* 邮箱: kevin.hongs@gmail.com
* 授权: MIT License

因受够了第三方聊天平台, 繁复的对接模式, 这也要钱那也要钱的服务, 决定另起炉灶搞一个简单的聊天平台. 采用 WebSocket 为连接方式, 通过 Java 队列进行消息分发. 本系统可隔离对接多个站点——故称为"平台", 但性能应该还不怎么行, 主要服务对象为受众较少的网站和应用. 现阶段目标在于解决有无问题.

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

可以将构建的 `hongs-masque-web/target/HongsMasque` 拷贝出来, 这是最终的应用目录. Windows 下在此目录, 双击 `setup.bat` 即可完成设置, 双击 `start.bat` 立即启动服务; 注意: Windows 下关闭服务程序务必在命令窗口按 Ctrl+C 中止进程, 不要直接关闭命令窗口, 后者导致下次启动不了可尝试删除 var/server/8080.pid 文件再重新启动.

浏览器打开 `http://localhost:8080/` 进入后台.
