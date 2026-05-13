# 多Nacos服务器支持 - 快速开始

## 新增功能

本项目现已支持同时挂载多个 Nacos 服务器！主要特性包括：

✅ **多服务器支持** - 同时挂载多个 Nacos 服务器实例  
✅ **命名空间过滤** - 通过正则表达式过滤显示的命名空间  
✅ **读写控制** - 为每个服务器设置只读或读写模式  
✅ **YAML配置** - 使用简洁的 YAML 文件进行配置  

## 快速开始

### 1. 创建配置文件

在项目根目录创建 `config.yaml` 文件（支持两种格式）：

**格式一：嵌套结构（推荐）**
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

**格式二：扁平结构**
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

### 2. 运行程序

**Windows:**
```bash
run.bat config.yaml N:\
```

**Linux/Mac:**
```bash
./run.sh config.yaml /tmp/nacos_mnt
```

### 3. 访问配置

挂载成功后，文件系统结构如下：

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

## 配置说明

### 服务器配置参数

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `name` | String | 服务器名称（作为目录名） | `nacos-server1` |
| `url` | String | Nacos 服务器地址 | `http://127.0.0.1:8848` |
| `namespace-pattern` | String | 命名空间过滤正则表达式 | `"*"`, `"^(test|dev)$"` |
| `read-only` | Boolean | 是否只读模式 | `true` / `false` |

### 命名空间过滤示例

- `"*"` - 显示所有命名空间
- `"^(test|dev)$"` - 只显示 test 和 dev 命名空间
- `"^prod.*$"` - 显示所有以 prod 开头的命名空间
- `"^(?!public$).*$"` - 排除 public 命名空间

## 使用场景

### 场景1: 开发和生产环境分离

```yaml
nacos:
  servers:
    - name: dev-environment
      url: http://dev-nacos.example.com:8848
      namespace-pattern: "^(dev|test)$"
      read-only: false
    - name: prod-environment
      url: http://prod-nacos.example.com:8848
      namespace-pattern: "^prod$"
      read-only: true
```

### 场景2: 多微服务配置中心

```yaml
nacos:
  servers:
    - name: user-service
      url: http://user-nacos:8848
      namespace-pattern: "*"
      read-only: false
    - name: order-service
      url: http://order-nacos:8848
      namespace-pattern: "*"
      read-only: false
    - name: payment-service
      url: http://payment-nacos:8848
      namespace-pattern: "*"
      read-only: true  # 支付配置只读保护
```

### 场景3: 跨区域部署

```yaml
nacos:
  servers:
    - name: beijing-datacenter
      url: http://bj-nacos:8848
      namespace-pattern: "*"
      read-only: false
    - name: shanghai-datacenter
      url: http://sh-nacos:8848
      namespace-pattern: "*"
      read-only: false
```

## 操作示例

### 查看服务器列表
```bash
ls /mount-point/
# 输出: nacos-server1  nacos-server2
```

### 查看配置内容
```bash
cat /mount-point/nacos-server1/phoenix-test/DEFAULT_GROUP/application.properties
```

### 修改配置（仅非只读服务器）
```bash
vim /mount-point/nacos-server1/phoenix-test/DEFAULT_GROUP/application.properties
# 保存后自动同步到 Nacos 服务器
```

### 只读保护测试
```bash
# 尝试修改只读服务器的配置会失败
echo "test=value" > /mount-point/nacos-server2/_default/DEFAULT_GROUP/config.yml
# 错误: Read-only file system
```

## 注意事项

⚠️ **重要提示：**

1. **配置文件路径**: 默认在当前目录查找 `config.yaml`，也可通过命令行参数指定
2. **命名空间显示**: 默认命名空间在文件系统中显示为 `_default`
3. **只读限制**: 只读服务器禁止写入、创建和删除操作
4. **正则表达式**: 使用 Java 正则表达式语法
5. **缓存机制**: 系统自动缓存配置信息以提高性能

## 故障排除

### 问题：找不到配置文件
**解决方案：** 确保 `config.yaml` 文件存在且格式正确

### 问题：无法连接 Nacos 服务器
**解决方案：** 检查 URL 是否正确，确保 Nacos 服务器正在运行

### 问题：命名空间不显示
**解决方案：** 检查 `namespace-pattern` 正则表达式，尝试使用 `"*"` 测试

### 问题：写入失败
**解决方案：** 确认服务器的 `read-only` 设置为 `false`

## 更多信息

详细文档请参考 [MULTI_SERVER.md](MULTI_SERVER.md)
