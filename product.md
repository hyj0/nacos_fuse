
# Table of Contents

1.  [依赖](#org948b548)
    1.  [nacos api](#org2995d45)
    2.  [java fuse](#org2522cb7)
2.  [实现将nacos配置文件挂载到本地](#org3ee6ca0)



<a id="org948b548"></a>

# 依赖


<a id="org2995d45"></a>

## nacos api

get namespace list
or GET <http://10.9.2.85:8848/nacos/v2/console/namespace/list>

    GET http://10.9.2.85:8848/nacos/v1/console/namespaces

response:
```json
{
  "code": 200,
  "message": null,
  "data": [
    {
      "namespace": "",
      "namespaceShowName": "public",
      "namespaceDesc": null,
      "quota": 200,
      "configCount": 12,
      "type": 0
    },
    {
      "namespace": "dev",
      "namespaceShowName": "devss",
      "namespaceDesc": "dev namespace",
      "quota": 200,
      "configCount": 80,
      "type": 2
    }
  ]
}
```


get config list

    GET http://10.9.2.85:8848/nacos/v1/cs/configs?dataId=&group=&appName=&config_tags=&pageNo=1&pageSize=3&tenant=phoenix-test&search=accurate
response:
```json
{
  "totalCount": 80,
  "pageNumber": 1,
  "pagesAvailable": 27,
  "pageItems": [
    {
      "id": "921571709375827968",
      "dataId": "devxx.properties",
      "group": "devxx",
      "content": "abc\n\"\\'",
      "md5": null,
      "encryptedDataKey": null,
      "tenant": "test",
      "appName": "",
      "type": "properties"
    },
    {
      "id": "921571709380022272",
      "dataId": "bb.properties",
      "group": "bb",
      "content": "",
      "md5": null,
      "encryptedDataKey": null,
      "tenant": "test",
      "appName": "",
      "type": "properties"
    }
  ]
}
```

get config content 使用java sdk， <https://nacos.io/docs/v2.4/manual/user/java-sdk/usage/?spm=5238cd80.1f77ca18.0.0.4d31e37evVYVyz>

    <properties>
        <!-- 2.1.2版本以上支持纯净版客户端 -->
        <nacos.version>2.4.2</nacos.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>com.alibaba.nacos</groupId>
            <artifactId>nacos-client</artifactId>
            <version>${nacos.version}</version>
            <!-- 指定纯净版SDK -->
            <classifier>pure</classifier>
        </dependency>
        <!-- 使用纯净版时必须要引入同版本nacos-api和nacos-common，否则可能出现运行时找不到类的问题 -->
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>nacos-common</artifactId>
            <version>${nacos.version}</version>
        </dependency>
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>nacos-api</artifactId>
            <version>${nacos.version}</version>
        </dependency>
    </dependencies>

    2. 初始化SDK
    Nacos 初始化SDK仅需要使用 NacosFactory 类进行不同模块的创建即可：
    
    String serverAddr = "localhost:8848";
    
    // 初始化配置中心的Nacos Java SDK
    ConfigService configService = NacosFactory.createConfigService(serverAddr);
    
    // 初始化配置中心的Nacos Java SDK
    NamingService namingService = NacosFactory.createNamingService(serverAddr);
    
    如果初始化SDK时，还需要配置一些参数，可以使用 Properties 类进行配置：
    
    Properties properties = new Properties();
    // 指定Nacos-Server的地址
    properties.setProperty(PropertyKeyConst.SERVER_ADDR, "localhost:8848");
    // 指定Nacos-SDK的命名空间
    properties.setProperty(PropertyKeyConst.NAMESPACE, "${namespaceId}");
    
    // 初始化配置中心的Nacos Java SDK
    ConfigService configService = NacosFactory.createConfigService(properties);
    
    // 初始化配置中心的Nacos Java SDK
    NamingService namingService = NacosFactory.createNamingService(properties);

获取配置

    
      public String getConfig(String dataId, String group, long timeoutMs) throws NacosException
    参数名	参数类型	描述
    dataId	string	配置 ID，采用类似 package.class（如com.taobao.tc.refund.log.level）的命名规则保证全局唯一性，class 部分建议是配置的业务含义。全部字符小写。只允许英文字符和 4 种特殊字符（”.”、”:”、”-”、”_”），不超过 256 字节。
    group	string	配置分组，建议填写产品名:模块名（Nacos
    ）保证唯一性，只允许英文字符和4种特殊字符（”.”、”:”、”-”、”_”），不超过128字节。
    timeout	long	读取配置超时时间，单位 ms，推荐值 3000。

发布配置

    发布配置
    描述
    用于通过程序自动发布 Nacos 配置，以便通过自动化手段降低运维成本。
    注意：创建和修改配置时使用的同一个发布接口，当配置不存在时会创建配置，当配置已存在时会更新配置。
    public boolean publishConfig(String dataId, String group, String content) throws NacosException;
    public boolean publishConfig(String dataId, String group, String content, String type) throws NacosException;
    ￼
    请求参数
    参数名
    参数类型
    描述
    dataId
    string
    配置 ID，采用类似 package.class（如 com.taobao.tc.refund.log.level）的命名规则保证全局唯一性。建议根据配置的业务含义来定义 class 部分。全部字符均为小写。只允许英文字符和 4 种特殊字符（“.”、“:”、“-”、“_”），不超过 256 字节。
    group
    string
    配置分组，建议填写产品名:模块名（如 Nacos:Test）来保证唯一性。只允许英文字符和 4 种特殊字符（“.”、“:”、“-”、“_”），不超过 128 字节。
    content
    string
    配置内容，不超过 100K 字节。
    type
    string
    @Since 1.4.1. 配置类型，见 com.alibaba.nacos.api.config.ConfigType，默认为TEXT
    返回参数
    参数类型
    描述
    boolean
    是否发布成功


<a id="org2522cb7"></a>

## java fuse

使用
<https://github.com/SerCeMan/jnr-fuse>

    <dependencies>
        <dependency>
            <groupId>com.github.serceman</groupId>
            <artifactId>jnr-fuse</artifactId>
            <version>0.5.8</version>
        </dependency>
    </dependencies>


<a id="org3ee6ca0"></a>

# 实现将nacos配置文件挂载到本地

目前：
目录结构
#### 路径映射
```
/namespaces/                                    → 根目录
/namespaces/{namespace}/                        → 命名空间目录
/namespaces/{namespace}/{group}/                → 分组目录
/namespaces/{namespace}/{group}/{config_file}   → 配置文件
```
### 目录结构示例

```
/mount-point/
└── namespaces/
    ├── _default/              # 默认命名空间
    │   └── DEFAULT_GROUP/
    │       ├── application.properties
    │       └── config.yml
    └── phoenix-test/          # 自定义命名空间
        └── DEFAULT_GROUP/
            └── application.yml
```
修改为支持多个nacos服务， 
目录结构改为：
```
/mount-point/
|── nacos-server1/
|   ├── _default/              # 默认命名空间
|   │   └── DEFAULT_GROUP/
|   │       ├── application.properties
|   │       └── config.yml
|   └── phoenix-test/          # 自定义命名空间
|       └── DEFAULT_GROUP/
|           └── application.yml
|           
└── nacos-server2/
    ├── _default/              # 默认命名空间
    │   └── DEFAULT_GROUP/
    │       ├── application.properties
    │       └── config.yml
    └── phoenix-test/          # 自定义命名空间
        └── DEFAULT_GROUP/
            └── application.yml
```
需要用yaml配置支持多个nacos服务
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

## 实现说明

### 核心类

1. **MultiNacosConfig**: 多服务器配置模型
   - `NacosServerConfig`: 单个服务器配置（包含 name, url, namespacePattern, readOnly）

2. **MultiNacosService**: 多服务器管理服务
   - 从 YAML 配置文件加载多个 Nacos 服务器配置
   - 管理多个 NacosService 实例
   - 支持命名空间过滤（通过正则表达式）
   - 支持只读模式控制

3. **NacosFuseFileSystem**: FUSE 文件系统实现
   - 新的目录结构: `/{server-name}/{namespace}/{group}/{config_file}`
   - 支持多服务器的配置读写
   - 对只读服务器进行写保护

### 使用方法

```bash
# 使用配置文件启动
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar config.yaml /tmp/nacos_mnt

# 或使用默认配置文件
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar
```

详细文档请参考 [MULTI_SERVER.md](MULTI_SERVER.md)

