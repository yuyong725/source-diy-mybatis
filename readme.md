### 手抄Mybatis
> 为了好看移除所有类的证书注释，侵权立删

#### TODO
1. invoker,factory,meta,resolver 分包搞清楚, 按设计模式

#### 说明
1. 对标线上的3.5.1-SNAPSHOT分支, 使用JDK8
2. 只支持Mysql，不做其他数据库类型的兼容
3. 不支持存储过程
4. 模块的划分与包划分不一定是完全一对一，我编写时视情况尽量一对一
5. 根据层级划分成3个大模块，而不是参照mybatis放在一起
6. 尽量不更改类名方法名，避免对照mybatis源码时比对不上，部分变量名为了更好理解会有改动
7. 所有异常统一抽离到一个包，工具类也抽离到一个包

#### 模块划分
> 内容基于[芋道源码](http://svip.iocoder.cn/MyBatis/)
* 基础支持层
    * 解析器：`parsing`
        * 对 XPath 进行封装，为 MyBatis 初始化时解析 mybatis-config.xml 配置文件以及映射配置文件提供支持
        * 另一个功能，是为处理动态 SQL 语句中的占位符提供支持
    * 反射：`reflection`
        * 对 Java 原生的反射进行了良好的封装，提了更加简洁易用的 API，方便上层使调用，并且对反射操作进行了一系列优化，例如缓存了类的元数据，提高了反射操作的性能
    * 异常：`exceptions`
        * 基础模块所有的异常类，源码中部分模块的异常分散在各自的模块，本项目几种到了一起
    * 数据源：`datasource`
        * 提供了相应的数据源实现，当然 MyBatis 也提供了与第三方数据源集成的接口，这些功能都位于数据源模块之中
    * 缓存：`cache`
        * MyBatis 中提供了一级缓存和二级缓存，而这两级缓存都是依赖于基础支持层中的缓 存模块实现的
        * TODO 貌似一级缓存不在这里！？
    * 资源加载：`io`
        * 资源加载模块，主要是对类加载器进行封装，确定类加载器的使用顺序，并提供了加载类文件以及其他资源文件的功能
    * 事务管理：`transaction`
        * MyBatis 对数据库中的事务进行了抽象，其自身提供了相应的事务接口和简单实现
        * 在很多场景中，MyBatis 会与 Spring 框架集成，并由 Spring 框架管理事务
    * 日志：`logging`
        * 集成第三方日志框架
    * Binding：`binding`
        * 将用户自定义的 Mapper 接口与映射配置文件关联起来
    * 类型转换：`type`
        * 为简化配置文件提供了别名机制
        * 实现 JDBC 类型与 Java 类型之间
    * 注解：`annotations` (功能上不是一个模块)
* 核心处理层
    * 配置解析：`builder`
    * 参数映射：`mapping`
    * SQL解析：`scripting`
    * SQL执行：`executor`,`cursor`
    * 结果集映射：`mapping`
    * 插件：`plugin`
* 接口层
    * 会话模块：`session`

#### 各个模块的删减部分
> 都是与主功能无关，且我不懂的地方，比如xml文件的DTD验证，反射的SecurityManager安全管理

##### 解析器模块
* `XPathParser`解析器的xml验证
    * 涉及`validation`与`entityResolver`属性
        * `validation`: xml格式校验是否开启，默认开启
        * `entityResolver`: xml文档头部指定的DTD或XSD文件，用于对像xml格式进行校验，但离线情况下下载不下来会出现xml校验失败的情况，通过此类实现使用本地DTD的效果
    
##### 反射模块
* `Reflector`等反射操作的安全验证
    * 源码可以参见mybatis的`Reflector#canControlMemberAccessible`方法
    
##### 数据源模块
* `JndiDataSourceFactory` 不支持

##### 日志模块
* `LogFactory` 只支持 slf4j
    * 加入其他日志的支持很简单，只是不想引入太多的jar
    
##### 缓存模块
* 移除`SerializedCache`，不支持可序列化的缓存，这块不是很懂
* 移除`SoftCache`和`WeakCache`，对虚引用和弱引用不是很懂

##### 类型转换模块
* 不再支持存储过程的类型转换
    * 因为我不用，哈哈

##### 文件加载模块
* 移除`ExternalResources`，源码里这个类已废弃
* 移除`JBoss6VFS`，不用JBoss，看起来也累

##### mapping模块
* 移除`DefaultDatabaseIdProvider`，本就已废弃

##### executor模块
* 移除`CglibProxyFactory`和`JavassistProxyFactory`，这俩已废弃。在各自包下有相同的