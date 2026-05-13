# 多Nacos服务器支持

## 概述

本项目现已支持同时挂载多个 Nacos 服务器，每个服务器可以配置不同的命名空间过滤规则和读写权限。

## 目录结构

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
    ├── _default/              # 默认命名空间
    │   └── DEFAULT_GROUP/
    │       ├── application.properties
    │       └── config.yml
    └── phoenix-test/          # 自定义命名空间
        └── DEFAULT_GROUP/
            └── application.yml
```

## 配置文件格式

创建 `config.yaml` 文件来配置多个 Nacos 服务器（支持两种格式）：

### 格式一：嵌套结构（推荐）

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

### 格式二：扁平结构

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

### 配置说明

- **name**: 服务器名称，将作为文件系统根目录的名称
- **url**: Nacos 服务器的地址
- **namespace-pattern**: 命名空间过滤的正则表达式模式
  - `"*"` 表示显示所有命名空间
  - `"^(test|dev)$"` 表示只显示 test 和 dev 命名空间
  - 可以使用任何有效的正则表达式
- **read-only**: 是否为只读模式
  - `false`: 允许读取和写入配置
  - `true`: 只允许读取配置，禁止写入、创建和删除操作

## 使用方法

### 1. 创建配置文件

根据上述格式创建 `config.yaml` 文件。

### 2. 运行程序

```bash
# Windows
run.bat config.yaml N:\

# Linux/Mac
./run.sh config.yaml /tmp/nacos_mnt
```

或者直接运行：

```bash
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar config.yaml /tmp/nacos_mnt
```

### 3. 访问配置

挂载成功后，可以通过文件系统访问各个 Nacos 服务器的配置：

```bash
# 查看服务器列表
ls /mount-point/

# 查看某个服务器的命名空间
ls /mount-point/nacos-server1/

# 查看某个命名空间的分组
ls /mount-point/nacos-server1/phoenix-test/

# 查看某个分组的配置文件
ls /mount-point/nacos-server1/phoenix-test/DEFAULT_GROUP/

# 读取配置文件内容
cat /mount-point/nacos-server1/phoenix-test/DEFAULT_GROUP/application.properties

# 编辑配置文件（仅适用于非只读服务器）
vim /mount-point/nacos-server1/phoenix-test/DEFAULT_GROUP/application.properties
```

## 功能特性

### 1. 多服务器支持

可以同时挂载多个 Nacos 服务器，每个服务器独立管理。

### 2. 命名空间过滤

通过正则表达式过滤每个服务器要显示的命名空间，便于管理和隔离。

### 3. 读写控制

可以为每个服务器设置只读或读写模式：
- **只读模式**: 禁止写入、创建和删除操作，保护重要配置
- **读写模式**: 允许完整的配置管理操作

### 4. 自动缓存

系统会自动缓存配置信息，减少网络请求，提高性能。

## 注意事项

1. **配置文件路径**: 默认在当前目录下查找 `config.yaml`，也可以通过命令行参数指定
2. **命名空间显示**: 默认命名空间在文件系统中显示为 `_default`
3. **只读限制**: 对于设置为只读的服务器，尝试写入、创建或删除文件时会返回错误
4. **正则表达式**: namespace-pattern 使用 Java 正则表达式语法
5. **缓存更新**: 修改配置后，缓存会自动更新，但可能需要短暂延迟

## 示例配置

### 示例 1: 开发和生产环境分离

```yaml
nacos:
  servers:
    - name: dev-environment
      url: http://dev-nacos.example.com:8848
      namespace-pattern: "^(dev|test)$"
      read-only: false
    - name: prod-environment
      url: http://prod-nacos.example.com:8848
      namespace-pattern: "^(prod)$"
      read-only: true
```

### 示例 2: 多个微服务配置中心

```yaml
nacos:
  servers:
    - name: user-service
      url: http://user-nacos.example.com:8848
      namespace-pattern: "*"
      read-only: false
    - name: order-service
      url: http://order-nacos.example.com:8848
      namespace-pattern: "*"
      read-only: false
    - name: payment-service
      url: http://payment-nacos.example.com:8848
      namespace-pattern: "*"
      read-only: true
```

## 故障排除

### 问题 1: 找不到配置文件

确保 `config.yaml` 文件存在且格式正确。运行时会显示配置文件的完整路径。

**支持的YAML格式：**
- 嵌套结构：`nacos.servers: [...]`
- 扁平结构：`servers: [...]`

两种格式都可以正常工作，系统会自动检测并解析。

### 问题 2: 无法连接 Nacos 服务器

检查配置文件中的 URL 是否正确，确保 Nacos 服务器正在运行且网络可达。

### 问题 3: 命名空间不显示

检查 `namespace-pattern` 正则表达式是否正确匹配目标命名空间。可以尝试使用 `"*"` 显示所有命名空间进行测试。

### 问题 4: 写入失败

确认服务器的 `read-only` 设置为 `false`，并且有足够的权限修改 Nacos 配置。
