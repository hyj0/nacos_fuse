package org.hyj.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hyj.model.MultiNacosConfig;

import java.io.File;
import java.util.List;

/**
 * 测试YAML配置文件解析
 */
public class TestConfigParser {
    public static void main(String[] args) {
        try {
            // 加载配置文件
            File configFile = new File("config.yaml");
            if (!configFile.exists()) {
                System.out.println("配置文件不存在: " + configFile.getAbsolutePath());
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            MultiNacosConfig config = mapper.readValue(configFile, MultiNacosConfig.class);
            
            System.out.println("========== 配置解析成功 ==========");
            System.out.println("服务器数量: " + config.getServers().size());
            System.out.println();
            
            List<MultiNacosConfig.NacosServerConfig> servers = config.getServers();
            for (int i = 0; i < servers.size(); i++) {
                MultiNacosConfig.NacosServerConfig server = servers.get(i);
                System.out.println("服务器 " + (i + 1) + ":");
                System.out.println("  名称: " + server.getName());
                System.out.println("  URL: " + server.getUrl());
                System.out.println("  命名空间模式: " + server.getNamespacePattern());
                System.out.println("  只读模式: " + server.isReadOnly());
                System.out.println();
            }
            
            System.out.println("========== 测试通过 ==========");
            
        } catch (Exception e) {
            System.err.println("配置解析失败:");
            e.printStackTrace();
        }
    }
}
