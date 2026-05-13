package org.hyj;

import jnr.ffi.Platform;
import org.hyj.fuse.NacosFuseFileSystem;
import org.hyj.service.MultiNacosService;
import org.hyj.service.NacosService;
import ru.serce.jnrfuse.FuseFS;

import java.io.File;
import java.nio.file.Paths;

/**
 * Nacos FUSE挂载程序主入口
 * 将Nacos配置以文件系统形式挂载到本地目录
 */
public class Main {
    public static void main(String[] args) {
        // 默认配置
        String configFilePath = "config.yaml";
        String mountPoint;
        
        switch (Platform.getNativePlatform().getOS()) {
            case WINDOWS:
                mountPoint = "N:\\";
                break;
            default:
                mountPoint = "/tmp/nacos_mnt";
        }

        // 解析命令行参数
        if (args.length >= 1) {
            configFilePath = args[0];
        }
        if (args.length >= 2) {
            mountPoint = args[1];
        }
        
        System.out.println("========================================");
        System.out.println("Nacos FUSE File System (Multi-Server)");
        System.out.println("========================================");
        System.out.println("Config File: " + configFilePath);
        System.out.println("Mount Point: " + mountPoint);
        System.out.println("========================================");
        System.out.println();
        System.out.println("Directory Structure:");
        System.out.println("  /{server-name}/");
        System.out.println("    /{namespace}/");
        System.out.println("      /{group}/");
        System.out.println("        {config_file}");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  /nacos-server1/phoenix-test/DEFAULT_GROUP/application.properties");
        System.out.println("  /nacos-server2/_default/DEFAULT_GROUP/config.yml");
        System.out.println();
        System.out.println("Starting FUSE filesystem...");
        System.out.println("Press Ctrl+C to unmount and exit.");
        System.out.println();
        
        try {
            // 检查配置文件是否存在
            File configFile = new File(configFilePath);
            if (!configFile.exists()) {
                System.err.println("Config file not found: " + configFilePath);
                System.err.println("Please create a YAML configuration file with the following format:");
                System.err.println();
                System.err.println("nacos:");
                System.err.println("  servers:");
                System.err.println("    - name: nacos-server1");
                System.err.println("      url: http://127.0.0.1:8848");
                System.err.println("      namespace-pattern: \"*\"");
                System.err.println("      read-only: false");
                System.err.println("    - name: nacos-server2");
                System.err.println("      url: http://127.0.0.1:8849");
                System.err.println("      namespace-pattern: \"^(test|dev)$\"");
                System.err.println("      read-only: true");
                System.exit(1);
            }
            
            // 创建多Nacos服务管理器
            MultiNacosService multiNacosService = new MultiNacosService(configFilePath);
            
            System.out.println("Loaded " + multiNacosService.getServerNames().size() + " Nacos server(s):");
            for (String serverName : multiNacosService.getServerNames()) {
                boolean readOnly = multiNacosService.isReadOnly(serverName);
                System.out.println("  - " + serverName + " (read-only: " + readOnly + ")");
            }
            System.out.println();
            
            // 创建FUSE文件系统
            FuseFS fuseFS = new NacosFuseFileSystem(multiNacosService);

            // 挂载文件系统（阻塞调用）
            fuseFS.mount(Paths.get(mountPoint), true, true);
            
        } catch (Exception e) {
            System.err.println("Failed to mount FUSE filesystem:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}