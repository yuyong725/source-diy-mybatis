package cn.javadog.sd.mybatis.support.datasource.pooled;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.exceptions.ExceptionFactory;


/**
 * @author: 余勇
 * @date: 2019-12-03 00:03
 * 实现 InvocationHandler 接口，池化的 Connection 对象
 */
class PooledConnection implements InvocationHandler {

  /**
   * 关闭 Connection 方法名
   */
  private static final String CLOSE = "close";

  /**
   * JDK Proxy 的接口
   */
  private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };

  /**
   * 对象的标识，基于 {@link #realConnection} 求 hashCode
   */
  private final int hashCode;

  /**
   * 所属的 PooledDataSource 对象
   */
  private final PooledDataSource dataSource;

  /**
   * 真实的 Connection 连接
   */
  private final Connection realConnection;

  /**
   * 代理的 Connection 连接，即 {@link PooledConnection} 这个动态代理的 Connection 对象
   * note 这个类才是被代理后的类，而不是
   */
  private final Connection proxyConnection;

  /**
   * 从连接池中，获取走的时间戳
   */
  private long checkoutTimestamp;

  /**
   * 对象创建时间
   */
  private long createdTimestamp;

  /**
   * 最后更新时间
   */
  private long lastUsedTimestamp;

  /**
   * 连接的标识，即 {@link PooledDataSource#expectedConnectionTypeCode}
   */
  private int connectionTypeCode;

  /**
   * 是否有效
   */
  private boolean valid;

  /**
   * 构造函数
   * Constructor for SimplePooledConnection that uses the Connection and PooledDataSource passed in
   *
   * @param connection - 数据库连接，将被池化使用，通俗点说就是这个连接会交给连接池管理
   * @param dataSource - 数据源，connection就是来自于它
   */
  public PooledConnection(Connection connection, PooledDataSource dataSource) {
    this.hashCode = connection.hashCode();
    this.realConnection = connection;
    this.dataSource = dataSource;
    this.createdTimestamp = System.currentTimeMillis();
    this.lastUsedTimestamp = System.currentTimeMillis();
    this.valid = true;
    this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
  }

  /**
   * Invalidates the connection
   */
  /**
   * 标记连接非法
   */
  public void invalidate() {
    valid = false;
  }

  /**
   * Method to see if the connection is usable
   *
   * @return True if the connection is usable
   */
  public boolean isValid() {
    return valid && realConnection != null && dataSource.pingConnection(this);
  }

  /**
   * Getter for the *real* connection that this wraps
   *
   * @return The connection
   */
  public Connection getRealConnection() {
    return realConnection;
  }

  /**
   * Getter for the proxy for the connection
   *
   * @return The proxy
   */
  public Connection getProxyConnection() {
    return proxyConnection;
  }

  /**
   * Gets the hashcode of the real connection (or 0 if it is null)
   *
   * @return The hashcode of the real connection (or 0 if it is null)
   */
  public int getRealHashCode() {
    return realConnection == null ? 0 : realConnection.hashCode();
  }

  /**
   * Getter for the connection type (based on url + user + password)
   *
   * @return The connection type
   */
  public int getConnectionTypeCode() {
    return connectionTypeCode;
  }

  /**
   * Setter for the connection type
   *
   * @param connectionTypeCode - the connection type
   */
  public void setConnectionTypeCode(int connectionTypeCode) {
    this.connectionTypeCode = connectionTypeCode;
  }

  /**
   * Getter for the time that the connection was created
   *
   * @return The creation timestamp
   */
  public long getCreatedTimestamp() {
    return createdTimestamp;
  }

  /**
   * Setter for the time that the connection was created
   *
   * @param createdTimestamp - the timestamp
   */
  public void setCreatedTimestamp(long createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  /**
   * Getter for the time that the connection was last used
   *
   * @return - the timestamp
   */
  public long getLastUsedTimestamp() {
    return lastUsedTimestamp;
  }

  /**
   * Setter for the time that the connection was last used
   *
   * @param lastUsedTimestamp - the timestamp
   */
  public void setLastUsedTimestamp(long lastUsedTimestamp) {
    this.lastUsedTimestamp = lastUsedTimestamp;
  }

  /**
   * Getter for the time since this connection was last used
   *
   * @return - the time since the last use
   */
  public long getTimeElapsedSinceLastUse() {
    return System.currentTimeMillis() - lastUsedTimestamp;
  }

  /**
   * Getter for the age of the connection
   *
   * @return the age
   */
  public long getAge() {
    return System.currentTimeMillis() - createdTimestamp;
  }

  /**
   * Getter for the timestamp that this connection was checked out
   *
   * @return the timestamp
   */
  public long getCheckoutTimestamp() {
    return checkoutTimestamp;
  }

  /**
   * Setter for the timestamp that this connection was checked out
   *
   * @param timestamp the timestamp
   */
  public void setCheckoutTimestamp(long timestamp) {
    this.checkoutTimestamp = timestamp;
  }

  /**
   * Getter for the time that this connection has been checked out
   *
   * @return the time
   */
  public long getCheckoutTime() {
    return System.currentTimeMillis() - checkoutTimestamp;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * Allows comparing this connection to another
   *
   * @param obj - the other connection to test for equality
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PooledConnection) {
      return realConnection.hashCode() == ((PooledConnection) obj).realConnection.hashCode();
    } else if (obj instanceof Connection) {
      return hashCode == obj.hashCode();
    } else {
      return false;
    }
  }

  /**
   * 代理调用方法
   *
   * Required for InvocationHandler implementation.
   *
   * @param proxy  - not used
   * @param method - the method to be executed
   * @param args   - the parameters to be passed to the method
   * @see InvocationHandler#invoke(Object, Method, Object[])
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    // <1> 判断是否为 CLOSE 方法，则将连接放回到连接池中，避免连接被关闭
    if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
      dataSource.pushConnection(this);
      return null;
    } else {
      try {
        // <2.1> 判断非 Object 的方法，则先检查连接是否可用
        if (!Object.class.equals(method.getDeclaringClass())) {
          // issue #579 toString() should never fail
          // throw an SQLException instead of a Runtime
          checkConnection();
        }
        // <2.2> 反射调用对应的方法
        return method.invoke(realConnection, args);
      } catch (Throwable t) {
        throw ExceptionFactory.unwrapThrowable(t);
      }
    }
  }

  private void checkConnection() throws SQLException {
    if (!valid) {
      throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
    }
  }

}
