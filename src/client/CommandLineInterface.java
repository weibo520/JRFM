package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 命令行界面类
 * 提供与用户交互的命令行界面
 */
public class CommandLineInterface {
    private FileClient client;
    private BufferedReader consoleReader;
    private String currentDirectory = "/";
    
    /**
     * 构造函数
     * @param client 文件客户端
     */
    public CommandLineInterface(FileClient client) {
        this.client = client;
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }
    
    /**
     * 启动命令行界面
     */
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
                        
                    case "cd":
                        changeDirectory(args);
                        break;
                        
                    case "download":
                        downloadFile(args);
                        break;
                        
                    case "upload":
                        uploadFile(args);
                        break;
                        
                    case "delete":
                        deleteFile(args);
                        break;
                        
                    case "mkdir":
                        createDirectory(args);
                        break;
                        
                    case "rename":
                        renameFile(args);
                        break;
                        
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
    
    /**
     * 显示帮助信息
     */
    private void showHelp() {
        System.out.println("可用命令:");
        System.out.println("  help              - 显示此帮助信息");
        System.out.println("  exit              - 退出程序");
        System.out.println("  ls [path]         - 列出目录内容");
        System.out.println("  cd <path>         - 更改当前目录");
        System.out.println("  download <remote> <local> - 下载文件");
        System.out.println("  upload <local> <remote>   - 上传文件");
        System.out.println("  delete <path>     - 删除文件或目录");
        System.out.println("  mkdir <path>      - 创建新目录");
        System.out.println("  rename <old> <new> - 重命名文件或目录");
    }
    
    /**
     * 列出目录内容
     */
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
    
    /**
     * 更改当前目录
     */
    private void changeDirectory(String path) throws IOException {
        if (path.isEmpty()) {
            System.out.println("请指定目录路径");
            return;
        }
        
        String newPath;
        if (path.startsWith("/")) {
            newPath = path;
        } else {
            newPath = combinePath(currentDirectory, path);
        }
        
        // 规范化路径
        if (!newPath.endsWith("/")) {
            newPath += "/";
        }
        
        // 验证目录是否存在
        List<FileClient.FileItem> files = client.listFiles(newPath);
        currentDirectory = newPath;
        System.out.println("当前目录: " + currentDirectory);
    }
    
    /**
     * 下载文件
     */
    private void downloadFile(String args) throws IOException {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("用法: download <远程文件路径> <本地保存路径>");
            return;
        }
        
        String remotePath = parts[0];
        String localPath = parts[1];
        
        if (!remotePath.startsWith("/")) {
            remotePath = combinePath(currentDirectory, remotePath);
        }
        
        System.out.println("下载中...");
        boolean success = client.downloadFile(remotePath, localPath);
        if (success) {
            System.out.println("文件下载成功: " + localPath);
        }
    }
    
    /**
     * 上传文件
     */
    private void uploadFile(String args) throws IOException {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("用法: upload <本地文件路径> <远程保存路径>");
            return;
        }
        
        String localPath = parts[0];
        String remotePath = parts[1];
        
        if (!remotePath.startsWith("/")) {
            remotePath = combinePath(currentDirectory, remotePath);
        }
        
        System.out.println("上传中...");
        boolean success = client.uploadFile(localPath, remotePath);
        if (success) {
            System.out.println("文件上传成功: " + remotePath);
        }
    }
    
    /**
     * 删除文件或目录
     */
    private void deleteFile(String path) throws IOException {
        if (path.isEmpty()) {
            System.out.println("请指定要删除的路径");
            return;
        }
        
        if (!path.startsWith("/")) {
            path = combinePath(currentDirectory, path);
        }
        
        System.out.print("确认删除 " + path + "? (y/n): ");
        String confirm = consoleReader.readLine().trim().toLowerCase();
        if (confirm.equals("y") || confirm.equals("yes")) {
            boolean success = client.deleteFile(path);
            if (success) {
                System.out.println("删除成功: " + path);
            }
        } else {
            System.out.println("操作已取消");
        }
    }
    
    /**
     * 创建目录
     */
    private void createDirectory(String path) throws IOException {
        if (path.isEmpty()) {
            System.out.println("请指定要创建的目录路径");
            return;
        }
        
        if (!path.startsWith("/")) {
            path = combinePath(currentDirectory, path);
        }
        
        boolean success = client.createDirectory(path);
        if (success) {
            System.out.println("目录创建成功: " + path);
        }
    }
    
    /**
     * 重命名文件或目录
     */
    private void renameFile(String args) throws IOException {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("用法: rename <原路径> <新路径>");
            return;
        }
        
        String oldPath = parts[0];
        String newPath = parts[1];
        
        if (!oldPath.startsWith("/")) {
            oldPath = combinePath(currentDirectory, oldPath);
        }
        
        if (!newPath.startsWith("/")) {
            newPath = combinePath(currentDirectory, newPath);
        }
        
        boolean success = client.renameFile(oldPath, newPath);
        if (success) {
            System.out.println("重命名成功: " + oldPath + " -> " + newPath);
        }
    }
    
    /**
     * 合并路径
     */
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

    /**
     * 主方法，启动命令行客户端
     */
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 8888;
        
        // 解析命令行参数
        if (args.length >= 1) {
            serverAddress = args[0];
        }
        
        if (args.length >= 2) {
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("端口号格式错误，使用默认端口: " + serverPort);
            }
        }
        
        System.out.println("连接到服务器: " + serverAddress + ":" + serverPort);
        
        // 创建客户端并连接
        FileClient client = new FileClient(serverAddress, serverPort);
        if (client.connect()) {
            System.out.println("已连接到服务器");
            
            // 创建并启动命令行界面
            CommandLineInterface cli = new CommandLineInterface(client);
            cli.start();
            
            // 断开连接
            client.disconnect();
        } else {
            System.err.println("无法连接到服务器");
        }
    }
} 