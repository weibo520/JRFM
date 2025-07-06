# Java远程文件管理系统 - 代码示例

本文档展示了Java远程文件管理系统的关键功能实现代码示例，帮助理解系统的核心工作原理。

## 1. 服务器端核心功能

### 1.1 服务器启动与客户端连接管理

```java
// FileServer.java - 服务器启动与连接管理
public class FileServer {
    private int port;
    private String rootDirectory;
    private boolean running;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    
    public FileServer(int port, String rootDirectory) {
        this.port = port;
        this.rootDirectory = rootDirectory;
        this.threadPool = Executors.newFixedThreadPool(10); // 创建固定大小的线程池
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("文件服务器启动成功，监听端口: " + port);
            System.out.println("根目录设置为: " + rootDirectory);
            
            // 循环接受客户端连接
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("接收到新的客户端连接: " + clientSocket.getInetAddress().getHostAddress());
                    
                    // 创建客户端处理线程并提交到线程池
                    ClientHandler clientHandler = new ClientHandler(clientSocket, rootDirectory);
                    threadPool.submit(clientHandler);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("接受客户端连接时出错: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    // 其他方法...
}
```

### 1.2 命令处理机制

```java
// ClientHandler.java - 命令处理
private void processCommand(String commandLine) {
    String[] parts = commandLine.split("\\|");
    if (parts.length == 0) {
        sendResponse(400, "无效命令", "");
        return;
    }
    
    String command = parts[0].toUpperCase();
    
    try {
        switch (command) {
            case "LIST":
                if (parts.length < 2) {
                    sendResponse(400, "缺少参数", "");
                } else {
                    handleListCommand(parts[1]);
                }
                break;
                
            case "DOWNLOAD":
                if (parts.length < 2) {
                    sendResponse(400, "缺少参数", "");
                } else {
                    handleDownloadCommand(parts[1]);
                }
                break;
                
            // 其他命令处理...
                
            default:
                sendResponse(400, "未知命令", "");
                break;
        }
    } catch (Exception e) {
        sendResponse(500, "服务器错误: " + e.getMessage(), "");
        System.err.println("处理命令时出错: " + e.getMessage());
        e.printStackTrace();
    }
}
```

### 1.3 文件列表功能

```java
// ClientHandler.java - 列出目录内容
private void handleListCommand(String path) throws IOException {
    Path targetPath = getAbsolutePath(path);
    
    // 检查路径是否存在且是目录
    if (!Files.exists(targetPath)) {
        sendResponse(400, "路径不存在", "");
        return;
    }
    
    if (!Files.isDirectory(targetPath)) {
        sendResponse(400, "路径不是目录", "");
        return;
    }
    
    // 获取目录列表
    String fileList = Files.list(targetPath)
            .map(p -> {
                String name = p.getFileName().toString();
                return Files.isDirectory(p) ? name + "/DIR" : name + "/FILE";
            })
            .collect(Collectors.joining(","));
    
    sendResponse(200, "成功", fileList);
}
```

### 1.4 文件下载功能

```java
// ClientHandler.java - 文件下载
private void handleDownloadCommand(String path) throws IOException {
    Path filePath = getAbsolutePath(path);
    
    // 检查文件是否存在且不是目录
    if (!Files.exists(filePath)) {
        sendResponse(400, "文件不存在", "");
        return;
    }
    
    if (Files.isDirectory(filePath)) {
        sendResponse(400, "无法下载目录", "");
        return;
    }
    
    // 获取文件大小
    long fileSize = Files.size(filePath);
    sendResponse(200, "成功", String.valueOf(fileSize));
    
    // 发送文件内容
    try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            dataOut.write(buffer, 0, bytesRead);
        }
        dataOut.flush();
    }
}
```

### 1.5 文件上传功能

```java
// ClientHandler.java - 文件上传
private void handleUploadCommand(String path, long fileSize) throws IOException {
    Path filePath = getAbsolutePath(path);
    
    // 检查父目录是否存在
    Path parentPath = filePath.getParent();
    if (parentPath != null && !Files.exists(parentPath)) {
        Files.createDirectories(parentPath);
    }
    
    // 告诉客户端准备接收文件
    sendResponse(200, "准备接收文件", "");
    
    // 接收并保存文件
    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
        byte[] buffer = new byte[8192];
        long bytesRemaining = fileSize;
        int bytesRead;
        
        while (bytesRemaining > 0 && (bytesRead = dataIn.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) != -1) {
            fos.write(buffer, 0, bytesRead);
            bytesRemaining -= bytesRead;
        }
    }
    
    sendResponse(200, "上传完成", "");
}
```

