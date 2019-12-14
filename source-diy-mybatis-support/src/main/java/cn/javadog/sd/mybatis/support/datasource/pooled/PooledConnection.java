package cn.javadog.sd.mybatis.support.datasource.pooled;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.util.ExceptionUtil;


/**
 * @author 余勇
 * @date 2019-12-03 00:03
 * 实现 InvocationHandler 接口，池化的 Connection 对象
 */
class PooledConnection implements InvocationHandler {

  /**
   * 关闭 Connection的 方法名
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
   * 标记连接非法, 只是一个标记，判断合法还要看能否ping的通
   */
  public void invalidate() {
    valid = false;
  }

  /**
   * 判断连接是否可用
   */
  public boolean isValid() {
    return valid && realConnection != null && dataSource.pingConnection(this);
  }

  /**
   * 一些get方法
   */
  public Connection getRealConnection() {
    return realConnection;
  }

  public Connection getProxyConnection() {
    return proxyConnection;
  }

  public int getRealHashCode() {
    return realConnection == null ? 0 : realConnection.hashCode();
  }

  public int getConnectionTypeCode() {
    return connectionTypeCode;
  }

  public long getLastUsedTimestamp() {
    return lastUsedTimestamp;
  }

  public long getCreatedTimestamp() {
    return createdTimestamp;
  }

  public void setConnectionTypeCode(int connectionTypeCode) {
    this.connectionTypeCode = connectionTypeCode;
  }

  public long getCheckoutTimestamp() {
    return checkoutTimestamp;
  }

  /**
   * 一些set方法
   */
  public void setCreatedTimestamp(long createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  public void setLastUsedTimestamp(long lastUsedTimestamp) {
    this.lastUsedTimestamp = lastUsedTimestamp;
  }

  public void setCheckoutTimestamp(long timestamp) {
    this.checkoutTimestamp = timestamp;
  }

  /**
   * 获取连接多久没使用了
   */
  public long getTimeElapsedSinceLastUse() {
    return System.currentTimeMillis() - lastUsedTimestamp;
  }

  /**
   * 获取连接创建的时间
   */
  public long getAge() {
    return System.currentTimeMillis() - createdTimestamp;
  }

  /**
   * 获取连接使用的时间
   */
  public long getCheckoutTime() {
    return System.currentTimeMillis() - checkoutTimestamp;
  }

  /**
   * 重写hashcode
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * 重写equal，逻辑很简单
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
   * @param method - 要被执行的方法
   * @param args   - the parameters to be passed to the method
   * @see InvocationHandler#invoke(Object, Method, Object[])
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    // 判断是否为 CLOSE 方法，则将连接放回到连接池中，避免连接被关闭 note 标准的判断相等
    if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
      dataSource.pushConnection(this);
      return null;
    } else {
      try {
        // 判断非 Object 的方法，也就是Connection自己的方法，则先检查连接是否可用
        if (!Object.class.equals(method.getDeclaringClass())) {
          // 失败的话抛出SQLException，而不是runtime
          checkConnection();
        }
        // 反射调用对应的方法
        return method.invoke(realConnection, args);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    }
  }

  /**
   * 检查连接，这里只判断valid
   */
  private void checkConnection() throws SQLException {
    if (!valid) {
      throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
    }
  }

}
