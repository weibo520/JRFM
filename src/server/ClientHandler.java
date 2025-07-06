package server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * 客户端处理类
 * 负责处理单个客户端的请求
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String rootDirectory;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    
    /**
     * 构造函数
     * @param clientSocket 客户端套接字
     * @param rootDirectory 根目录
     */
    public ClientHandler(Socket clientSocket, String rootDirectory) {
        this.clientSocket = clientSocket;
        this.rootDirectory = rootDirectory;
    }
    
    @Override
    public void run() {
        try {
            // 初始化输入输出流
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
            dataIn = new DataInputStream(clientSocket.getInputStream());
            dataOut = new DataOutputStream(clientSocket.getOutputStream());
            
            // 检查并创建根目录
            Path rootPath = Paths.get(rootDirectory);
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }
            
            // 处理客户端命令
            String command;
            while ((command = in.readLine()) != null) {
                try {
                    processCommand(command);
                } catch (Exception e) {
                    System.err.println("处理命令时出错: " + e.getMessage());
                    e.printStackTrace();
                    sendResponse(500, "处理命令时出错: " + e.getMessage(), "");
                }
            }
        } catch (IOException e) {
            System.err.println("处理客户端请求时出错: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 处理客户端命令
     * @param commandLine 命令行
     */
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
                    
                case "UPLOAD":
                    if (parts.length < 3) {
                        sendResponse(400, "缺少参数", "");
                    } else {
                        handleUploadCommand(parts[1], Long.parseLong(parts[2]));
                    }
                    break;
                    
                case "DELETE":
                    if (parts.length < 2) {
                        sendResponse(400, "缺少参数", "");
                    } else {
                        handleDeleteCommand(parts[1]);
                    }
                    break;
                    
                case "MKDIR":
                    if (parts.length < 2) {
                        sendResponse(400, "缺少参数", "");
                    } else {
                        handleMkdirCommand(parts[1]);
                    }
                    break;
                    
                case "RENAME":
                    if (parts.length < 3) {
                        sendResponse(400, "缺少参数", "");
                    } else {
                        handleRenameCommand(parts[1], parts[2]);
                    }
                    break;
                    
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
    
    /**
     * 处理LIST命令 - 列出目录内容
     */
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
    
    /**
     * 处理DOWNLOAD命令 - 下载文件
     */
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
    
    /**
     * 处理UPLOAD命令 - 上传文件
     */
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
    
    /**
     * 处理DELETE命令 - 删除文件或目录
     */
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
    
    /**
     * 递归删除文件或目录
     */
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
    
    /**
     * 处理MKDIR命令 - 创建目录
     */
    private void handleMkdirCommand(String path) throws IOException {
        Path dirPath = getAbsolutePath(path);
        
        // 检查目录是否已存在
        if (Files.exists(dirPath)) {
            sendResponse(400, "路径已存在", "");
            return;
        }
        
        // 创建目录
        Files.createDirectories(dirPath);
        sendResponse(200, "目录创建成功", "");
    }
    
    /**
     * 处理RENAME命令 - 重命名文件或目录
     */
    private void handleRenameCommand(String oldPath, String newPath) throws IOException {
        Path sourcePath = getAbsolutePath(oldPath);
        Path targetPath = getAbsolutePath(newPath);
        
        // 检查源路径是否存在
        if (!Files.exists(sourcePath)) {
            sendResponse(400, "源路径不存在", "");
            return;
        }
        
        // 检查目标路径是否已存在
        if (Files.exists(targetPath)) {
            sendResponse(400, "目标路径已存在", "");
            return;
        }
        
        // 重命名文件或目录
        Files.move(sourcePath, targetPath);
        sendResponse(200, "重命名成功", "");
    }
    
    /**
     * 获取绝对路径
     */
    private Path getAbsolutePath(String relativePath) {
        // 规范化路径，防止路径遍历攻击
        String normalizedPath = relativePath.replace("\\", "/");
        
        // 移除开头的斜杠
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // 移除结尾的引号和其他不合法字符
        if (normalizedPath.endsWith("\"") || normalizedPath.endsWith("'")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        
        // 过滤掉不合法的字符
        normalizedPath = normalizedPath.replaceAll("[\"':*?<>|]", "");
        
        return Paths.get(rootDirectory, normalizedPath);
    }
    
    /**
     * 发送响应给客户端
     */
    private void sendResponse(int statusCode, String message, String data) {
        out.println(statusCode + "|" + message + "|" + data);
    }
    
    /**
     * 关闭连接
     */
    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("客户端连接已关闭");
        } catch (IOException e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }
} 