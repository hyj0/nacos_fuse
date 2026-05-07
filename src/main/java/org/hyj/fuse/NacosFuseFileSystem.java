package org.hyj.fuse;

import jnr.ffi.Pointer;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import org.hyj.model.NacosConfig;
import org.hyj.service.NacosService;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Nacos FUSE文件系统实现
 * 目录结构: /namespaces/{namespace}/{group}/{config_file}
 */
public class NacosFuseFileSystem extends FuseStubFS {
    private final NacosService nacosService;
    
    // 缓存配置信息，避免频繁请求
    private final Map<String, List<NacosConfig>> configsCache = new HashMap<>();
    
    public NacosFuseFileSystem(NacosService nacosService) {
        this.nacosService = nacosService;
    }
    
    /**
     * 获取文件属性
     */
    @Override
    public int getattr(String path, FileStat stat) {
        System.out.println("getattr: " + path);
        
        if (path.equals("/")) {
            // 根目录
            stat.st_mode.set(FileStat.S_IFDIR | 0755);
            stat.st_nlink.set(2);
            return 0;
        }
        
        String[] parts = splitPath(path);
        
        if (parts.length == 1 && parts[0].equals("namespaces")) {
            // /namespaces 目录
            stat.st_mode.set(FileStat.S_IFDIR | 0755);
            stat.st_nlink.set(2);
            return 0;
        }
        
        if (parts.length == 2) {
            // /namespaces/{namespace} 目录
            String namespace = parts[1];
            List<String> namespaces = nacosService.getNamespaces();
            String displayNamespace = namespace.equals("_default") ? "" : namespace;
            
            if (namespaces.contains(displayNamespace)) {
                stat.st_mode.set(FileStat.S_IFDIR | 0755);
                stat.st_nlink.set(2);
                return 0;
            }
        }
        
        if (parts.length == 3) {
            // /namespaces/{namespace}/{group} 目录
            String namespace = parts[1].equals("_default") ? "" : parts[1];
            String group = parts[2];
            
            List<NacosConfig> configs = get_configs(namespace);
            boolean groupExists = configs.stream().anyMatch(c -> c.getGroup().equals(group));
            
            if (groupExists) {
                stat.st_mode.set(FileStat.S_IFDIR | 0755);
                stat.st_nlink.set(2);
                return 0;
            }
        }
        
        if (parts.length == 4) {
            // /namespaces/{namespace}/{group}/{config_file} 文件
            String namespace = parts[1].equals("_default") ? "" : parts[1];
            String group = parts[2];
            String fileName = parts[3];
            
            List<NacosConfig> configs = get_configs(namespace);
            Optional<NacosConfig> configOpt = configs.stream()
                .filter(c -> c.getGroup().equals(group) && c.getFileName().equals(fileName))
                .findFirst();
            
            if (configOpt.isPresent()) {
                NacosConfig config = configOpt.get();
                stat.st_mode.set(FileStat.S_IFREG | 0644);
                stat.st_nlink.set(1);
                stat.st_size.set(config.getContent().getBytes(StandardCharsets.UTF_8).length);
                return 0;
            }
        }
        
        return -ErrorCodes.ENOENT();
    }
    
    /**
     * 读取目录内容
     */
    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, long offset, FuseFileInfo fi) {
        System.out.println("readdir: " + path);
        
        if (path.equals("/")) {
            // 根目录：显示 namespaces
            filler.apply(buf, ".", null, 0);
            filler.apply(buf, "..", null, 0);
            filler.apply(buf, "namespaces", null, 0);
            return 0;
        }
        
        String[] parts = splitPath(path);
        
        if (parts.length == 1 && parts[0].equals("namespaces")) {
            // /namespaces 目录：显示所有命名空间
            filler.apply(buf, ".", null, 0);
            filler.apply(buf, "..", null, 0);
            
            List<String> namespaces = nacosService.getNamespaces();
            for (String ns : namespaces) {
                // 空字符串表示默认命名空间，显示为 _default
                String displayName = ns.isEmpty() ? "_default" : ns;
                filler.apply(buf, displayName, null, 0);
            }
            return 0;
        }
        
        if (parts.length == 2) {
            // /namespaces/{namespace} 目录：显示所有分组
            filler.apply(buf, ".", null, 0);
            filler.apply(buf, "..", null, 0);
            
            String namespace = parts[1].equals("_default") ? "" : parts[1];
            List<NacosConfig> configs = get_configs(namespace);
            
            // 提取所有唯一的分组
            Set<String> groups = new LinkedHashSet<>();
            for (NacosConfig config : configs) {
                groups.add(config.getGroup());
            }
            
            for (String group : groups) {
                filler.apply(buf, group, null, 0);
            }
            return 0;
        }
        
        if (parts.length == 3) {
            // /namespaces/{namespace}/{group} 目录：显示所有配置文件
            filler.apply(buf, ".", null, 0);
            filler.apply(buf, "..", null, 0);
            
            String namespace = parts[1].equals("_default") ? "" : parts[1];
            String group = parts[2];
            
            List<NacosConfig> configs = get_configs(namespace);
            for (NacosConfig config : configs) {
                if (config.getGroup().equals(group)) {
                    filler.apply(buf, config.getFileName(), null, 0);
                }
            }
            return 0;
        }
        
        return -ErrorCodes.ENOENT();
    }
    
    /**
     * 读取文件内容
     */
    @Override
    public int read(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
        System.out.println("read: " + path + ", size: " + size + ", offset: " + offset);
        
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
            return -ErrorCodes.ENOENT();
        }
        
        NacosConfig config = configOpt.get();
        byte[] content = config.getContent().getBytes(StandardCharsets.UTF_8);
        
        if (offset >= content.length) {
            return 0;
        }
        
        int bytesToRead = (int) Math.min(size, content.length - offset);
        buf.put(0, content, (int) offset, bytesToRead);
        
        return bytesToRead;
    }
    
    /**
     * 写入文件内容
     */
    @Override
    public int write(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
        System.out.println("write: " + path + ", size: " + size + ", offset: " + offset);

        if (true) {
            throw new UnsupportedOperationException("Not implemented");
        }
        
        String[] parts = splitPath(path);
        
        if (parts.length != 4) {
            return -ErrorCodes.EISDIR();
        }
        
        String namespace = parts[1].equals("_default") ? "" : parts[1];
        String group = parts[2];
        String fileName = parts[3];
        
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
        // FUSE中创建文件后通常会立即写入，这里返回成功
        return 0;
    }
    
    /**
     * 取消链接（删除文件）
     */
    @Override
    public int unlink(String path) {
        System.out.println("unlink: " + path);
        // 注意：实际删除配置需要谨慎，这里暂时不支持删除
        return -ErrorCodes.EPERM();
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
