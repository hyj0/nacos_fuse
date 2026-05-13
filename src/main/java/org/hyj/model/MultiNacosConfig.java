package org.hyj.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 多Nacos服务器配置模型
 */
public class MultiNacosConfig {
    private List<NacosServerConfig> servers;
    
    public MultiNacosConfig() {
    }
    
    public MultiNacosConfig(List<NacosServerConfig> servers) {
        this.servers = servers;
    }
    
    public List<NacosServerConfig> getServers() {
        return servers;
    }
    
    public void setServers(List<NacosServerConfig> servers) {
        this.servers = servers;
    }
    
    /**
     * Nacos服务器配置
     */
    public static class NacosServerConfig {
        private String name;
        private String url;
        
        @JsonProperty("namespace-pattern")
        private String namespacePattern;
        
        @JsonProperty("read-only")
        private boolean readOnly;
        
        public NacosServerConfig() {
        }
        
        public NacosServerConfig(String name, String url, String namespacePattern, boolean readOnly) {
            this.name = name;
            this.url = url;
            this.namespacePattern = namespacePattern;
            this.readOnly = readOnly;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getNamespacePattern() {
            return namespacePattern;
        }
        
        public void setNamespacePattern(String namespacePattern) {
            this.namespacePattern = namespacePattern;
        }
        
        public boolean isReadOnly() {
            return readOnly;
        }
        
        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }
    }
}
