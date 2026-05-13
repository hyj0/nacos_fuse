# Nacos FUSE 文件系统

将 Nacos 配置文件以文件系统形式挂载到本地，实现配置的可视化和便捷编辑。

## 🎉 新功能：多服务器支持

**v2.0** 现已支持同时挂载多个 Nacos 服务器！

- ✅ **多服务器支持** - 同时管理多个 Nacos 实例
- ✅ **命名空间过滤** - 通过正则表达式过滤命名空间
- ✅ **读写控制** - 为每个服务器设置只读或读写模式
- ✅ **YAML配置** - 简洁的配置文件管理

详细文档请查看：
- [多服务器快速开始](MULTI_SERVER_QUICKSTART.md)
- [多服务器详细说明](MULTI_SERVER.md)
- [实现总结](IMPLEMENTATION_SUMMARY.md)

## 功能特性

- 📁 以目录结构展示 Nacos 命名空间、分组和配置
- 📖 直接读取配置文件内容
- ✏️ 支持编辑并同步更新到 Nacos Server
- 🔄 自动缓存配置信息，提升性能
- 🌐 支持多个 Nacos 服务器实例（NEW!）
- 🔒 支持只读模式保护重要配置（NEW!）

## 目录结构

### 多服务器模式（推荐）

```
/mount-point/
├── nacos-server1/
│   ├── _default/              # 默认命名空间
│   │   └── DEFAULT_GROUP/
│   │       ├── application.properties
│   │       └── config.yml
│   └── phoenix-test/          # 自定义命名空间
│       └── DEFAULT_GROUP/
│           └── application.yml
└── nacos-server2/
    ├── _default/
    │   └── DEFAULT_GROUP/
    │       └── service.properties
    └── dev/
        └── DEFAULT_GROUP/
            └── test.yml
```

### 单服务器模式（旧版）

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

### 方式一：使用 YAML 配置文件（推荐，支持多服务器）

1. 创建 `config.yaml` 配置文件（支持两种格式）：

**嵌套结构：**
```yaml
nacos:
  servers:
    - name: nacos-server1
      url: http://127.0.0.1:8848
      namespace-pattern: "*"
      read-only: false
    - name: nacos-server2
      url: http://127.0.0.1:8849
      namespace-pattern: "^(test|dev)$"
      read-only: true
```

**或扁平结构：**
```yaml
servers:
  - name: nacos-server1
    url: http://127.0.0.1:8848
    namespace-pattern: "*"
    read-only: false
  - name: nacos-server2
    url: http://127.0.0.1:8849
    namespace-pattern: "^(test|dev)$"
    read-only: true
```

2. 运行程序：

```bash
# Linux/Mac
./run.sh config.yaml /tmp/nacos-config

# Windows
run.bat config.yaml N:\

# 或直接运行
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar config.yaml /tmp/nacos-config
```

详细文档请参考 [MULTI_SERVER_QUICKSTART.md](MULTI_SERVER_QUICKSTART.md)

### 方式二：单服务器快速启动（旧版，不再推荐）

**注意**：此方式已过时，建议使用 YAML 配置文件方式。

```bash
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar
```

默认会连接到 `10.9.2.85:8848`，挂载到 `/tmp/nacos-config`（Linux/Mac）或相应目录。

### 方式三：命令行指定参数（旧版，不再推荐）

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

### 多服务器模式（推荐）

```bash
# 查看服务器列表
ls /tmp/nacos-config/
# 输出: nacos-server1  nacos-server2

# 查看某个服务器的命名空间
ls /tmp/nacos-config/nacos-server1/

# 查看配置内容
cat /tmp/nacos-config/nacos-server1/phoenix-test/DEFAULT_GROUP/application.properties

# 编辑配置（仅适用于非只读服务器）
vim /tmp/nacos-config/nacos-server1/phoenix-test/DEFAULT_GROUP/application.properties
```

### 单服务器模式（旧版）

#### 查看配置列表

```bash
ls /tmp/nacos-config/namespaces/
ls /tmp/nacos-config/namespaces/phoenix-test/
ls /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/
```

#### 读取配置内容

```bash
cat /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
```

#### 编辑配置

```bash
# 使用编辑器打开
vim /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties

# 保存后会自动同步到 Nacos Server
```

#### 卸载文件系统

```bash
# Linux/Mac
fusermount -u /tmp/nacos-config

# Windows
# 在文件资源管理器中右键点击挂载的驱动器，选择"弹出"
```

## 注意事项

### 多服务器模式

1. **配置文件**：确保 `config.yaml` 格式正确且存在
2. **命名空间过滤**：使用正则表达式灵活控制显示的命名空间
3. **只读保护**：设置为只读的服务器禁止写入、创建和删除操作
4. **缓存机制**：配置信息会被缓存，修改后可能需要短暂延迟

### 通用注意事项

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

**多服务器支持（新版）**：
- `Main.java`: 程序入口，加载 YAML 配置文件
- `MultiNacosConfig.java`: 多服务器配置模型
- `MultiNacosService.java`: 多服务器管理服务
- `NacosFuseFileSystem.java`: FUSE 文件系统实现
- `NacosService.java`: Nacos API 封装（单个服务器）
- `NacosConfig.java`: 配置数据模型

**单服务器模式（旧版）**：

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
