# YAML 配置格式说明

## 支持的格式

Nacos FUSE 支持两种 YAML 配置文件格式，系统会自动检测并解析。

### 格式一：嵌套结构（推荐）

```yaml
nacos:
  servers:
    - name: local-nacos
      url: http://10.9.2.85:8848/
      namespace-pattern: ".*phoenix.*"
      read-only: false
    - name: pdc-nacos
      url: http://10.9.2.85:8848
      namespace-pattern: "^(test|dev)$"
      read-only: true
```

**优点：**
- 结构清晰，层次分明
- 便于未来扩展其他配置项
- 符合常见的配置文件规范

### 格式二：扁平结构

```yaml
servers:
  - name: local-nacos
    url: http://10.9.2.85:8848/
    namespace-pattern: ".*phoenix.*"
    read-only: false
  - name: pdc-nacos
    url: http://10.9.2.85:8848
    namespace-pattern: "^(test|dev)$"
    read-only: true
```

**优点：**
- 简洁直接
- 减少嵌套层级

## 配置项说明

### 服务器配置参数

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| `name` | String | 是 | 服务器名称，作为文件系统目录名 | `local-nacos` |
| `url` | String | 是 | Nacos 服务器地址 | `http://10.9.2.85:8848` |
| `namespace-pattern` | String | 否 | 命名空间过滤正则表达式 | `"*"`, `".*phoenix.*"` |
| `read-only` | Boolean | 否 | 是否只读模式，默认 `false` | `true` / `false` |

### 命名空间过滤示例

- `"*"` - 显示所有命名空间
- `".*phoenix.*"` - 显示包含 "phoenix" 的命名空间
- `"^(test|dev)$"` - 只显示 test 和 dev 命名空间
- `"^prod.*$"` - 显示所有以 prod 开头的命名空间
- `"^(?!public$).*$"` - 排除 public 命名空间

## 完整示例

```yaml
nacos:
  servers:
    # 开发环境 - 可读写
    - name: dev-server
      url: http://dev-nacos.example.com:8848
      namespace-pattern: "^(dev|test|staging)$"
      read-only: false
    
    # 生产环境 - 只读保护
    - name: prod-server
      url: http://prod-nacos.example.com:8848
      namespace-pattern: "^prod.*$"
      read-only: true
    
    # Phoenix 项目专用
    - name: phoenix-nacos
      url: http://phoenix-nacos.example.com:8848
      namespace-pattern: ".*phoenix.*"
      read-only: false
```

## 常见问题

### Q: 我应该使用哪种格式？

A: 推荐使用**嵌套结构**，因为：
1. 结构更清晰
2. 便于未来扩展
3. 符合主流配置文件规范

但两种格式功能完全相同，可以根据个人喜好选择。

### Q: 可以混用两种格式吗？

A: 不可以。每个配置文件只能使用一种格式。

### Q: 如果配置错误会怎样？

A: 程序启动时会显示详细的错误信息，包括：
- 文件路径
- 错误类型
- 具体位置（行号、列号）

根据错误信息修正配置文件即可。

### Q: 如何验证配置文件是否正确？

A: 可以使用在线 YAML 验证工具，或者运行程序查看是否有解析错误。

## 技术实现

系统通过以下步骤解析 YAML 配置：

1. 读取整个 YAML 文件为 Map 结构
2. 检查是否存在顶层 `nacos` 键
3. 如果存在，提取 `nacos` 下的内容
4. 如果不存在，直接使用根级别内容
5. 将提取的内容转换为 `MultiNacosConfig` 对象

这种设计使得系统可以兼容两种不同的 YAML 格式，提供更好的用户体验。
