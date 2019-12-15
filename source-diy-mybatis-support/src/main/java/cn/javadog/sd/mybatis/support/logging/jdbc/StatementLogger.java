package cn.javadog.sd.mybatis.support.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;


/**
 * @author 余勇
 * @date 2019-12-15 14:17
 * 
 * Statement 代理类
 */
public final class StatementLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * 要被代理的 statement
   */
  private final Statement statement;

  /**
   * 构造
   */
  private StatementLogger(Statement stmt, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.statement = stmt;
  }

  /**
   * 拦截增强
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
    try {
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }    
      if (EXECUTE_METHODS.contains(method.getName())) {
        if (isDebugEnabled()) {
          debug(" Executing: " + removeBreakingWhitespace((String) params[0]), true);
        }
        if ("executeQuery".equals(method.getName())) {
          ResultSet rs = (ResultSet) method.invoke(statement, params);
          return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
        } else {
          return method.invoke(statement, params);
        }
      } else if ("getResultSet".equals(method.getName())) {
        ResultSet rs = (ResultSet) method.invoke(statement, params);
        return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
      } else {
        return method.invoke(statement, params);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 获取被增强的 Statement 代理对象
   */
  public static Statement newInstance(Statement stmt, Log statementLog, int queryStack) {
    InvocationHandler handler = new StatementLogger(stmt, statementLog, queryStack);
    ClassLoader cl = Statement.class.getClassLoader();
    return (Statement) Proxy.newProxyInstance(cl, new Class[]{Statement.class}, handler);
  }

  /**
   * 获取 statement
   */
  public Statement getStatement() {
    return statement;
  }

}
