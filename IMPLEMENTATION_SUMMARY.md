# 多Nacos服务器支持 - 实现总结

## 概述

本次更新为 Nacos FUSE 文件系统添加了多服务器支持，允许同时挂载多个 Nacos 服务器实例，每个服务器可以独立配置命名空间过滤规则和读写权限。

## 主要变更

### 1. 新增文件

#### 模型类
- **`src/main/java/org/hyj/model/MultiNacosConfig.java`**
  - 多Nacos服务器配置模型
  - 包含 `NacosServerConfig` 内部类，定义单个服务器的配置参数
  - 支持字段：name, url, namespacePattern, readOnly

#### 服务类
- **`src/main/java/org/hyj/service/MultiNacosService.java`**
  - 多服务器管理服务
  - 从 YAML 配置文件加载多个 Nacos 服务器配置
  - 管理多个 `NacosService` 实例
  - 提供命名空间过滤功能（基于正则表达式）
  - 提供只读模式检查

#### 测试类
- **`src/test/java/TestConfigParser.java`**
  - 测试 YAML 配置文件解析
  - 验证配置格式的正确性

#### 配置文件
- **`config.yaml`**
  - 示例配置文件
  - 展示如何配置多个 Nacos 服务器

#### 文档
- **`MULTI_SERVER.md`**
  - 详细的功能说明文档
  - 包含配置说明、使用场景、故障排除等

- **`MULTI_SERVER_QUICKSTART.md`**
  - 快速开始指南
  - 简明扼要的使用说明和示例

### 2. 修改文件

#### `pom.xml`
- 添加 Jackson YAML 依赖（`jackson-dataformat-yaml:2.15.2`）
- 用于解析 YAML 配置文件

#### `src/main/java/org/hyj/fuse/NacosFuseFileSystem.java`
- **重大变更**：从单服务器架构改为多服务器架构
- 构造函数参数从 `NacosService` 改为 `MultiNacosService`
- 目录结构从 `/namespaces/{namespace}/{group}/{file}` 改为 `/{server-name}/{namespace}/{group}/{file}`
- 更新所有路径解析逻辑（parts.length 从 4 改为 5）
- 在 write、create、rename 方法中添加只读模式检查
- 缓存键从 `namespace` 改为 `serverName:namespace`

#### `src/main/java/org/hyj/Main.java`
- 命令行参数从 `serverAddr mountPoint` 改为 `configFilePath mountPoint`
- 初始化 `MultiNacosService` 替代 `NacosService`
- 添加配置文件存在性检查
- 更新启动信息显示
- 移除不再需要的注释代码

#### `run.bat` (Windows 启动脚本)
- 默认参数从 `NACOS_SERVER` 改为 `CONFIG_FILE`
- 更新提示信息以反映多服务器支持
- 传递配置文件路径而非服务器地址

#### `run.sh` (Linux/Mac 启动脚本)
- 与 run.bat 相同的变更
- 保持跨平台一致性

#### `product.md`
- 更新 YAML 配置格式（从嵌套结构改为扁平结构）
- 添加实现说明章节
- 添加使用方法说明
- 修正配置示例格式

## 技术实现细节

### 配置解析

使用 Jackson YAML 模块解析配置文件：

```java
ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
MultiNacosConfig config = mapper.readValue(configFile, MultiNacosConfig.class);
```

### 命名空间过滤

通过正则表达式实现灵活的命名空间过滤：

```java
Pattern regexPattern = Pattern.compile(pattern);
for (String namespace : allNamespaces) {
    if (regexPattern.matcher(namespace).matches()) {
        filteredNamespaces.add(namespace);
    }
}
```

### 只读保护

在写操作前检查服务器的只读状态：

```java
if (multiNacosService.isReadOnly(serverName)) {
    return -ErrorCodes.EROFS(); // Read-only file system
}
```

### 缓存策略

使用复合键 `serverName:namespace` 进行缓存：

```java
String cacheKey = serverName + ":" + namespace;
return configsCache.computeIfAbsent(cacheKey, key -> {
    NacosService nacosService = multiNacosService.getNacosService(serverName);
    return nacosService.getConfigs(namespace);
});
```