### 1.6 文件删除功能

```java
// ClientHandler.java - 文件删除
private void handleDeleteCommand(String path) throws IOException {
    Path targetPath = getAbsolutePath(path);
    
    // 检查路径是否存在
    if (!Files.exists(targetPath)) {
        sendResponse(400, "路径不存在", "");
        return;
    }
    
    // 删除文件或目录
    boolean success = deleteRecursively(targetPath);
    
    if (success) {
        sendResponse(200, "删除成功", "");
    } else {
        sendResponse(500, "删除失败", "");
    }
}

private boolean deleteRecursively(Path path) throws IOException {
    if (Files.isDirectory(path)) {
        Files.list(path).forEach(p -> {
            try {
                deleteRecursively(p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    Files.delete(path);
    return true;
}
```

### 1.7 路径安全处理

```java
// ClientHandler.java - 安全路径处理
private Path getAbsolutePath(String relativePath) {
    // 规范化路径，防止路径遍历攻击
    String normalizedPath = relativePath.replace("\\", "/");
    while (normalizedPath.startsWith("/")) {
        normalizedPath = normalizedPath.substring(1);
    }
    
    return Paths.get(rootDirectory, normalizedPath);
}
```

## 2. 客户端核心功能

### 2.1 客户端连接管理

```java
// FileClient.java - 连接管理
public boolean connect() {
    try {
        socket = new Socket(serverAddress, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        dataIn = new DataInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());
        return true;
    } catch (IOException e) {
        System.err.println("连接服务器失败: " + e.getMessage());
        return false;
    }
}

public void disconnect() {
    try {
        if (in != null) in.close();
        if (out != null) out.close();
        if (dataIn != null) dataIn.close();
        if (dataOut != null) dataOut.close();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    } catch (IOException e) {
        System.err.println("断开连接时出错: " + e.getMessage());
    }
}
```

### 2.2 命令发送与响应处理

```java
// FileClient.java - 命令发送与响应处理
private String[] sendCommand(String command) throws IOException {
    out.println(command);
    String response = in.readLine();
    return response.split("\\|", 3);
}
```

### 2.3 文件列表获取

```java
// FileClient.java - 获取文件列表
public List<FileItem> listFiles(String path) throws IOException {
    String[] response = sendCommand("LIST|" + path);
    List<FileItem> fileList = new ArrayList<>();
    
    if (Integer.parseInt(response[0]) == 200) {
        if (response.length >= 3 && !response[2].isEmpty()) {
            String[] items = response[2].split(",");
            for (String item : items) {
                String[] parts = item.split("/");
                if (parts.length == 2) {
                    String name = parts[0];
                    boolean isDirectory = "DIR".equals(parts[1]);
                    fileList.add(new FileItem(name, isDirectory));
                }
            }
        }
    } else {
        System.err.println("列出目录失败: " + response[1]);
    }
    
    return fileList;
}
```

### 2.4 文件下载功能

```java
// FileClient.java - 文件下载
public boolean downloadFile(String remotePath, String localPath) throws IOException {
    String[] response = sendCommand("DOWNLOAD|" + remotePath);
    
    if (Integer.parseInt(response[0]) == 200) {
        long fileSize = Long.parseLong(response[2]);
        
        // 创建本地文件
        Path localFilePath = Paths.get(localPath);
        Path parentDir = localFilePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        
        // 接收文件内容
        try (FileOutputStream fos = new FileOutputStream(localFilePath.toFile())) {
            byte[] buffer = new byte[8192];
            long bytesRemaining = fileSize;
            int bytesRead;
            
            while (bytesRemaining > 0 && (bytesRead = dataIn.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) != -1) {
                fos.write(buffer, 0, bytesRead);
                bytesRemaining -= bytesRead;
            }
        }
        
        return true;
    } else {
        System.err.println("下载文件失败: " + response[1]);
        return false;
    }
}
```

### 2.5 文件上传功能

```java
// FileClient.java - 文件上传
public boolean uploadFile(String localPath, String remotePath) throws IOException {
    File localFile = new File(localPath);
    
    if (!localFile.exists() || !localFile.isFile()) {
        System.err.println("本地文件不存在或不是文件");
        return false;
    }
    
    long fileSize = localFile.length();
    String[] response = sendCommand("UPLOAD|" + remotePath + "|" + fileSize);
    
    if (Integer.parseInt(response[0]) == 200) {
        // 发送文件内容
        try (FileInputStream fis = new FileInputStream(localFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
            dataOut.flush();
        }
        
        // 等待上传完成响应
        response = new String[]{in.readLine()}.split("\\|", 3);
        return Integer.parseInt(response[0]) == 200;
    } else {
        System.err.println("上传文件失败: " + response[1]);
        return false;
    }
}
```

