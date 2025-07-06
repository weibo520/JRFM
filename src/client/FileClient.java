package client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件客户端主类
 * 负责与服务器建立连接并发送请求
 */
public class FileClient {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    
    /**
     * 构造函数
     * @param serverAddress 服务器地址
     * @param serverPort 服务器端口
     */
    public FileClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
    
    /**
     * 连接到服务器
     * @return 是否连接成功
     */
    public boolean connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            socket.setSoTimeout(30000); // 设置30秒超时
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());
            return true;
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查连接状态并尝试重连
     * @return 连接是否有效
     */
    public boolean ensureConnected() {
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            System.out.println("连接已断开，尝试重新连接...");
            return connect();
        }
        return true;
    }
    
    /**
     * 断开与服务器的连接
     */
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
    
    /**
     * 发送命令并获取响应
     * @param command 命令
     * @return 响应
     */
    private String[] sendCommand(String command) throws IOException {
        if (!ensureConnected()) {
            throw new IOException("无法连接到服务器");
        }
        
        try {
            out.println(command);
            String response = in.readLine();
            if (response == null) {
                throw new IOException("服务器连接已关闭");
            }
            return response.split("\\|", 3);
        } catch (IOException e) {
            System.err.println("发送命令时出错: " + e.getMessage());
            disconnect();
            throw e;
        }
    }
    
    /**
     * 列出目录内容
     * @param path 目录路径
     * @return 文件和目录列表
     */
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
    
    /**
     * 下载文件
     * @param remotePath 远程文件路径
     * @param localPath 本地保存路径
     * @return 是否下载成功
     */
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
    
    /**
     * 上传文件
     * @param localPath 本地文件路径
     * @param remotePath 远程保存路径
     * @return 是否上传成功
     */
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
            String line = in.readLine();
            response = line.split("\\|", 3);
            return Integer.parseInt(response[0]) == 200;
        } else {
            System.err.println("上传文件失败: " + response[1]);
            return false;
        }
    }
    
    /**
     * 删除文件或目录
     * @param path 要删除的路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String path) throws IOException {
        String[] response = sendCommand("DELETE|" + path);
        
        if (Integer.parseInt(response[0]) == 200) {
            return true;
        } else {
            System.err.println("删除失败: " + response[1]);
            return false;
        }
    }
    
    /**
     * 创建目录
     * @param path 目录路径
     * @return 是否创建成功
     */
    public boolean createDirectory(String path) throws IOException {
        String[] response = sendCommand("MKDIR|" + path);
        
        if (Integer.parseInt(response[0]) == 200) {
            return true;
        } else {
            System.err.println("创建目录失败: " + response[1]);
            return false;
        }
    }
    
    /**
     * 重命名文件或目录
     * @param oldPath 原路径
     * @param newPath 新路径
     * @return 是否重命名成功
     */
    public boolean renameFile(String oldPath, String newPath) throws IOException {
        String[] response = sendCommand("RENAME|" + oldPath + "|" + newPath);
        
        if (Integer.parseInt(response[0]) == 200) {
            return true;
        } else {
            System.err.println("重命名失败: " + response[1]);
            return false;
        }
    }
    
    /**
     * 文件项类，表示一个文件或目录
     */
    public static class FileItem {
        private String name;
        private boolean directory;
        
        public FileItem(String name, boolean directory) {
            this.name = name;
            this.directory = directory;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isDirectory() {
            return directory;
        }
        
        @Override
        public String toString() {
            return (directory ? "[目录] " : "[文件] ") + name;
        }
    }
    
    /**
     * 主方法，用于测试客户端功能
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
}