# Java远程文件管理系统 - 技术文档

## 1. 系统概述

Java远程文件管理系统是一个基于Java Socket实现的客户端-服务器架构应用程序，允许用户通过网络远程管理文件。系统使用纯Java原生API实现，不依赖第三方库，提供文件浏览、上传、下载、删除等基本功能。

## 2. 系统架构

系统采用经典的C/S（客户端/服务器）架构，分为服务器端和客户端两个主要部分。

### 2.1 服务器端

服务器端负责处理客户端请求，管理文件系统操作，实现权限控制，处理并发连接。主要包括以下组件：

- **FileServer**: 主服务器类，监听客户端连接请求，为每个连接创建处理线程。
- **ClientHandler**: 处理单个客户端连接的线程，接收并解析客户端命令，执行相应的文件操作。

### 2.2 客户端

客户端负责提供用户界面，发送请求到服务器，显示文件列表，处理文件上传/下载操作。主要包括以下组件：

- **FileClient**: 主客户端类，处理与服务器的连接和通信。
- **CommandLineInterface**: 命令行界面，提供基于文本的用户交互。
- **GUI**: 图形用户界面，提供可视化的文件管理操作。

## 3. 通信协议

系统使用自定义的简单文本协议进行通信，基于TCP Socket实现。

### 3.1 请求格式
```
COMMAND|PARAM1|PARAM2|...|PARAMn
```

### 3.2 响应格式
```
STATUS_CODE|MESSAGE|DATA
```

- STATUS_CODE: 200(成功), 400(客户端错误), 500(服务器错误)
- MESSAGE: 状态描述
- DATA: 响应数据（如文件列表、文件内容等）

### 3.3 支持的命令

| 命令 | 格式 | 描述 |
|------|------|------|
| LIST | `LIST\|path` | 列出指定路径下的文件和目录 |
| DOWNLOAD | `DOWNLOAD\|path` | 下载指定文件 |
| UPLOAD | `UPLOAD\|path\|size` | 上传文件到指定路径 |
| DELETE | `DELETE\|path` | 删除指定文件或目录 |
| MKDIR | `MKDIR\|path` | 创建新目录 |
| RENAME | `RENAME\|oldPath\|newPath` | 重命名文件或目录 |

## 4. 核心功能实现

### 4.1 服务器端实现

#### 4.1.1 服务器启动与连接管理
服务器通过`ServerSocket`监听指定端口，使用线程池管理客户端连接，确保系统能够处理多个并发连接。

```java
serverSocket = new ServerSocket(port);
Socket clientSocket = serverSocket.accept();
ClientHandler clientHandler = new ClientHandler(clientSocket, rootDirectory);
threadPool.submit(clientHandler);
```

#### 4.1.2 文件操作实现
服务器使用Java NIO的`Files`和`Paths`API处理文件操作，包括：

- 列出目录内容：`Files.list(path)`
- 读取文件：`Files.readAllBytes(path)`
- 写入文件：`Files.write(path, bytes)`
- 删除文件：`Files.delete(path)`
- 创建目录：`Files.createDirectories(path)`
- 移动/重命名：`Files.move(source, target)`

#### 4.1.3 安全考虑
- 路径规范化：防止路径遍历攻击
```java
String normalizedPath = relativePath.replace("\\", "/");
while (normalizedPath.startsWith("/")) {
    normalizedPath = normalizedPath.substring(1);
}
return Paths.get(rootDirectory, normalizedPath);
```

### 4.2 客户端实现

#### 4.2.1 连接管理
客户端使用`Socket`连接到服务器，并创建输入输出流进行通信。

```java
socket = new Socket(serverAddress, serverPort);
in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
out = new PrintWriter(socket.getOutputStream(), true);
dataIn = new DataInputStream(socket.getInputStream());
dataOut = new DataOutputStream(socket.getOutputStream());
```

#### 4.2.2 命令发送与响应处理
客户端将命令格式化为协议格式并发送到服务器，然后解析服务器响应。

