package cn.javadog.sd.mybatis.mapping;

/**
 * @author: 余勇
 * @date: 2019-12-13 12:53
 *
 * 获取嵌套查询结果的方式
 */
public enum FetchType {

  /**
   * 懒加载
   */
  LAZY,

  /**
   * 立即加载
   */
  EAGER,

  /**
   * 使用全局默认值
   */
  DEFAULT
}