## 目录结构对比

### 修改前（单服务器）
```
/mount-point/
└── namespaces/
    ├── _default/
    │   └── DEFAULT_GROUP/
    │       └── application.properties
    └── phoenix-test/
        └── DEFAULT_GROUP/
            └── application.yml
```

### 修改后（多服务器）
```
/mount-point/
├── nacos-server1/
│   ├── _default/
│   │   └── DEFAULT_GROUP/
│   │       └── application.properties
│   └── phoenix-test/
│       └── DEFAULT_GROUP/
│           └── application.yml
└── nacos-server2/
    ├── _default/
    │   └── DEFAULT_GROUP/
    │       └── config.yml
    └── dev/
        └── DEFAULT_GROUP/
            └── service.properties
```

## 向后兼容性

⚠️ **重要提示**：此更新**不向后兼容**原有的单服务器模式。

- **旧版本**：`java -jar nacos_fuse.jar <server_addr> <mount_point>`
- **新版本**：`java -jar nacos_fuse.jar <config_file> <mount_point>`

如果需要使用单服务器，可以在配置文件中只配置一个服务器。

## 使用示例

### 基本用法

```bash
# Windows
run.bat config.yaml N:\

# Linux/Mac
./run.sh config.yaml /tmp/nacos_mnt
```

### 配置文件示例

```yaml
nacos:
  servers:
    - name: dev-server
      url: http://dev-nacos:8848
      namespace-pattern: "*"
      read-only: false
    - name: prod-server
      url: http://prod-nacos:8848
      namespace-pattern: "^prod$"
      read-only: true
```

## 测试建议

1. **配置解析测试**
   ```bash
   java -cp target/nacos_fuse-1.0-SNAPSHOT.jar org.hyj.test.TestConfigParser
   ```

2. **单服务器测试**
   - 配置文件中只配置一个服务器
   - 验证基本功能正常

3. **多服务器测试**
   - 配置多个服务器
   - 验证目录结构正确
   - 验证命名空间过滤
   - 验证只读保护

4. **边界情况测试**
   - 无效的配置文件
   - 无法连接的服务器
   - 无效的正则表达式
   - 空的命名空间列表

## 已知限制

1. **不支持热重载**：修改配置文件需要重启程序
2. **缓存无过期**：配置更新后可能需要重启才能看到最新内容
3. **无认证支持**：当前版本不支持 Nacos 用户名密码认证
4. **无健康检查**：不会定期检查服务器连接状态

## 未来改进方向

1. 支持配置文件热重载
2. 添加缓存过期机制
3. 支持 Nacos 认证（用户名/密码）
4. 添加服务器健康检查
5. 支持动态添加/删除服务器
6. 添加配置变更监听和自动刷新
7. 支持更多的命名空间匹配模式（如通配符）

## 相关文件清单

### 核心代码
- `src/main/java/org/hyj/model/MultiNacosConfig.java` - 配置模型
- `src/main/java/org/hyj/service/MultiNacosService.java` - 多服务器管理
- `src/main/java/org/hyj/fuse/NacosFuseFileSystem.java` - FUSE 文件系统
- `src/main/java/org/hyj/Main.java` - 主入口

### 测试
- `src/test/java/TestConfigParser.java` - 配置解析测试

### 配置
- `config.yaml` - 示例配置文件
- `pom.xml` - Maven 依赖配置

### 脚本
- `run.bat` - Windows 启动脚本
- `run.sh` - Linux/Mac 启动脚本

### 文档
- `MULTI_SERVER.md` - 详细功能文档
- `MULTI_SERVER_QUICKSTART.md` - 快速开始指南
- `IMPLEMENTATION_SUMMARY.md` - 本文件
- `product.md` - 产品需求文档（已更新）

## 总结

本次更新成功实现了多 Nacos 服务器支持，提供了灵活的配置管理和访问控制功能。通过 YAML 配置文件，用户可以轻松管理多个 Nacos 服务器实例，并通过命名空间过滤和只读模式实现精细化的配置管理。

虽然此更新破坏了向后兼容性，但新的架构更加灵活和可扩展，为未来的功能增强奠定了良好的基础。
