package org.hyj.model;

/**
 * Nacos配置信息模型
 */
public class NacosConfig {
    private String dataId;
    private String group;
    private String content;
    private String type;
    
    public NacosConfig() {
    }
    
    public NacosConfig(String dataId, String group, String content) {
        this.dataId = dataId;
        this.group = group;
        this.content = content;
    }
    
    public String getDataId() {
        return dataId;
    }
    
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * 生成文件名：dataId + "." + type
     */
    public String getFileName() {
//        if (type != null && !type.isEmpty()) {
//            return dataId + "." + type.toLowerCase();
//        }
        return dataId;
    }
}
