# 变更日志

## [2.0.0] - 2026-05-12

### 🎉 重大更新：多Nacos服务器支持

#### 新增功能

- **多服务器架构**：支持同时挂载多个 Nacos 服务器实例
- **YAML 配置管理**：使用 YAML 文件配置多个服务器
- **命名空间过滤**：通过正则表达式过滤每个服务器的命名空间
- **读写控制**：为每个服务器设置只读或读写模式
- **增强的目录结构**：`/{server-name}/{namespace}/{group}/{config_file}`

#### 新增文件

- `src/main/java/org/hyj/model/MultiNacosConfig.java` - 多服务器配置模型
- `src/main/java/org/hyj/service/MultiNacosService.java` - 多服务器管理服务
- `src/test/java/TestConfigParser.java` - 配置解析测试
- `config.yaml` - YAML 配置文件示例
- `MULTI_SERVER.md` - 多服务器详细文档
- `MULTI_SERVER_QUICKSTART.md` - 快速开始指南
- `IMPLEMENTATION_SUMMARY.md` - 实现总结
- `CHANGELOG.md` - 本文件

#### 修改文件

- `pom.xml` - 添加 Jackson YAML 依赖
- `src/main/java/org/hyj/fuse/NacosFuseFileSystem.java` - 重构为多服务器支持
- `src/main/java/org/hyj/Main.java` - 改为加载 YAML 配置
- `run.bat` - 更新启动脚本支持配置文件
- `run.sh` - 更新启动脚本支持配置文件
- `README.md` - 更新文档说明新功能
- `product.md` - 更新产品需求文档

#### 技术细节

- 使用 Jackson YAML 模块解析配置文件
- **支持两种YAML格式**：嵌套结构（`nacos.servers`）和扁平结构（`servers`）
- 通过正则表达式实现灵活的命名空间过滤
- 在写操作前检查服务器只读状态
- 使用复合键 `serverName:namespace` 进行缓存

#### 破坏性变更

⚠️ **此版本不向后兼容！**

命令行参数变更：
- **旧版**：`java -jar nacos_fuse.jar <server_addr> <mount_point>`
- **新版**：`java -jar nacos_fuse.jar <config_file> <mount_point>`

如果需要使用单服务器，请在配置文件中只配置一个服务器。

#### 迁移指南

从单服务器模式迁移到多服务器模式：

1. 创建 `config.yaml` 文件：
```yaml
nacos:
  servers:
    - name: my-server
      url: http://your-nacos-server:8848
      namespace-pattern: "*"
      read-only: false
```

2. 运行程序：
```bash
./run.sh config.yaml /tmp/nacos-config
```

#### 使用场景

1. **开发和生产环境分离**
   ```yaml
   nacos:
     servers:
       - name: dev
         url: http://dev-nacos:8848
         namespace-pattern: "^(dev|test)$"
         read-only: false
       - name: prod
         url: http://prod-nacos:8848
         namespace-pattern: "^prod$"
         read-only: true
   ```

2. **多微服务配置中心**
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
   ```

3. **跨区域部署**
   ```yaml
   nacos:
     servers:
       - name: beijing
         url: http://bj-nacos:8848
         namespace-pattern: "*"
         read-only: false
       - name: shanghai
         url: http://sh-nacos:8848
         namespace-pattern: "*"
         read-only: false
   ```

#### 已知限制

- 不支持配置文件热重载
- 缓存无过期机制
- 不支持 Nacos 用户名密码认证
- 无服务器健康检查

#### 未来计划

- [ ] 支持配置文件热重载
- [ ] 添加缓存过期机制
- [ ] 支持 Nacos 认证
- [ ] 添加服务器健康检查
- [ ] 支持动态添加/删除服务器
- [ ] 配置变更监听和自动刷新

---

## [1.0.0] - 2026-05-11

### 初始版本

#### 功能

- 单 Nacos 服务器支持
- 以 FUSE 文件系统形式挂载 Nacos 配置
- 目录结构：`/namespaces/{namespace}/{group}/{config_file}`
- 支持读取和写入配置
- 自动缓存配置信息

#### 技术栈

- Nacos SDK 2.2.4
- JNR-FUSE 0.5.8
- Java 8+
- Jackson 2.15.2
