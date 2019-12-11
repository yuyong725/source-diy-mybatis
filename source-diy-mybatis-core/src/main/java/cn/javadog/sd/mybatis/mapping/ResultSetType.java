package cn.javadog.sd.mybatis.mapping;

import java.sql.ResultSet;

/**
 * @author: 余勇
 * @date: 2019-12-10 21:58
 *
 * 区别详情参见：https://juejin.im/post/5abb78a0f265da239e4e2331
 */
public enum ResultSetType {

  /**
   * 默认的，基于驱动，驱动默认啥行为就啥行为
   * @since 3.5.0
   */
  DEFAULT(-1),

  /**
   * 结果集的游标只能向下滚动
   */
  FORWARD_ONLY(ResultSet.TYPE_FORWARD_ONLY),

  /**
   * 结果集的游标可以上下移动，当数据库变化时，当前结果集不变
   */
  SCROLL_INSENSITIVE(ResultSet.TYPE_SCROLL_INSENSITIVE),

  /**
   * 返回可滚动的结果集，当数据库变化时，当前结果集同步改变
   */
  SCROLL_SENSITIVE(ResultSet.TYPE_SCROLL_SENSITIVE);

  private final int value;

  ResultSetType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
