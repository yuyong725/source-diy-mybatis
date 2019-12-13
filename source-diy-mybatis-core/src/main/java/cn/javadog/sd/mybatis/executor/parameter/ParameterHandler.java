package cn.javadog.sd.mybatis.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A parameter handler sets the parameters of the {@code PreparedStatement}
 *
 * @author Clinton Begin
 *
 * 参数处理器接口
 */
public interface ParameterHandler {

  /**
   * @return 参数对象
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
