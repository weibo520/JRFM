package server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文件服务器主类
 * 负责监听客户端连接请求并为每个客户端创建处理线程
 */
public class FileServer {
    private int port;
    private String rootDirectory;
    private boolean running;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    
    /**
     * 构造函数
     * @param port 服务器监听端口
     * @param rootDirectory 服务器文件根目录
     */
    public FileServer(int port, String rootDirectory) {
        this.port = port;
        this.rootDirectory = rootDirectory;
        this.threadPool = Executors.newFixedThreadPool(10); // 创建固定大小的线程池
    }
    
    /**
     * 启动服务器
     */
    public void start() {
        try {
            // 检查并创建根目录
            File rootDir = new File(rootDirectory);
            if (!rootDir.exists()) {
                if (rootDir.mkdirs()) {
                    System.out.println("创建根目录: " + rootDirectory);
                } else {
                    System.err.println("无法创建根目录: " + rootDirectory);
                    return;
                }
            }
            
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("文件服务器启动成功，监听端口: " + port);
            System.out.println("根目录设置为: " + rootDirectory);
            System.out.println("服务器正在等待客户端连接...");
            
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
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("服务器运行时发生未知错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }
    
    /**
     * 关闭服务器
     */
    public void shutdown() {
        running = false;
        
        // 关闭线程池
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }
        
        // 关闭服务器套接字
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("服务器已关闭");
            } catch (IOException e) {
                System.err.println("关闭服务器套接字时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 主方法，启动文件服务器
     */
    public static void main(String[] args) {
        int port = 8888; // 默认端口
        String rootDirectory = "./files"; // 默认根目录
        
        // 解析命令行参数
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("端口号格式错误，使用默认端口: " + port);
            }
        }
        
        if (args.length >= 2) {
            rootDirectory = args[1];
        }
        
        // 创建并启动服务器
        FileServer server = new FileServer(port, rootDirectory);
        server.start();
    }
}