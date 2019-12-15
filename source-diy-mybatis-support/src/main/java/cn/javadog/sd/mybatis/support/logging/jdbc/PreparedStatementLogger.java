package cn.javadog.sd.mybatis.support.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * @author 余勇
 * @date 2019-12-15 13:55
 *
 * PreparedStatement 的代理类，做的增强是打印日志
 */
public final class PreparedStatementLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * 被代理的 PreparedStatement 对象
   */
  private final PreparedStatement statement;

  /**
   * 构造函数
   */
  private PreparedStatementLogger(PreparedStatement stmt, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.statement = stmt;
  }

  /**
   * 执行被拦截的方法
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
    try {
      // Object 的方法，交由代理对象自己完成
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }
      // 执行的是 EXECUTE_METHODS 包含的方法，包括增删改查
      if (EXECUTE_METHODS.contains(method.getName())) {
        // 打印debug日志
        if (isDebugEnabled()) {
          debug("Parameters: " + getParameterValueString(), true);
        }
        // 清除列信息
        clearColumnInfo();
        if ("executeQuery".equals(method.getName())) {
          // 如果是查询方法，将结果 ResultSet 增强为 ResultSetLogger 返回
          ResultSet rs = (ResultSet) method.invoke(statement, params);
          return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
        } else {
          // 删增改方法，执行即可
          return method.invoke(statement, params);
        }
      } else if (SET_METHODS.contains(method.getName())) {
        // SET_METHODS 系列的方法，不做日志增强
        if ("setNull".equals(method.getName())) {
          setColumn(params[0], null);
        } else {
          setColumn(params[0], params[1]);
        }
        return method.invoke(statement, params);
      } else if ("getResultSet".equals(method.getName())) {
        // getResultSet 方法，将结果 ResultSet 增强为 ResultSetLogger 返回
        ResultSet rs = (ResultSet) method.invoke(statement, params);
        return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
      } else if ("getUpdateCount".equals(method.getName())) {
        // getUpdateCount 方法，打印日志
        int updateCount = (Integer) method.invoke(statement, params);
        if (updateCount != -1) {
          debug("   Updates: " + updateCount, false);
        }
        return updateCount;
      } else {
        // 其他方法，由 statement 完成
        return method.invoke(statement, params);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 获取日志增强的 PreparedStatement 代理对象
   */
  public static PreparedStatement newInstance(PreparedStatement stmt, Log statementLog, int queryStack) {
    InvocationHandler handler = new PreparedStatementLogger(stmt, statementLog, queryStack);
    ClassLoader cl = PreparedStatement.class.getClassLoader();
    return (PreparedStatement) Proxy.newProxyInstance(cl, new Class[]{PreparedStatement.class, CallableStatement.class}, handler);
  }

  /**
   * 获取 statement
   */
  public PreparedStatement getPreparedStatement() {
    return statement;
  }

}
