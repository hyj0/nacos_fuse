package org.hyj.fuse;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import org.hyj.model.NacosConfig;
import org.hyj.service.MultiNacosService;
import org.hyj.service.NacosService;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.examples.MemoryFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Nacos FUSE文件系统实现
 * 目录结构: /{server-name}/{namespace}/{group}/{config_file}
 */
public class NacosFuseFileSystem extends MemoryFS {
    private final MultiNacosService multiNacosService;
    
    // 缓存配置信息，避免频繁请求
    // Key格式: serverName:namespace
    private final Map<String, List<NacosConfig>> configsCache = new HashMap<>();
    
    public NacosFuseFileSystem(MultiNacosService multiNacosService) {
        this.multiNacosService = multiNacosService;
          /*
removed 'Directory with files/hello.txt'
removed 'Directory with files/hello again.txt'
removed 'Directory with files/Sample nested directory/So deep.txt'
removed directory: 'Directory with files/Sample nested directory'
removed directory: 'Directory with files'
removed directory: 'Sample directory'
removed 'Sample file.txt'
             */
            super.unlink("/Directory with files/hello.txt");
            super.unlink("/Directory with files/hello again.txt");
            super.unlink("/Directory with files/Sample nested directory/So deep.txt");
            super.rmdir("/Directory with files/Sample nested directory");
            super.rmdir("/Directory with files");
            super.rmdir("/Sample directory");
            super.unlink("/Sample file.txt");
            
            // 为每个服务器创建根目录
            for (String serverName : multiNacosService.getServerNames()) {
                super.mkdir("/" + serverName, 0777);
                
                // 获取该服务器的命名空间列表
                List<String> namespaces = multiNacosService.getNamespacesForServer(serverName);
                
                for (String namespace : namespaces) {
                    String displayName = namespace.isEmpty() ? "_default" : namespace;
                    super.mkdir("/" + serverName + "/" + displayName, 0777);
                    
                    // 获取该命名空间下的所有配置
                    List<NacosConfig> configs = getConfigs(serverName, namespace);
                    
                    // 提取所有唯一的分组
                    Set<String> groups = new LinkedHashSet<>();
                    for (NacosConfig config : configs) {
                        groups.add(config.getGroup());
                    }
                    
                    // 为每个分组创建目录和文件
                    for (String group : groups) {
                        super.mkdir("/" + serverName + "/" + displayName + "/" + group, 0777);
                        for (NacosConfig config : configs) {
                            if (config.getGroup().equals(group)) {
                                super.create("/" + serverName + "/" + displayName + "/" + group + "/" + config.getFileName(), 0777, null);
                                Pointer buf = Pointer.wrap(Runtime.getSystemRuntime(), ByteBuffer.wrap(config.getContent().getBytes(StandardCharsets.UTF_8)));
                                super.write("/" + serverName + "/" + displayName + "/" + group + "/" + config.getFileName(), buf, buf.size(), 0, null);
                            }
                        }
                    }
                }
            }
    }
    
    /**
     * 写入文件内容
     */
    @Override
    public int write(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
        System.out.println("write: " + path + ", size: " + size + ", offset: " + offset);

        String[] parts = splitPath(path);
        
        if (parts.length != 4) {
            return -ErrorCodes.EISDIR();
        }
        
        String serverName = parts[0];
        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];
        
        // 检查服务器是否为只读模式
        if (multiNacosService.isReadOnly(serverName)) {
            System.err.println("Server " + serverName + " is read-only");
            return -ErrorCodes.EPERM();
        }

        List<NacosConfig> configs = getConfigs(serverName, namespace);
        Optional<NacosConfig> configOpt = configs.stream()
            .filter(c -> c.getGroup().equals(group) && c.getFileName().equals(fileName))
            .findFirst();

        if (!configOpt.isPresent()) {
            return super.write(path, buf, size, offset, fi);
//            return -ErrorCodes.ENOENT();
        }

        // 从文件名中提取 dataId（去掉扩展名）
        String dataId = extractDataId(fileName);
        
        // 先从父类中读取当前文件的原始内容
        FileStat stat = new FileStat(Runtime.getSystemRuntime());
        int ret = getattr(path, stat);
        if (ret != 0) {
            return -ErrorCodes.EIO();
        }
        
