package org.hyj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hyj.model.MultiNacosConfig;
import org.hyj.model.NacosConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 多Nacos服务管理器
 */
public class MultiNacosService {
    private final Map<String, NacosService> nacosServices = new HashMap<>();
    private final Map<String, MultiNacosConfig.NacosServerConfig> serverConfigs = new HashMap<>();
    
    public MultiNacosService(String configFilePath) throws IOException {
        // 加载YAML配置文件
        MultiNacosConfig multiConfig = loadConfig(configFilePath);
        
        // 为每个服务器创建NacosService实例
        if (multiConfig != null && multiConfig.getServers() != null) {
            for (MultiNacosConfig.NacosServerConfig serverConfig : multiConfig.getServers()) {
                String serverName = serverConfig.getName();
                String url = serverConfig.getUrl();
                
                // 从URL中提取serverAddr (去掉http://前缀)
                String serverAddr = url.replaceFirst("^https?://", "");
                serverAddr = serverAddr.replaceFirst("/$", "");
                NacosService nacosService = new NacosService(serverAddr);
                nacosServices.put(serverName, nacosService);
                serverConfigs.put(serverName, serverConfig);
            }
        }
    }
    
    /**
     * 从YAML文件加载配置
     */
    private MultiNacosConfig loadConfig(String configFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // 允许未知属性，以兼容不同的YAML结构
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 读取整个YAML文件为Map
        Map<String, Object> yamlData = mapper.readValue(new File(configFilePath), Map.class);
        
        // 检查是否有顶层的"nacos"键
        if (yamlData.containsKey("nacos")) {
            // 嵌套结构: { nacos: { servers: [...] } }
            Map<String, Object> nacosData = (Map<String, Object>) yamlData.get("nacos");
            return mapper.convertValue(nacosData, MultiNacosConfig.class);
        } else {
            // 扁平结构: { servers: [...] }
            return mapper.convertValue(yamlData, MultiNacosConfig.class);
        }
    }
    
    /**
     * 获取所有服务器名称
     */
    public Set<String> getServerNames() {
        return nacosServices.keySet();
    }
    
    /**
     * 获取指定服务器的NacosService实例
     */
    public NacosService getNacosService(String serverName) {
        return nacosServices.get(serverName);
    }
    
    /**
     * 获取指定服务器的配置
     */
    public MultiNacosConfig.NacosServerConfig getServerConfig(String serverName) {
        return serverConfigs.get(serverName);
    }
    
    /**
     * 检查指定服务器是否为只读模式
     */
    public boolean isReadOnly(String serverName) {
        MultiNacosConfig.NacosServerConfig config = serverConfigs.get(serverName);
        return config != null && config.isReadOnly();
    }
    
    /**
     * 获取指定服务器的命名空间列表（根据namespacePattern过滤）
     */
    public List<String> getNamespacesForServer(String serverName) {
        NacosService nacosService = nacosServices.get(serverName);
        MultiNacosConfig.NacosServerConfig config = serverConfigs.get(serverName);
        
        if (nacosService == null || config == null) {
            return Collections.emptyList();
        }
        
        List<String> allNamespaces = nacosService.getNamespaces();
        String pattern = config.getNamespacePattern();
        
        // 如果没有设置pattern或pattern为"*"，返回所有命名空间
        if (pattern == null || pattern.isEmpty() || "*".equals(pattern)) {
            return allNamespaces;
        }
        
        // 使用正则表达式过滤命名空间
        try {
            Pattern regexPattern = Pattern.compile(pattern);
            List<String> filteredNamespaces = new ArrayList<>();
            for (String namespace : allNamespaces) {
                if (regexPattern.matcher(namespace).matches()) {
                    filteredNamespaces.add(namespace);
                }
            }
            return filteredNamespaces;
        } catch (Exception e) {
            System.err.println("Invalid namespace pattern: " + pattern + " for server: " + serverName);
            e.printStackTrace();
            return allNamespaces; // 如果pattern无效，返回所有命名空间
        }
    }
    
    /**
     * 获取所有服务器的命名空间映射
     * 返回格式: Map<serverName, List<namespace>>
     */
    public Map<String, List<String>> getAllNamespaces() {
        Map<String, List<String>> result = new HashMap<>();
        for (String serverName : nacosServices.keySet()) {
            result.put(serverName, getNamespacesForServer(serverName));
        }
        return result;
    }
}
