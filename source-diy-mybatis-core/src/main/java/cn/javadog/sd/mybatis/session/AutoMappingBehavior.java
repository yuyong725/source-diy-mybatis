package cn.javadog.sd.mybatis.session;

/**
 * @author: 余勇
 * @date: 2019-12-11 13:10
 * 自动映射行为的枚举。
 * 自动映射：数据库字段与POJO类的字段的映射。
 * 用于指定MyBatis是否开启自动映射，以及开启的程度。
 * 可以参考：https://www.cnblogs.com/TheViper/p/4480765.html，不过写的也不是很详细
 */
public enum AutoMappingBehavior {

  /**
   * 禁用自动映射的功能
   */
  NONE,

  /**
   * 开启部分映射的功能。也就是请求的结果没有复杂的嵌套，比如属性中有collection
   */
  PARTIAL,

  /**
   * 开启全部映射的功能。即支持复杂的嵌套
   */
  FULL
}
