package cn.javadog.sd.mybatis.mapping;

/**
 * @author 余勇
 * @date 2019-12-10 16:43
 *
 * Statement类型，移除存储过程CALLABLE
 */
public enum StatementType {

  /**
   * 普通的不带参的查询SQL；支持批量更新,批量删除；
   */
  STATEMENT,

  /**
   * 变参数的SQL,编译一次,执行多次,效率高; 安全性好，有效防止Sql注入等问题; 支持批量更新,批量删除;
   */
  PREPARED
}