## 3. GUI界面核心功能

### 3.1 文件列表显示

```java
// GUI.java - 刷新文件列表
private void refreshFileList() {
    try {
        // 清空表格
        tableModel.setRowCount(0);
        
        // 更新地址栏
        addressBar.setText(currentDirectory);
        
        // 获取文件列表
        List<FileClient.FileItem> files = client.listFiles(currentDirectory);
        
        // 添加到表格
        for (FileClient.FileItem item : files) {
            String[] rowData = {
                item.getName(),
                item.isDirectory() ? "目录" : "文件",
                ""
            };
            tableModel.addRow(rowData);
        }
        
        statusBar.setText("目录加载完成: " + files.size() + " 个项目");
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "加载目录失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        statusBar.setText("错误: " + e.getMessage());
    }
}
```

### 3.2 目录导航功能

```java
// GUI.java - 目录导航
private void navigateToDirectory(String path) {
    // 规范化路径
    if (!path.endsWith("/")) {
        path += "/";
    }
    
    try {
        // 尝试列出目录内容以验证目录存在
        client.listFiles(path);
        
        // 更新当前目录
        currentDirectory = path;
        
        // 刷新文件列表
        refreshFileList();
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "无法访问目录: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }
}

private void navigateToParent() {
    if (currentDirectory.equals("/")) {
        return;
    }
    
    // 移除最后的斜杠
    String path = currentDirectory.substring(0, currentDirectory.length() - 1);
    
    // 找到最后一个斜杠
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash >= 0) {
        // 获取父目录路径
        String parentPath = path.substring(0, lastSlash + 1);
        navigateToDirectory(parentPath);
    }
}
```

### 3.3 文件操作事件处理

```java
// GUI.java - 上传文件事件处理
private void uploadFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("选择要上传的文件");
    
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        
        try {
            statusBar.setText("正在上传文件...");
            
            boolean success = client.uploadFile(selectedFile.getAbsolutePath(), currentDirectory + selectedFile.getName());
            
            if (success) {
                statusBar.setText("文件上传成功");
                refreshFileList();
            } else {
                statusBar.setText("文件上传失败");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "上传文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            statusBar.setText("错误: " + e.getMessage());
        }
    }
}
```

## 4. 命令行界面核心功能

### 4.1 命令解析与执行

```java
// CommandLineInterface.java - 命令解析与执行
public void start() {
    System.out.println("远程文件管理系统客户端");
    System.out.println("输入 'help' 获取帮助，输入 'exit' 退出");
    
    boolean running = true;
    while (running) {
        try {
            System.out.print(currentDirectory + "> ");
            String command = consoleReader.readLine().trim();
            
            if (command.isEmpty()) {
                continue;
            }
            
            String[] parts = command.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1] : "";
            
            switch (cmd) {
                case "exit":
                    running = false;
                    break;
                    
                case "help":
                    showHelp();
                    break;
                    
                case "ls":
                    listFiles(args);
                    break;
                    
                // 其他命令处理...
                    
                default:
                    System.out.println("未知命令: " + cmd);
                    System.out.println("输入 'help' 获取帮助");
                    break;
            }
        } catch (IOException e) {
            System.err.println("错误: " + e.getMessage());
        }
    }
    
    System.out.println("再见！");
}
```

### 4.2 文件列表显示

```java
// CommandLineInterface.java - 文件列表显示
private void listFiles(String path) throws IOException {
    if (path.isEmpty()) {
        path = currentDirectory;
    } else if (!path.startsWith("/")) {
        path = combinePath(currentDirectory, path);
    }
    
    List<FileClient.FileItem> files = client.listFiles(path);
    
    if (files.isEmpty()) {
        System.out.println("目录为空");
    } else {
        System.out.println("目录内容:");
        for (FileClient.FileItem item : files) {
            System.out.println(item);
        }
    }
}
```

### 4.3 路径合并功能

```java
// CommandLineInterface.java - 路径合并
private String combinePath(String basePath, String relativePath) {
    if (relativePath.equals("..")) {
        // 返回上一级目录
        int lastSlash = basePath.lastIndexOf('/', basePath.length() - 2);
        if (lastSlash >= 0) {
            return basePath.substring(0, lastSlash + 1);
        } else {
            return "/";
        }
    } else if (relativePath.equals(".")) {
        // 当前目录
        return basePath;
    } else {
        // 普通路径
        return basePath + (basePath.endsWith("/") ? "" : "/") + relativePath;
    }
}
```