        long fileSize = stat.st_size.longValue();
        byte[] originalContent = new byte[(int) fileSize];
        if (fileSize > 0) {
            Pointer originalBuf = Pointer.wrap(Runtime.getSystemRuntime(), ByteBuffer.allocate((int) fileSize));
            ret = read(path, originalBuf, fileSize, 0, null);
            if (ret < 0) {
                return -ErrorCodes.EIO();
            }
            originalBuf.get(0, originalContent, 0, (int) fileSize);
        }
        
        // 读取新写入的内容
        byte[] newContent = new byte[(int) size];
        buf.get(0, newContent, 0, (int) size);
        
        // 合并变更：将新内容写入到原始内容的指定偏移位置
        byte[] finalContent;
        long endOffset = offset + size;
        if (endOffset > fileSize) {
            // 如果写入超出原文件大小，需要扩展数组
            finalContent = new byte[(int) endOffset];
            System.arraycopy(originalContent, 0, finalContent, 0, (int) fileSize);
        } else {
            finalContent = originalContent;
        }
        System.arraycopy(newContent, 0, finalContent, (int) offset, (int) size);
        
        String contentStr = new String(finalContent, StandardCharsets.UTF_8);
        
        // 判断配置类型
        String type = detectType(fileName);
        
        // 发布配置
        NacosService nacosService = multiNacosService.getNacosService(serverName);
        boolean success = nacosService.publishConfig(namespace, dataId, group, contentStr, type);
        
