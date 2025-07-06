# Java远程文件管理系统 - 工作流程

本文档描述了Java远程文件管理系统的完整工作流程，包括服务器部署、客户端使用以及常见操作流程。

## 1. 系统部署流程

### 1.1 服务器部署

1. **准备环境**
   - 确保服务器安装了Java 8或更高版本
   - 检查JDK安装：`java -version`

2. **编译服务器代码**
   ```bash
   mkdir -p build
   javac -d build src/server/*.java
   ```

3. **创建文件存储目录**
   ```bash
   mkdir -p files
   ```

4. **启动服务器**
   ```bash
   cd build
   java server.FileServer 8888 ../files
   ```
   服务器将在8888端口启动，并使用`../files`作为根目录。

5. **验证服务器运行**
   - 确认控制台输出：`文件服务器启动成功，监听端口: 8888`
   - 确认根目录设置：`根目录设置为: ../files`

### 1.2 客户端部署

1. **编译客户端代码**
   ```bash
   javac -d build src/client/*.java
   ```

2. **准备客户端环境**
   - 确保客户端计算机安装了Java 8或更高版本
   - 图形界面客户端需要支持Swing

## 2. 使用流程

### 2.1 命令行界面使用流程

1. **启动命令行客户端**
   ```bash
   cd build
   java client.FileClient localhost 8888
   ```

2. **连接到服务器**
   - 客户端将自动连接到指定的服务器地址和端口
   - 连接成功后显示：`已连接到服务器: localhost:8888`

3. **查看帮助信息**
   ```
   /> help
   ```
   系统将显示可用命令列表。

4. **浏览文件系统**
   ```
   /> ls
   /> cd documents/
   /documents/> ls
   ```

5. **创建目录**
   ```
   /documents/> mkdir reports
   ```

6. **上传文件**
   ```
   /documents/> upload /path/to/local/file.txt file.txt
   ```

7. **下载文件**
   ```
   /documents/> download file.txt /path/to/save/file.txt
   ```

8. **删除文件或目录**
   ```
   /documents/> delete file.txt
   ```

9. **重命名文件或目录**
   ```
   /documents/> rename old_name.txt new_name.txt
   ```

10. **退出客户端**
    ```
    /> exit
    ```

### 2.2 图形界面使用流程

1. **启动图形界面客户端**
   ```bash
   cd build
   java client.GUI localhost 8888
   ```

2. **连接到服务器**
   - 客户端将自动连接到指定的服务器地址和端口
   - 连接成功后显示文件管理界面

3. **浏览文件系统**
   - 双击目录进入该目录
   - 点击"返回上级"按钮返回上级目录
   - 在地址栏输入路径并按Enter直接导航到指定目录

4. **创建目录**
   - 点击"新建文件夹"按钮
   - 在弹出的对话框中输入文件夹名称
   - 点击"确定"创建目录

5. **上传文件**
   - 点击"上传文件"按钮
   - 在文件选择对话框中选择要上传的本地文件
   - 文件将上传到当前目录

6. **下载文件**
   - 在文件列表中选择一个文件
   - 点击"下载文件"按钮
   - 在文件保存对话框中选择保存位置
   - 文件将下载到指定位置

7. **删除文件或目录**
   - 在文件列表中选择一个文件或目录
   - 点击"删除"按钮
   - 在确认对话框中点击"是"确认删除

8. **重命名文件或目录**
   - 在文件列表中选择一个文件或目录
   - 点击"重命名"按钮
   - 在弹出的对话框中输入新名称
   - 点击"确定"完成重命名

9. **刷新文件列表**
   - 点击"刷新"按钮更新当前目录的文件列表

## 3. 常见操作场景

### 3.1 文件备份场景

1. **在服务器创建备份目录**
   ```
   /> mkdir backups
   /> cd backups
   ```

2. **上传备份文件**
   ```
   /backups/> upload /path/to/important_document.docx important_document.docx
   ```

3. **验证备份文件**
   ```
   /backups/> ls
   ```

4. **下载备份文件（恢复）**
   ```
   /backups/> download important_document.docx /path/to/restored_document.docx
   ```

### 3.2 文件共享场景

1. **在服务器创建共享目录**
   ```
   /> mkdir shared
   /> cd shared
   ```

2. **上传共享文件**
   ```
   /shared/> upload /path/to/presentation.pptx presentation.pptx
   ```

3. **其他用户连接到服务器**
   - 另一台计算机启动客户端：`java client.FileClient server_address 8888`

4. **访问共享文件**
   ```
   /> cd shared
   /shared/> ls
   /shared/> download presentation.pptx /path/to/save/presentation.pptx
   ```

### 3.3 文件组织场景

1. **创建目录结构**
   ```
   /> mkdir projects
   /> cd projects
   /projects/> mkdir project1
   /projects/> mkdir project2
   ```

2. **在目录间移动文件**
   ```
   /projects/> cd project1
   /projects/project1/> upload /path/to/file.txt file.txt
   /projects/project1/> cd ..
   /projects/> rename project1/file.txt project2/file.txt
   ```

## 4. 错误处理流程

### 4.1 连接错误

1. **服务器未启动**
   - 错误信息：`连接服务器失败: Connection refused`
   - 解决方法：确保服务器已启动，检查服务器地址和端口是否正确

2. **网络问题**
   - 错误信息：`连接服务器失败: Network is unreachable`
   - 解决方法：检查网络连接，确保客户端和服务器在同一网络或可以互相访问

### 4.2 文件操作错误

1. **文件不存在**
   - 错误信息：`下载文件失败: 文件不存在`
   - 解决方法：检查文件路径是否正确，使用`ls`命令确认文件存在

2. **权限问题**
   - 错误信息：`上传文件失败: 权限被拒绝`
   - 解决方法：检查服务器目录权限，确保服务器进程有写入权限

3. **磁盘空间不足**
   - 错误信息：`上传文件失败: 磁盘空间不足`
   - 解决方法：清理服务器磁盘空间或选择更小的文件上传

## 5. 性能优化建议

1. **大文件传输**
   - 对于大文件传输，建议在网络稳定的环境下进行
   - 避免同时传输多个大文件，可能导致服务器内存压力

2. **并发连接**
   - 服务器默认支持10个并发连接
   - 如需支持更多连接，可修改`FileServer.java`中的线程池大小

3. **网络带宽**
   - 在低带宽环境下，建议减小传输文件的大小
   - 可以考虑在客户端压缩文件后再上传

## 6. 安全建议

1. **不要在公共网络暴露服务器**
   - 系统没有实现用户认证，不适合在公共网络使用
   - 建议在受信任的内部网络使用

2. **定期备份服务器文件**
   - 服务器存储的文件应定期备份，防止数据丢失

3. **避免存储敏感信息**
   - 由于系统不支持加密传输，不建议存储敏感信息

## 7. 故障排除

1. **服务器无响应**
   - 检查服务器进程是否仍在运行
   - 尝试重启服务器
   - 检查服务器日志输出

2. **客户端断开连接**
   - 检查网络连接是否稳定
   - 重新启动客户端并连接
   - 确认服务器仍在运行

3. **文件传输中断**
   - 系统不支持断点续传，需要重新传输文件
   - 检查网络连接稳定性
   - 对于大文件，考虑分割后传输 