package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * GUI class for file management
 * Provides Swing-based graphical file management interface
 */
public class GUI extends JFrame {
    private FileClient client;
    private String currentDirectory = "/";
    
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextField addressBar;
    private JButton backButton;
    private JButton refreshButton;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton deleteButton;
    private JButton mkdirButton;
    private JButton renameButton;
    private JLabel statusBar;
    
    /**
     * Constructor
     * @param client File client
     */
    public GUI(FileClient client) {
        this.client = client;
        
        // Initialize window properties
        setTitle("Java远程文件管理系统");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create interface components
        createComponents();
        
        // Refresh file list
        refreshFileList();
    }
    
    /**
     * Create interface components
     */
    private void createComponents() {
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Toolbar
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        
        // Address bar
        JPanel addressPanel = new JPanel(new BorderLayout());
        addressBar = new JTextField(currentDirectory);
        addressBar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateToDirectory(addressBar.getText());
            }
        });
        
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backButton = new JButton("返回");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateToParent();
            }
        });
        
        refreshButton = new JButton("刷新");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshFileList();
            }
        });
        
        navigationPanel.add(backButton);
        navigationPanel.add(refreshButton);
        
        addressPanel.add(new JLabel("路径: "), BorderLayout.WEST);
        addressPanel.add(addressBar, BorderLayout.CENTER);
        addressPanel.add(navigationPanel, BorderLayout.EAST);
        
        // Operation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        uploadButton = new JButton("上传文件");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadFile();
            }
        });
        
        downloadButton = new JButton("下载文件");
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadFile();
            }
        });
        
        deleteButton = new JButton("删除");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteFile();
            }
        });
        
        mkdirButton = new JButton("新建文件夹");
        mkdirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createDirectory();
            }
        });
        
        renameButton = new JButton("重命名");
        renameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renameFile();
            }
        });
        
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(mkdirButton);
        buttonPanel.add(renameButton);
        
        toolbarPanel.add(addressPanel, BorderLayout.NORTH);
        toolbarPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // File list table
        String[] columnNames = {"名称", "类型", "大小"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = fileTable.getSelectedRow();
                    if (selectedRow != -1) {
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        String fileType = (String) tableModel.getValueAt(selectedRow, 1);
                        
                        if ("目录".equals(fileType)) {
                            navigateToDirectory(combinePath(currentDirectory, fileName));
                        } else {
                            previewFile(combinePath(currentDirectory, fileName));
                        }
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(fileTable);
        
        // Status bar
        statusBar = new JLabel("就绪");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        
        // Assemble interface
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * Refresh file list
     */
    private void refreshFileList() {
        try {
            updateStatus("正在加载文件列表...");
            List<FileClient.FileItem> files = client.listFiles(currentDirectory);
            
            // Clear table
            tableModel.setRowCount(0);
            
            // Add file items
            for (FileClient.FileItem item : files) {
                Object[] row = {
                    item.getName(),
                    item.isDirectory() ? "目录" : "文件",
                    item.isDirectory() ? "" : "未知"
                };
                tableModel.addRow(row);
            }
            
            // Update address bar
            addressBar.setText(currentDirectory);
            updateStatus("文件列表已更新");
            
        } catch (IOException e) {
            showError("刷新文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * Navigate to specified directory
     * @param path Directory path
     */
    private void navigateToDirectory(String path) {
        try {
            // Normalize path
            if (!path.startsWith("/")) {
                path = combinePath(currentDirectory, path);
            }
            if (!path.endsWith("/")) {
                path += "/";
            }
            
            // Verify directory exists
            client.listFiles(path);
            currentDirectory = path;
            refreshFileList();
            
        } catch (IOException e) {
            showError("无法访问目录: " + e.getMessage());
        }
    }
    
    /**
     * Navigate to parent directory
     */
    private void navigateToParent() {
        if (!"/".equals(currentDirectory)) {
            String parentPath = currentDirectory.substring(0, currentDirectory.lastIndexOf('/', currentDirectory.length() - 2) + 1);
            navigateToDirectory(parentPath);
        }
    }
    
    /**
     * Upload file
     */
    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String remotePath = combinePath(currentDirectory, selectedFile.getName());
            
            try {
                updateStatus("正在上传文件...");
                boolean success = client.uploadFile(selectedFile.getAbsolutePath(), remotePath);
                
                if (success) {
                    updateStatus("文件上传成功");
                    refreshFileList();
                } else {
                    showError("文件上传失败");
                }
            } catch (IOException e) {
                showError("上传文件时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * Download file
     */
    private void downloadFile() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("请选择要下载的文件");
            return;
        }
        
        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        String fileType = (String) tableModel.getValueAt(selectedRow, 1);
        
        if ("目录".equals(fileType)) {
            showError("无法下载目录");
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            String remotePath = combinePath(currentDirectory, fileName);
            
            try {
                updateStatus("正在下载文件...");
                boolean success = client.downloadFile(remotePath, saveFile.getAbsolutePath());
                
                if (success) {
                    updateStatus("文件下载成功");
                } else {
                    showError("文件下载失败");
                }
            } catch (IOException e) {
                showError("下载文件时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * Delete file or directory
     */
    private void deleteFile() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("请选择要删除的文件或目录");
            return;
        }
        
        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        
        int result = JOptionPane.showConfirmDialog(
            this,
            "确定要删除 '" + fileName + "' 吗?",
            "确认删除",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                String filePath = combinePath(currentDirectory, fileName);
                boolean success = client.deleteFile(filePath);
                
                if (success) {
                    updateStatus("删除成功");
                    refreshFileList();
                } else {
                    showError("删除失败");
                }
            } catch (IOException e) {
                showError("删除时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * Create new directory
     */
    private void createDirectory() {
        String dirName = JOptionPane.showInputDialog(this, "输入目录名称:", "新建目录", JOptionPane.PLAIN_MESSAGE);
        
        if (dirName != null && !dirName.trim().isEmpty()) {
            try {
                String dirPath = combinePath(currentDirectory, dirName.trim());
                boolean success = client.createDirectory(dirPath);
                
                if (success) {
                    updateStatus("目录创建成功");
                    refreshFileList();
                } else {
                    showError("目录创建失败");
                }
            } catch (IOException e) {
                showError("创建目录时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * Rename file or directory
     */
    private void renameFile() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("请选择要重命名的文件或目录");
            return;
        }
        
        String oldName = (String) tableModel.getValueAt(selectedRow, 0);
        String newName = JOptionPane.showInputDialog(this, "输入新名称:", oldName);
        
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
            try {
                String oldPath = combinePath(currentDirectory, oldName);
                String newPath = combinePath(currentDirectory, newName.trim());
                boolean success = client.renameFile(oldPath, newPath);
                
                if (success) {
                    updateStatus("重命名成功");
                    refreshFileList();
                } else {
                    showError("重命名失败");
                }
            } catch (IOException e) {
                showError("重命名时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * Combine paths
     * @param parent Parent path
     * @param child Child path
     * @return Combined path
     */
    private String combinePath(String parent, String child) {
        if (parent.endsWith("/")) {
            return parent + child;
        } else {
            return parent + "/" + child;
        }
    }
    
    /**
     * Update status bar
     * @param message Status message
     */
    private void updateStatus(String message) {
        statusBar.setText(message);
    }
    
    /**
     * Show error message
     * @param message Error message
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
        updateStatus("就绪");
    }
    
    /**
     * 预览文件内容
     * @param path 文件路径
     */
    private void previewFile(String path) {
        try {
            updateStatus("正在加载文件内容...");
            
            // 创建临时文件用于预览
            File tempFile = File.createTempFile("preview_", ".tmp");
            tempFile.deleteOnExit();
            
            // 下载文件到临时位置
            boolean success = client.downloadFile(path, tempFile.getAbsolutePath());
            
            if (success) {
                // 读取文件内容
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                // 创建文本区域显示内容
                JTextArea textArea = new JTextArea(content.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                
                // 创建滚动面板
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 400));
                
                // 显示文件内容对话框
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "文件内容: " + fileName,
                    JOptionPane.PLAIN_MESSAGE
                );
                
                updateStatus("文件内容已加载");
            } else {
                showError("无法加载文件内容");
            }
            
            // 删除临时文件
            tempFile.delete();
            
        } catch (IOException e) {
            showError("预览文件时出错: " + e.getMessage());
        }
    }
    
    /**
     * Main method to start GUI client
     */
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 8888;
        
        // Parse command line arguments
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
        
        // Create client and connect to server
        FileClient client = new FileClient(serverAddress, serverPort);
        if (client.connect()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new GUI(client).setVisible(true);
                }
            });
        } else {
            JOptionPane.showMessageDialog(null, "无法连接到服务器: " + serverAddress + ":" + serverPort, "连接错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}