        if (success) {
            // 清除缓存
            configsCache.remove(serverName + ":" + namespace);
            int writeSize = super.write(path, buf, size, offset, fi);
            return (int) size;
        } else {
            return -ErrorCodes.EIO();
        }
    }
    
    /**
     * 创建文件
     */
    @Override
    public int create(String path, long mode, FuseFileInfo fi) {
        System.out.println("create: " + path);
        String[] parts = splitPath(path);

        if (parts.length != 4) {
            return -ErrorCodes.EISDIR();
        }
        
        String serverName = parts[0];
        // 检查服务器是否为只读模式
        if (multiNacosService.isReadOnly(serverName)) {
            System.err.println("Server " + serverName + " is read-only");
            return -ErrorCodes.EROFS();
        }
        
        // FUSE中创建文件后通常会立即写入，这里返回成功
        return super.create(path, mode, fi);
    }
    
    /**
     * 取消链接（删除文件）
     */
    @Override
    public int unlink(String path) {
        System.out.println("unlink: " + path);
        // 注意：实际删除配置需要谨慎，这里暂时不支持删除
        String[] parts = splitPath(path);

        if (parts.length != 4) {
            return -ErrorCodes.EISDIR();
        }

        String serverName = parts[0];
        String namespace = parts[1].equals("_default") ? "" : parts[2];
        String group = parts[2];
        String fileName = parts[3];

        List<NacosConfig> configs = getConfigs(serverName, namespace);
        Optional<NacosConfig> configOpt = configs.stream()
                .filter(c -> c.getGroup().equals(group) && c.getFileName().equals(fileName))
                .findFirst();

        if (!configOpt.isPresent()) {
//            return -ErrorCodes.ENOENT();
            return super.unlink(path);
        }

        return -ErrorCodes.EPERM();
    }

    @Override
    public int rename(String path, String newName) {
        System.out.println("rename: " + path + " -> " + newName);

        //check source
        if (getNacosConfigWithPath(path) != null) {
            System.out.println("rename source can not be nacos file! path=" +  path);
            return -ErrorCodes.EPERM();
        }

        String[] parts = splitPath(newName);

        if (parts.length != 4) {
            return -ErrorCodes.EISDIR();
        }

        String serverName = parts[0];
        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];
        
        // 检查服务器是否为只读模式
        if (multiNacosService.isReadOnly(serverName)) {
            System.err.println("Server " + serverName + " is read-only");
            return -ErrorCodes.EROFS();
        }

        List<NacosConfig> configs = getConfigs(serverName, namespace);
        Optional<NacosConfig> configOpt = configs.stream()
                .filter(c -> c.getGroup().equals(group) && c.getFileName().equals(fileName))
                .findFirst();

        if (!configOpt.isPresent()) {
//            return -ErrorCodes.ENOENT();
            // Implement rename logic manually instead of calling super.rename()
            // Get the source path components
            String[] srcParts = splitPath(path);
            if (srcParts.length != 4) {
                return -ErrorCodes.EISDIR();
            }
            
            // Read content from source file
            FileStat stat = new FileStat(Runtime.getSystemRuntime());
            int ret = getattr(path, stat);
            if (ret != 0) {
                return ret;
            }
            
            Pointer buf = Pointer.wrap(Runtime.getSystemRuntime(), ByteBuffer.allocate((int) stat.st_size.longValue()));
            ret = read(path, buf, stat.st_size.longValue(), 0, null);
            if (ret < 0) {
                return ret;
            }
            
            // Check if target already exists
            FileStat targetStat = new FileStat(Runtime.getSystemRuntime());
            int targetRet = getattr(newName, targetStat);
            if (targetRet == 0) {
                // Target exists, truncate it first
                ret = truncate(newName, 0);
                if (ret != 0) {
                    return ret;
                }
                // Write content to existing target file
                ret = write(newName, buf, stat.st_size.longValue(), 0, null);
            } else {
                // Target doesn't exist, create new file
                ret = create(newName, 0777, null);
                if (ret != 0) {
                    return ret;
                }
                // Write content to new file
                ret = write(newName, buf, stat.st_size.longValue(), 0, null);
            }
            
            if (ret < 0) {
                return ret;
            }
            
            // Delete the old file
            ret = unlink(path);
            return ret;
        }

        // For Nacos-managed files, publish the renamed config
        FileStat stat = new FileStat(Runtime.getSystemRuntime());
        int ret = getattr(path, stat);
        if (ret != 0) {
            return -ErrorCodes.EIO();
        }
        Pointer buf = Pointer.wrap(Runtime.getSystemRuntime(), ByteBuffer.allocate((int) stat.st_size.longValue()));
        ret = read(path, buf, stat.st_size.longValue(), 0, null);
        if (ret < 0) {
            return -ErrorCodes.EIO();
        }
        
        NacosService nacosService = multiNacosService.getNacosService(serverName);
        boolean res = nacosService.publishConfig(namespace, configOpt.get().getDataId(), group, buf.getString(0), configOpt.get().getType());
        if (!res) {
            return -ErrorCodes.EIO();
        }
        
        // Check if target already exists
        FileStat targetStat = new FileStat(Runtime.getSystemRuntime());
        int targetRet = getattr(newName, targetStat);
        if (targetRet == 0) {
            // Target exists, truncate it first
            ret = truncate(newName, 0);
            if (ret != 0) {
                return ret;
            }
            // Write content to existing target file
            ret = write(newName, buf, stat.st_size.longValue(), 0, null);
        } else {
            // Target doesn't exist, create new file
            ret = create(newName, 0777, null);
            if (ret != 0) {
                return ret;
            }
            // Write content to new file
            ret = write(newName, buf, stat.st_size.longValue(), 0, null);
        }
        
        if (ret < 0) {
            return ret;
        }
        
        // Delete old file
        ret = unlink(path);
        return ret;
    }

    @Override
    public int truncate(String path, long offset) {
        return super.truncate(path, offset);
    }
    /**
     * 辅助方法：分割路径
     */
    private String[] splitPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return new String[0];
        }
        
        // 移除开头的 /
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        return path.split("/");
    }

    private NacosConfig getNacosConfigWithPath(String path) {
        String[] parts = splitPath(path);

        if (parts.length != 4) {
            return null;
        }

        String serverName = parts[0];
        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];

        List<NacosConfig> configs = getConfigs(serverName, namespace);
        Optional<NacosConfig> configOpt = configs.stream()
                .filter(c -> c.getGroup().equals(group) && c.getFileName().equals(fileName))
                .findFirst();
        return configOpt.orElse(null);
    }
    
    /**
     * 辅助方法：获取配置列表（带缓存）
     */
    private List<NacosConfig> getConfigs(String serverName, String namespace) {
        String cacheKey = serverName + ":" + namespace;
        return configsCache.computeIfAbsent(cacheKey, key -> {
            NacosService nacosService = multiNacosService.getNacosService(serverName);
            return nacosService.getConfigs(namespace);
        });
    }
    
    /**
     * 辅助方法：从文件名提取 dataId
     */
    private String extractDataId(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
    
    /**
     * 辅助方法：检测配置类型
     */
    private String detectType(String fileName) {
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return "yaml";
        } else if (fileName.endsWith(".json")) {
            return "json";
        } else if (fileName.endsWith(".xml")) {
            return "xml";
        } else {
            return "text";
        }
    }
}
