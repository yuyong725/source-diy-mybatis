### 手抄Mybatis

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

![进度表](https://i.loli.net/2019/11/30/NxOe8CTiI93yPoq.png)