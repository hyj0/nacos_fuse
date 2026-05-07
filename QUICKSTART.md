# 快速开始指南

## 项目结构

```
nacos_fuse/
├── src/main/java/org/hyj/
│   ├── Main.java                          # 程序入口
│   ├── model/
│   │   └── NacosConfig.java               # 配置数据模型
│   ├── service/
│   │   └── NacosService.java              # Nacos API 封装
│   └── fuse/
│       └── NacosFuseFileSystem.java       # FUSE 文件系统实现
├── pom.xml                                # Maven 配置文件
├── README.md                              # 详细文档
├── QUICKSTART.md                          # 快速开始（本文件）
├── run.bat                                # Windows 启动脚本
└── run.sh                                 # Linux/Mac 启动脚本
```

## 快速使用步骤

### 1. 环境准备

#### Windows 用户
- 安装 JDK 8 或更高版本
- 安装 Maven
- **安装 WinFsp**（必需）: https://winfsp.dev/rel/

#### Linux 用户
```bash
# Ubuntu/Debian
sudo apt-get install openjdk-8-jdk maven libfuse-dev

# CentOS/RHEL
sudo yum install java-1.8.0-openjdk-devel maven fuse-devel
```

#### macOS 用户
```bash
brew install openjdk@8 maven macfuse
```

### 2. 编译和运行

#### 方式一：使用启动脚本（推荐）

**Windows:**
```cmd
run.bat
```
或使用自定义参数：
```cmd
run.bat 10.9.2.85:8848 N:
```

**Linux/Mac:**
```bash
chmod +x run.sh
./run.sh
```
或使用自定义参数：
```bash
./run.sh 10.9.2.85:8848 /mnt/nacos
```

#### 方式二：手动编译运行

```bash
# 编译
mvn clean package

# 运行
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar [nacos-server] [mount-point]
```

### 3. 访问配置

挂载成功后，可以通过文件系统访问 Nacos 配置：

**Windows:**
- 打开"此电脑"，找到新挂载的驱动器（如 N:）
- 浏览目录：`N:\namespaces\phoenix-test\DEFAULT_GROUP\`

**Linux/Mac:**
```bash
# 查看命名空间
ls /tmp/nacos-config/namespaces/

# 查看某个命名空间的分组
ls /tmp/nacos-config/namespaces/phoenix-test/

# 查看配置文件
ls /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/

# 读取配置内容
cat /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
```

### 4. 编辑配置

直接使用任何文本编辑器编辑配置文件，保存后会自动同步到 Nacos Server：

```bash
vim /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
# 或
nano /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
```

### 5. 卸载文件系统

**Windows:**
- 在文件资源管理器中右键点击挂载的驱动器
- 选择"弹出"或"断开连接"

**Linux/Mac:**
```bash
fusermount -u /tmp/nacos-config
# 或
umount /tmp/nacos-config
```

## 常见问题

### Q1: 挂载失败，提示 "Cannot load library"
**A:** 确保已正确安装 FUSE/WinFsp/macFUSE

### Q2: 看不到配置文件
**A:** 
- 检查 Nacos Server 地址是否正确
- 检查网络连接
- 当前版本只会显示已存在的配置，如果配置不存在则不会显示

### Q3: 编辑后保存失败
**A:**
- 检查是否有 Nacos Server 的写入权限
- 检查配置文件格式是否正确
- 查看控制台错误信息

### Q4: 新增的配置不显示
**A:** 当前版本会缓存配置列表，需要重启程序才能看到新配置

## 下一步

- 阅读 [README.md](README.md) 了解更多详细信息
- 查看源码了解实现细节
- 根据需求扩展功能

## 技术支持

如有问题，请检查：
1. Java 版本是否正确（8+）
2. Maven 是否正确安装
3. FUSE/WinFsp 是否已安装
4. Nacos Server 是否可访问
5. 查看控制台输出的错误信息
