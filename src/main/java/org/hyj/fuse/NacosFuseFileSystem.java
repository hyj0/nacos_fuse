package org.hyj.fuse;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import org.hyj.model.NacosConfig;
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
 * 目录结构: /namespaces/{namespace}/{group}/{config_file}
 */
public class NacosFuseFileSystem extends MemoryFS {
    private final NacosService nacosService;
    
    // 缓存配置信息，避免频繁请求
    private final Map<String, List<NacosConfig>> configsCache = new HashMap<>();
    
    public NacosFuseFileSystem(NacosService nacosService) {
        this.nacosService = nacosService;
          /*
removed ‘Directory with files/hello.txt’
removed ‘Directory with files/hello again.txt’
removed ‘Directory with files/Sample nested directory/So deep.txt’
removed directory: ‘Directory with files/Sample nested directory’
removed directory: ‘Directory with files’
removed directory: ‘Sample directory’
removed ‘Sample file.txt’
             */
            super.unlink("/Directory with files/hello.txt");
            super.unlink("/Directory with files/hello again.txt");
            super.unlink("/Directory with files/Sample nested directory/So deep.txt");
            super.rmdir("/Directory with files/Sample nested directory");
            super.rmdir("/Directory with files");
            super.rmdir("/Sample directory");
            super.unlink("/Sample file.txt");
            super.mkdir("/namespaces", 0777);
            nacosService.getNamespaces().forEach(ns -> {
                String displayName = ns.isEmpty() ? "_default" : ns;
                super.mkdir("/namespaces/" + displayName, 0777);
                List<NacosConfig> configs = get_configs(displayName);
                // 提取所有唯一的分组
                Set<String> groups = new LinkedHashSet<>();
                for (NacosConfig config : configs) {
                    groups.add(config.getGroup());
                }
                for (String group : groups) {
                    super.mkdir("/namespaces/" + displayName + "/" + group, 0777);
                    configs.forEach(config -> {
                        if (config.getGroup().equals( group)) {
                            super.create("/namespaces/" + displayName + "/" + group + "/" + config.getFileName(), 0777, null);
                            Pointer buf = Pointer.wrap(Runtime.getSystemRuntime(), ByteBuffer.wrap(config.getContent().getBytes(StandardCharsets.UTF_8)));
                            super.write("/namespaces/" + displayName + "/" + group + "/" + config.getFileName(), buf, buf.size(), 0, null);
                        }
                    });
                }
            });
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
        
        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];

        List<NacosConfig> configs = get_configs(namespace);
        Optional<NacosConfig> configOpt = configs.stream()
            .filter(c -> c.getGroup().equals(group) && c.getFileName().equals(fileName))
            .findFirst();

        if (!configOpt.isPresent()) {
            return super.write(path, buf, size, offset, fi);
//            return -ErrorCodes.ENOENT();
        }

        // 从文件名中提取 dataId（去掉扩展名）
        String dataId = extractDataId(fileName);
        
        // 读取新内容
        byte[] newContent = new byte[(int) size];
        buf.get(0, newContent, 0, (int) size);
        String contentStr = new String(newContent, StandardCharsets.UTF_8);
        
        // 判断配置类型
        String type = detectType(fileName);
        
        // 发布配置
        boolean success = nacosService.publishConfig(namespace, dataId, group, contentStr, type);
        
        if (success) {
            // 清除缓存
            configsCache.remove(namespace);
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

        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];

        List<NacosConfig> configs = get_configs(namespace);
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

        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];

        List<NacosConfig> configs = get_configs(namespace);
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

        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];

        List<NacosConfig> configs = get_configs(namespace);
        Optional<NacosConfig> configOpt = configs.stream()
                .filter(c -> c.getGroup().equals(group) && c.getFileName().equals(fileName))
                .findFirst();
        return configOpt.orElse(null);
    }
    
    /**
     * 辅助方法：获取配置列表（带缓存）
     */
    private List<NacosConfig> get_configs(String namespace) {
        return configsCache.computeIfAbsent(namespace, ns -> nacosService.getConfigs(ns));
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
