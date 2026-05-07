# 使用示例

## 场景一：查看配置

### 查看所有命名空间
```bash
# Linux/Mac
ls /tmp/nacos-config/namespaces/

# Windows (在文件资源管理器中)
# 打开 N: 盘，进入 namespaces 文件夹
```

输出示例：
```
_default  phoenix-test
```

### 查看某个命名空间的分组
```bash
ls /tmp/nacos-config/namespaces/phoenix-test/
```

输出示例：
```
DEFAULT_GROUP  TEST_GROUP
```

### 查看配置文件列表
```bash
ls /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/
```

输出示例：
```
application.properties  config.yml  database.json
```

### 读取配置内容
```bash
cat /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
```

输出示例：
```properties
server.port=8080
spring.application.name=my-app
logging.level.root=INFO
```

## 场景二：编辑配置

### 使用 vim 编辑
```bash
vim /tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
```

修改内容：
```properties
# 修改前
server.port=8080

# 修改后
server.port=9090
```

保存后会自动同步到 Nacos Server。

### 使用记事本编辑（Windows）
```cmd
notepad N:\namespaces\phoenix-test\DEFAULT_GROUP\application.properties
```

## 场景三：批量查看配置

### 查看所有配置文件
```bash
find /tmp/nacos-config/namespaces -type f -name "*.properties" -exec echo "=== {} ===" \; -exec cat {} \;
```

### 搜索特定配置
```bash
grep -r "server.port" /tmp/nacos-config/namespaces/
```

## 场景四：备份配置

### 备份所有配置
```bash
# 创建备份目录
mkdir -p ~/nacos-backup

# 复制所有配置
cp -r /tmp/nacos-config/namespaces/* ~/nacos-backup/
```

### 备份特定命名空间
```bash
cp -r /tmp/nacos-config/namespaces/phoenix-test ~/nacos-backup/phoenix-test-backup-$(date +%Y%m%d)
```

## 场景五：对比配置

### 对比两个环境的配置
```bash
# 假设有两个挂载点
diff /tmp/nacos-config-dev/namespaces/phoenix-test/DEFAULT_GROUP/application.properties \
     /tmp/nacos-config-prod/namespaces/phoenix-test/DEFAULT_GROUP/application.properties
```

## 场景六：程序化访问

### Python 脚本读取配置
```python
#!/usr/bin/env python3
import os

mount_point = "/tmp/nacos-config"
namespace = "phoenix-test"
group = "DEFAULT_GROUP"
config_file = "application.properties"

config_path = os.path.join(mount_point, "namespaces", namespace, group, config_file)

with open(config_path, 'r') as f:
    content = f.read()
    print(content)
```

### Shell 脚本批量处理
```bash
#!/bin/bash

MOUNT_POINT="/tmp/nacos-config"
NAMESPACE="phoenix-test"
GROUP="DEFAULT_GROUP"

CONFIG_DIR="$MOUNT_POINT/namespaces/$NAMESPACE/$GROUP"

for config_file in "$CONFIG_DIR"/*; do
    if [ -f "$config_file" ]; then
        filename=$(basename "$config_file")
        echo "Processing: $filename"
        
        # 在这里添加你的处理逻辑
        # 例如：验证配置、转换格式等
    fi
done
```

## 场景七：监控配置变化

### 使用 inotify 监控（Linux）
```bash
#!/bin/bash

CONFIG_FILE="/tmp/nacos-config/namespaces/phoenix-test/DEFAULT_GROUP/application.properties"

inotifywait -m "$CONFIG_FILE" -e modify | while read path action file; do
    echo "[$(date)] Config changed: $file"
    # 发送通知或执行其他操作
    # curl -X POST http://your-webhook-url -d "Config updated"
done
```

## 实际应用场景

### 1. 开发环境快速切换配置
```bash
# 挂载开发环境
./run.sh dev-nacos.example.com:8848 /tmp/nacos-dev

# 挂载生产环境（只读）
./run.sh prod-nacos.example.com:8848 /tmp/nacos-prod

# 对比差异
diff /tmp/nacos-dev/namespaces/app/DEFAULT_GROUP/config.yml \
     /tmp/nacos-prod/namespaces/app/DEFAULT_GROUP/config.yml
```

### 2. CI/CD 集成
```bash
#!/bin/bash
# 在 CI/CD 流程中使用

# 挂载 Nacos
./run.sh $NACOS_SERVER /tmp/nacos &
FUSE_PID=$!

# 等待挂载完成
sleep 5

# 读取配置进行验证
CONFIG=$(cat /tmp/nacos/namespaces/$NAMESPACE/$GROUP/$CONFIG_FILE)

# 执行验证逻辑
if echo "$CONFIG" | grep -q "expected.value"; then
    echo "Configuration validated successfully"
else
    echo "Configuration validation failed"
    exit 1
fi

# 卸载
fusermount -u /tmp/nacos
kill $FUSE_PID
```

### 3. 配置审计
```bash
#!/bin/bash
# 定期审计配置变化

AUDIT_DIR="/var/log/nacos-audit"
mkdir -p "$AUDIT_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
AUDIT_FILE="$AUDIT_DIR/config_audit_$TIMESTAMP.log"

echo "Nacos Configuration Audit - $(date)" > "$AUDIT_FILE"
echo "================================" >> "$AUDIT_FILE"

for namespace_dir in /tmp/nacos-config/namespaces/*/; do
    namespace=$(basename "$namespace_dir")
    echo "Namespace: $namespace" >> "$AUDIT_FILE"
    
    for group_dir in "$namespace_dir"*/; do
        group=$(basename "$group_dir")
        
        for config_file in "$group_dir"*; do
            if [ -f "$config_file" ]; then
                filename=$(basename "$config_file")
                size=$(stat -f%z "$config_file" 2>/dev/null || stat -c%s "$config_file")
                echo "  - $group/$filename ($size bytes)" >> "$AUDIT_FILE"
            fi
        done
    done
done

echo "Audit completed: $AUDIT_FILE"
```

## 最佳实践

1. **只读访问**: 对于生产环境，建议以只读方式挂载
2. **权限控制**: 确保只有授权用户可以访问挂载点
3. **备份策略**: 定期备份重要配置
4. **变更管理**: 通过版本控制系统管理配置变更
5. **监控告警**: 监控配置变化和访问情况

---

更多问题请参考 [README.md](README.md) 和 [QUICKSTART.md](QUICKSTART.md)
