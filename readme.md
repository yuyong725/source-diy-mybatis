### 手抄Mybatis

#### TODO
1. invoker,factory,meta,resolver 分包搞清楚, 按设计模式

#### 说明
1. 对标线上的3.5.1-SNAPSHOT分支, 使用JDK8
2. 只支持Mysql，不做其他数据库类型的兼容
3. 不支持存储过程
4. 模块的划分与包划分不一定是完全一对一，我编写时视情况尽量一对一
5. 根据层级划分成3个大模块，而不是参照mybatis放在一起

#### 约定
1. 不能变的属性类对象使用`final`标记，比如`document`，工具类对象不需要
2. 严格按照方法实际的可作用区域，标记`private`等限制符
3. 对第三方工具（包括jdk和引入的其他jar）抛出的异常，尽量使用`exception`去捕捉，再抛出框架自定义的异常
4. 为空判断统一使用 `a == null`
5. 基于模块，为每个模块分配一个异常，若模块很大，再细分
6. 不允许更改方法名，避免对照mybatis源码时比对不上

#### 模块划分
* 基础支持层
    * 解析器：`parsing`
    * 反射：`reflection`
    * 异常：`exceptions`
    * 数据源：`datasource`
    * 缓存：`cache`
    * 资源加载：`io`
    * 事务管理：`transaction`
    * 日志：`logging`
    * Binding：`binding`
    * 类型转换：`type`
* 核心处理层
    * 配置解析：`builder`
    * 参数映射：`mapping`
    * SQL解析：`scripting`
    * SQL执行：`executor`,`cursor`
    * 结果集映射：`mapping`
    * 插件：`plugin`
* 接口层
    * 会话模块：`session`

#### 预计代码量与进度日志
> 每日上传最新截图
> 使用`find . -name "*.java"|xargs cat|grep -v -e ^$ -e ^\s*\/\/.*$|wc -l`统计每个包的代码量
 * 11.29 
    * 熬夜到凌晨三点，写完了`parsing`模块的主体逻辑，一共就五个类😄，成就满满，但着实担心速度
 * 11.30 
    * 一个`TypeParameterResolver`类就吃掉了六七个小时，没做过反射太吃力了，但不能放弃；
    * 手写代码速度太慢，更改策略，抄一波代码，再去翻译捋逻辑。
        * 肯定不能全抄，最好是从一个入口不断发散，这个明天再看
    * **代码手写的原则可以让步，但翻译一点都不能含糊，可以去掉一些代码，但保留的代码必须知道作用和调用链**
 * 12.2
    * 反射模块大体翻译完毕 ｜meta部分没翻，因为wrapper解析逻辑有点问题，先欠着
 * 12.3
    * 数据源模块大部分完成，越来越清晰
    * 日志模块只提供了slf4j的实现。很有意思，学习了框架如何对第三方日志框架的兼容
 * 12.4
    * 数据源模块剩下的一点完成
    * 事务模块完成，东西不多
    * 缓存模块类很多，但代码量不大，使用了装饰器模式
 *  12.5
    * 将所有类型的handler移到一个包下，避免与其他功能类混杂
![进度表](https://i.loli.net/2019/11/30/4ezrbNYBcADVT3j.png)

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
