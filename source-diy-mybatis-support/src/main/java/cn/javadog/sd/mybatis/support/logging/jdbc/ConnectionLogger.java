package cn.javadog.sd.mybatis.support.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;


/**
 * @author 余勇
 * @date 2019-12-15 13:48
 *
 * 打印 Connection 相关的日志。是个代理类
 */
public final class ConnectionLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * 连接对象
   */
  private final Connection connection;

  /**
   * 构造函数
   */
  private ConnectionLogger(Connection conn, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.connection = conn;
  }

  /**
   * 执行被代理的方法
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params)
      throws Throwable {
    try {
      // 父类 Object 的方法
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }
      // 执行 prepareStatement 方法
      if ("prepareStatement".equals(method.getName())) {
        if (isDebugEnabled()) {
          // 打印日志
          debug(" Preparing: " + removeBreakingWhitespace((String) params[0]), true);
        }        
        PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
        stmt = PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;
      }
      // 执行 prepareCall 方法
      else if ("prepareCall".equals(method.getName())) {
        if (isDebugEnabled()) {
          debug(" Preparing: " + removeBreakingWhitespace((String) params[0]), true);
        }        
        PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
        stmt = PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;
      }
      // 执行 createStatement 方法
      else if ("createStatement".equals(method.getName())) {
        Statement stmt = (Statement) method.invoke(connection, params);
        stmt = StatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;
      }
      // 其他的方法，交给 connection 去完成，但不打印日志
      else {
        return method.invoke(connection, params);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 创建一个 connection 的代理对象，该代理对象做的增强是打印日志
   */
  public static Connection newInstance(Connection conn, Log statementLog, int queryStack) {
    InvocationHandler handler = new ConnectionLogger(conn, statementLog, queryStack);
    ClassLoader cl = Connection.class.getClassLoader();
    // 获取代理对象
    return (Connection) Proxy.newProxyInstance(cl, new Class[]{Connection.class}, handler);
  }

  /**
   * 获取被包装的 connection。这里拿的是未包装的原生connection
   *
   */
  public Connection getConnection() {
    return connection;
  }

}
