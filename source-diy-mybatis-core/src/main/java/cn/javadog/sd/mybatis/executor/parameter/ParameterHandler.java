package cn.javadog.sd.mybatis.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author 余勇
 * @date 2019-12-15 17:28
 *
 * 参数处理器接口。用于给 {@code PreparedStatement} 设置参数
 */
public interface ParameterHandler {

  /**
   * 获取参数对象值
   */
  Object getParameterObject();


  /**
   * 设置 PreparedStatement 的占位符参数
   *
   * @param ps PreparedStatement 对象
   * @throws SQLException 发生 SQL 异常时
   */
  void setParameters(PreparedStatement ps) throws SQLException;

}