```java
out.println(command);
String response = in.readLine();
return response.split("\\|", 3);
```

#### 4.2.3 文件传输
文件传输使用二进制流进行，通过`DataInputStream`和`DataOutputStream`处理。

上传文件：
```java
try (FileInputStream fis = new FileInputStream(localFile)) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = fis.read(buffer)) != -1) {
        dataOut.write(buffer, 0, bytesRead);
    }
}
```

下载文件：
```java
try (FileOutputStream fos = new FileOutputStream(localFilePath.toFile())) {
    byte[] buffer = new byte[8192];
    long bytesRemaining = fileSize;
    int bytesRead;
    while (bytesRemaining > 0 && (bytesRead = dataIn.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) != -1) {
        fos.write(buffer, 0, bytesRead);
        bytesRemaining -= bytesRead;
    }
}
```

#### 4.2.4 用户界面
系统提供两种用户界面：

1. **命令行界面 (CLI)**：使用控制台输入输出，适合脚本和远程操作。
2. **图形用户界面 (GUI)**：使用Swing实现，提供可视化的文件管理体验。

## 5. 性能优化

### 5.1 并发处理
- 使用线程池管理客户端连接，避免线程爆炸
- 每个客户端连接使用独立的线程处理，确保响应性

### 5.2 文件传输优化
- 使用缓冲区（8KB）进行文件传输，平衡内存使用和传输效率
- 大文件分块传输，避免内存溢出

## 6. 安全性考虑

### 6.1 路径遍历防护
系统规范化所有路径请求，防止路径遍历攻击。客户端无法访问服务器根目录之外的文件。

### 6.2 错误处理
系统对所有可能的异常进行捕获和处理，确保系统稳定性和安全性。

## 7. 扩展性

系统设计考虑了未来可能的扩展：

- 支持用户认证和授权
- 文件加密传输
- 文件版本控制
- 文件同步功能
- 用户组和共享功能

## 8. 系统要求

### 8.1 服务器要求
- Java 8 或更高版本
- 至少 512MB 内存
- 足够的磁盘空间用于存储文件

### 8.2 客户端要求
- Java 8 或更高版本
- 图形界面需要支持Swing
- 至少 256MB 内存

## 9. 部署指南

### 9.1 服务器部署
1. 编译服务器代码：`javac server/*.java`
2. 创建文件存储目录：`mkdir files`
3. 启动服务器：`java server.FileServer [port] [rootDirectory]`

### 9.2 客户端部署
1. 编译客户端代码：`javac client/*.java`
2. 启动命令行客户端：`java client.FileClient [serverAddress] [serverPort]`
3. 启动图形界面客户端：`java client.GUI [serverAddress] [serverPort]`

## 10. 使用示例

### 10.1 命令行客户端
```
$ java client.FileClient localhost 8888
已连接到服务器: localhost:8888
/> help
可用命令:
  help              - 显示此帮助信息
  exit              - 退出程序
  ls [path]         - 列出目录内容
  cd <path>         - 更改当前目录
  download <remote> <local> - 下载文件
  upload <local> <remote>   - 上传文件
  delete <path>     - 删除文件或目录
  mkdir <path>      - 创建新目录
  rename <old> <new> - 重命名文件或目录
/> ls
目录内容:
[目录] documents
[文件] example.txt
/> cd documents/
/documents/> mkdir reports
目录创建成功: /documents/reports
/documents/> exit
再见！
```

### 10.2 图形界面客户端
图形界面提供直观的文件管理体验，包括：
- 文件和目录浏览
- 文件上传和下载
- 文件和目录删除
- 目录创建
- 文件和目录重命名

## 11. 限制与已知问题

- 不支持文件和目录权限控制
- 不支持文件内容预览
- 不支持多文件同时上传/下载
- 不支持文件搜索功能
- 不支持断点续传

## 12. 未来改进计划

- 添加用户认证和授权系统
- 实现文件传输加密
- 添加文件搜索功能
- 支持断点续传
- 添加文件预览功能
- 实现文件同步功能
- 添加日志记录和监控功能 