## 5. 完整工作流程示例

### 5.1 服务器启动到客户端连接

```java
// 服务器端
public static void main(String[] args) {
    int port = 8888;
    String rootDirectory = "./files";
    
    // 创建并启动服务器
    FileServer server = new FileServer(port, rootDirectory);
    server.start();
}

// 客户端
public static void main(String[] args) {
    String serverAddress = "localhost";
    int serverPort = 8888;
    
    // 创建客户端并连接服务器
    FileClient client = new FileClient(serverAddress, serverPort);
    if (client.connect()) {
        System.out.println("已连接到服务器: " + serverAddress + ":" + serverPort);
        
        // 创建命令行界面
        CommandLineInterface cli = new CommandLineInterface(client);
        cli.start();
        
        // 断开连接
        client.disconnect();
    } else {
        System.err.println("无法连接到服务器");
    }
}
```

### 5.2 完整的文件上传-下载流程

```java
// 上传文件
boolean uploadSuccess = client.uploadFile("local_file.txt", "/remote_file.txt");
if (uploadSuccess) {
    System.out.println("文件上传成功");
    
    // 验证文件是否存在
    List<FileClient.FileItem> files = client.listFiles("/");
    boolean fileExists = files.stream()
            .anyMatch(item -> "remote_file.txt".equals(item.getName()) && !item.isDirectory());
    
    if (fileExists) {
        System.out.println("文件验证成功");
        
        // 下载文件
        boolean downloadSuccess = client.downloadFile("/remote_file.txt", "downloaded_file.txt");
        if (downloadSuccess) {
            System.out.println("文件下载成功");
        }
    }
}
```

## 6. 错误处理示例

### 6.1 连接错误处理

```java
try {
    socket = new Socket(serverAddress, serverPort);
    // 初始化流...
    return true;
} catch (ConnectException e) {
    System.err.println("无法连接到服务器: " + e.getMessage());
    System.err.println("请检查服务器是否已启动，以及地址和端口是否正确。");
    return false;
} catch (UnknownHostException e) {
    System.err.println("未知主机: " + e.getMessage());
    System.err.println("请检查服务器地址是否正确。");
    return false;
} catch (IOException e) {
    System.err.println("连接服务器失败: " + e.getMessage());
    return false;
}
```

### 6.2 文件操作错误处理

```java
try {
    // 尝试删除文件
    boolean success = client.deleteFile(remotePath);
    if (success) {
        System.out.println("文件删除成功");
    }
} catch (FileNotFoundException e) {
    System.err.println("文件不存在: " + e.getMessage());
} catch (AccessDeniedException e) {
    System.err.println("访问被拒绝: " + e.getMessage());
    System.err.println("请检查文件权限。");
} catch (IOException e) {
    System.err.println("删除文件失败: " + e.getMessage());
}
```

## 7. 性能优化示例

### 7.1 缓冲区优化

```java
// 优化前
byte[] buffer = new byte[1024];

// 优化后 - 使用更大的缓冲区
byte[] buffer = new byte[8192]; // 8KB缓冲区
```

### 7.2 线程池优化

```java
// 优化前
new Thread(new ClientHandler(clientSocket, rootDirectory)).start();

// 优化后 - 使用线程池
private ExecutorService threadPool = Executors.newFixedThreadPool(10);
threadPool.submit(new ClientHandler(clientSocket, rootDirectory));
```

## 8. 安全性示例

### 8.1 路径遍历防护

```java
private Path getAbsolutePath(String relativePath) {
    // 规范化路径，防止路径遍历攻击
    String normalizedPath = relativePath.replace("\\", "/");
    
    // 移除开头的斜杠，防止访问根目录之外的文件
    while (normalizedPath.startsWith("/")) {
        normalizedPath = normalizedPath.substring(1);
    }
    
    // 检查是否包含 ".." 路径，防止目录遍历
    if (normalizedPath.contains("..")) {
        throw new SecurityException("不允许使用 '..' 路径");
    }
    
    return Paths.get(rootDirectory, normalizedPath);
}
```

### 8.2 输入验证

```java
private void validatePath(String path) {
    if (path == null || path.isEmpty()) {
        throw new IllegalArgumentException("路径不能为空");
    }
    
    // 检查路径中是否包含非法字符
    if (path.contains("|") || path.contains("*") || path.contains("?")) {
        throw new IllegalArgumentException("路径包含非法字符");
    }
}
``` 