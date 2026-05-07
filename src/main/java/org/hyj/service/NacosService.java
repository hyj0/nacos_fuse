package org.hyj.service;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyj.model.NacosConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nacos服务客户端
 */
public class NacosService {
    private final String serverAddr;
    private final Map<String, ConfigService> configServiceMap = new ConcurrentHashMap<>();
    
    public NacosService(String serverAddr) {
        this.serverAddr = serverAddr;
    }
    
    /**
     * 获取或创建ConfigService
     */
    private ConfigService getConfigService(String namespace) throws NacosException {
        return configServiceMap.computeIfAbsent(namespace, ns -> {
            try {
                Properties properties = new Properties();
                properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);
                if (ns != null && !ns.isEmpty()) {
                    properties.setProperty(PropertyKeyConst.NAMESPACE, ns);
                }
                return NacosFactory.createConfigService(properties);
            } catch (NacosException e) {
                throw new RuntimeException("Failed to create ConfigService for namespace: " + ns, e);
            }
        });
    }
    
    /**
     * 获取所有命名空间列表
     * 通过调用 Nacos HTTP API: GET /nacos/v1/console/namespaces 获取
     */
    public List<String> getNamespaces() {
        List<String> namespaces = new ArrayList<>();
        
        try {
            // 构建请求URL
            String urlString = "http://" + serverAddr + "/nacos/v1/console/namespaces";
            URL url = new URL(urlString);
            
            // 创建HTTP连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                // 解析JSON响应
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.toString());
                
                // 检查响应码
                int code = rootNode.path("code").asInt();
                if (code == 200) {
                    JsonNode dataNode = rootNode.path("data");
                    
                    // 解析data数组中的命名空间
                    if (dataNode.isArray()) {
                        for (JsonNode node : dataNode) {
                            String namespaceId = node.path("namespace").asText();
                            String namespaceShowName = node.path("namespaceShowName").asText();
                            
                            // 添加命名空间ID（空字符串表示默认命名空间）
                            if (!namespaces.contains(namespaceId)) {
                                namespaces.add(namespaceId);
                            }
                        }
                    }
                } else {
                    System.err.println("Nacos API returned error code: " + code);
                    // 降级方案：返回默认命名空间
                    namespaces.add("");
                }
            } else {
                System.err.println("Failed to get namespaces, response code: " + responseCode);
                // 降级方案：返回默认命名空间
                namespaces.add("");
            }
            
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Failed to get namespaces from Nacos API");
            e.printStackTrace();
            // 降级方案：返回默认命名空间
            namespaces.add("");
            throw new RuntimeException("Failed to get namespaces", e);
        }
        
        return namespaces;
    }
    
    /**
     * 获取指定命名空间下的所有配置
     * 通过调用 Nacos HTTP API: GET /nacos/v1/cs/configs 获取配置列表
     */
    public List<NacosConfig> getConfigs(String namespace) {
        List<NacosConfig> configs = new ArrayList<>();
        
        try {
            // 构建请求URL
            String urlString = "http://" + serverAddr + "/nacos/v1/cs/configs";
            urlString += "?dataId=&group=&appName=&config_tags=&pageNo=1&pageSize=10000";
            if (namespace != null && !namespace.isEmpty()) {
                urlString += "&tenant=" + namespace;
            }
            urlString += "&search=accurate";
            
            URL url = new URL(urlString);
            
            // 创建HTTP连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                // 解析JSON响应
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.toString());
                
                // 解析pageItems数组中的配置项
                JsonNode pageItems = rootNode.path("pageItems");
                if (pageItems.isArray()) {
                    for (JsonNode item : pageItems) {
                        String dataId = item.path("dataId").asText();
                        String group = item.path("group").asText();
                        String content = item.path("content").asText();
                        String type = item.path("type").asText();
                        
                        if (dataId != null && !dataId.isEmpty()) {
                            NacosConfig config = new NacosConfig(dataId, group, content);
                            
                            // 设置配置类型
                            if (type != null && !type.isEmpty()) {
                                config.setType(type.toLowerCase());
                            } else {
                                // 根据dataId后缀判断类型
                                if (dataId.endsWith(".yml") || dataId.endsWith(".yaml")) {
                                    config.setType("yaml");
                                } else if (dataId.endsWith(".json")) {
                                    config.setType("json");
                                } else if (dataId.endsWith(".xml")) {
                                    config.setType("xml");
                                } else if (dataId.endsWith(".properties")) {
                                    config.setType("properties");
                                } else {
                                    config.setType("text");
                                }
                            }
                            
                            configs.add(config);
                        }
                    }
                }
            } else {
                System.err.println("Failed to get configs, response code: " + responseCode);
            }
            
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Failed to get configs for namespace: " + namespace);
            e.printStackTrace();
        }
        
        return configs;
    }
    
    /**
     * 获取指定配置的详细内容
     */
    public String getConfigContent(String namespace, String dataId, String group) {
        try {
            ConfigService configService = getConfigService(namespace);
            return configService.getConfig(dataId, group, 3000);
        } catch (Exception e) {
            System.err.println("Failed to get config content: " + dataId);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 发布/更新配置
     */
    public boolean publishConfig(String namespace, String dataId, String group, String content, String type) {
        try {
            ConfigService configService = getConfigService(namespace);
            return configService.publishConfig(dataId, group, content, type);
        } catch (Exception e) {
            System.err.println("Failed to publish config: " + dataId);
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        NacosService nacosService = new NacosService("10.9.2.85:8848");
        List<String> namespaces = nacosService.getNamespaces();
        System.out.println(namespaces);

        List<NacosConfig> configs = nacosService.getConfigs("dev");

        for (NacosConfig config : configs) {
            System.out.println(config.getFileName() + ": " + config.getGroup());
        }
    }
}
