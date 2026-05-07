# Nacos FUSE 文件系统

将 Nacos 配置文件以文件系统形式挂载到本地，实现配置的可视化和便捷编辑。

## 功能特性

- 📁 以目录结构展示 Nacos 命名空间、分组和配置
- 📖 直接读取配置文件内容
- ✏️ 支持编辑并同步更新到 Nacos Server
- 🔄 自动缓存配置信息，提升性能

## 目录结构

```
/mount-point/
└── namespaces/
    ├── _default/              # 默认命名空间
    │   ├── DEFAULT_GROUP/
    │   │   ├── application.properties
    │   │   └── config.yml
    │   └── TEST_GROUP/
    │       └── test.properties
    └── phoenix-test/          # 自定义命名空间
        ├── DEFAULT_GROUP/
        │   └── application.yml
        └── OTHER_GROUP/
            └── other.json
```

## 前置要求

### Windows 用户
需要安装 WinFsp：
1. 下载 WinFsp: https://winfsp.dev/rel/
2. 安装 WinFsp

### Linux 用户
需要安装 FUSE：
```bash
# Ubuntu/Debian
sudo apt-get install libfuse-dev

# CentOS/RHEL
sudo yum install fuse-devel
```

### macOS 用户
需要安装 macFUSE：
```bash
brew install --cask macfuse
```

## 编译项目

```bash
mvn clean package
```

## 运行程序

### 方式一：使用默认配置

```bash
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar
```

默认会连接到 `10.9.2.85:8848`，挂载到 `/tmp/nacos-config`（Linux/Mac）或相应目录。

### 方式二：指定参数

```bash
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar <nacos-server-addr> <mount-point>
```

示例：
```bash
# Linux/Mac
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar 10.9.2.85:8848 /mnt/nacos

# Windows
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar 10.9.2.85:8848 Z:
```

## 使用示例

### 1. 查看配置列表

```bash
ls /tmp/nacos-config/namespaces/
ls /tmp/nacos-config/namespaces/phoenix-test/
ls /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/
```

### 2. 读取配置内容

```bash
cat /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
```

### 3. 编辑配置

```bash
# 使用编辑器打开
vim /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties

# 保存后会自动同步到 Nacos Server
```

### 4. 卸载文件系统

```bash
# Linux/Mac
fusermount -u /tmp/nacos-config

# Windows
# 在文件资源管理器中右键点击挂载的驱动器，选择"弹出"
```

## 注意事项

1. **权限问题**：确保有权限访问挂载点目录
2. **网络连通性**：确保能访问 Nacos Server
3. **配置缓存**：配置列表会被缓存，新增的配置可能需要重启才能看到
4. **删除限制**：当前版本不支持通过文件系统删除配置
5. **Windows 支持**：需要使用盘符作为挂载点（如 Z:）

## 技术栈

- **Nacos SDK**: 2.4.2 (pure version)
- **JNR-FUSE**: 0.5.8
- **Java**: 8+

## 开发说明

### 核心类

- `Main.java`: 程序入口
- `NacosFuseFileSystem.java`: FUSE 文件系统实现
- `NacosService.java`: Nacos API 封装
- `NacosConfig.java`: 配置数据模型

### 扩展功能

可以通过修改 `NacosService.getNamespaces()` 方法，使用 HTTP API 动态获取命名空间列表：

```java
// GET http://10.9.2.85:8848/nacos/v1/console/namespaces
```

同样，可以通过 HTTP API 获取完整的配置列表：

```java
// GET http://10.9.2.85:8848/nacos/v1/cs/configs?tenant={namespace}&pageNo=1&pageSize=100
```

## 许可证

MIT License
