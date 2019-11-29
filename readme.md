### 手抄Mybatis

#### 说明
1. 对标线上的3.5.1-SNAPSHOT分支
2. 只支持Mysql，不做其他数据库类型的兼容
3. 不支持存储过程
4. 模块的划分与包划分不一定是完全一对一，我编写时视情况尽量一对一


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
![image.png](https://i.loli.net/2019/11/29/vCVDkQy1fThXU5L.png)