package org.hyj;

import jnr.ffi.Platform;
import org.hyj.fuse.NacosFuseFileSystem;
import org.hyj.service.NacosService;
import ru.serce.jnrfuse.FuseFS;

import java.nio.file.Paths;

/**
 * Nacos FUSE挂载程序主入口
 * 将Nacos配置以文件系统形式挂载到本地目录
 */
public class Main {
    public static void main(String[] args) {
        // 默认配置
        String serverAddr = "10.9.2.85:8848";

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
            serverAddr = args[0];
        }
        if (args.length >= 2) {
            mountPoint = args[1];
        }
        
        System.out.println("========================================");
        System.out.println("Nacos FUSE File System");
        System.out.println("========================================");
        System.out.println("Nacos Server: " + serverAddr);
        System.out.println("Mount Point: " + mountPoint);
        System.out.println("========================================");
        System.out.println();
        System.out.println("Directory Structure:");
        System.out.println("  /namespaces/");
        System.out.println("    /{namespace}/");
        System.out.println("      /{group}/");
        System.out.println("        {config_file}");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  /namespaces/phoenix-test/DEFAULT_GROUP/application.properties");
        System.out.println();
        System.out.println("Starting FUSE filesystem...");
        System.out.println("Press Ctrl+C to unmount and exit.");
        System.out.println();
        
        try {
            // 创建Nacos服务客户端
            NacosService nacosService = new NacosService(serverAddr);
            
            // 创建FUSE文件系统
            FuseFS fuseFS = new NacosFuseFileSystem(nacosService);
//            fuseFS = new MemoryFS();
            /*
removed ‘Directory with files/hello.txt’
removed ‘Directory with files/hello again.txt’
removed ‘Directory with files/Sample nested directory/So deep.txt’
removed directory: ‘Directory with files/Sample nested directory’
removed directory: ‘Directory with files’
removed directory: ‘Sample directory’
removed ‘Sample file.txt’
             */
//            fuseFS.unlink("/Directory with files/hello.txt");
//            fuseFS.unlink("/Directory with files/hello again.txt");
//            fuseFS.unlink("/Directory with files/Sample nested directory/So deep.txt");
//            fuseFS.rmdir("/Directory with files/Sample nested directory");
//            fuseFS.rmdir("/Directory with files");
//            fuseFS.rmdir("/Sample directory");
//            fuseFS.unlink("/Sample file.txt");


            // 挂载文件系统（阻塞调用）
            fuseFS.mount(Paths.get(mountPoint), true, true);
            
        } catch (Exception e) {
            System.err.println("Failed to mount FUSE filesystem:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}