package cn.javadog.sd.mybatis.support.datasource.pooled;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import cn.javadog.sd.mybatis.support.datasource.unpooled.UnpooledDataSource;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * This is a simple, synchronous, thread-safe database connection pool.
 *
 * @author 余勇
 * @date 2019-12-03 21:05
 *
 * 实现 DataSource 接口，池化的 DataSource 实现类。这是个简单，线程安全的实现，麻雀虽小，五脏俱全
 * 实际场景下，我们基本不用 MyBatis 自带的数据库连接池的实现！！！
 *
 * note 属性很多，没必要记与理解，看懂源码就像
 */
public class PooledDataSource implements DataSource {

  private static final Log log = LogFactory.getLog(PooledDataSource.class);

  /**
   * PoolState 对象，记录池化的状态
   */
  private final PoolState state = new PoolState(this);

  /**
   * UnpooledDataSource 对象，其实PooledDataSource就是对UnpooledDataSource的增强！
   */
  private final UnpooledDataSource dataSource;

  // 下面使是一些可选的配置字段
  /**
   * 在任意时间可以存在的活动（也就是正在使用）连接数量
   */
  protected int poolMaximumActiveConnections = 10;

  /**
   * 任意时间可能存在的空闲连接数
   */
  protected int poolMaximumIdleConnections = 5;

  /**
   * 在被强制返回之前，池中连接被检出（checked out）时间。单位：毫秒
   */
  protected int poolMaximumCheckoutTime = 20000;

  /**
   * 这是一个底层设置，如果获取连接花费了相当长的时间，连接池会打印状态日志并重新尝试获取一个连接（避免在误配置的情况下一直安静的失败）。单位：毫秒
   */
  protected int poolTimeToWait = 20000;

  /**
   * 这是一个关于坏连接容忍度的底层设置，作用于每一个尝试从缓存池获取连接的线程.
   * 如果这个线程获取到的是一个坏的连接，那么这个数据源允许这个线程尝试重新获取一个新的连接，但是这个重新尝试的次数不应该超过 poolMaximumIdleConnections 与 poolMaximumLocalBadConnectionTolerance 之和。
   */
  protected int poolMaximumLocalBadConnectionTolerance = 3;

  /**
   * 发送到数据库的侦测查询，用来检验连接是否正常工作并准备接受请求。
   */
  protected String poolPingQuery = "NO PING QUERY SET";

  /**
   * 是否启用侦测查询。若开启，需要设置 poolPingQuery 属性为一个可执行的 SQL 语句（最好是一个速度非常快的 SQL 语句）
   */
  protected boolean poolPingEnabled;

  /**
   * 配置 poolPingQuery 的频率。可以被设置为和数据库连接超时时间一样，来避免不必要的侦测，默认值：0（即所有连接每一时刻都被侦测 — 当然仅当 poolPingEnabled 为 true 时适用）
   */
  protected int poolPingConnectionsNotUsedFor;

  /**
   * 期望 Connection 的类型编码，通过 {@link #assembleConnectionTypeCode(String, String, String)} 计算。
   */
  private int expectedConnectionTypeCode;

  public PooledDataSource() {
    dataSource = new UnpooledDataSource();
  }

  /**
   * 几种构造，说白了是先创建UnpooledDataSource，核心连接的逻辑都在 UnpooledDataSource 里面
   */
  public PooledDataSource(UnpooledDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public PooledDataSource(String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    // 创建 UnpooledDataSource 对象，这样，就能重用 UnpooledDataSource 的代码了。说白了，获取真正连接的逻辑，还是在 UnpooledDataSource 中实现。
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
    // 计算  expectedConnectionTypeCode 的值
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 获得 Connection 连接
   */
  @Override
  public Connection getConnection() throws SQLException {
    return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return popConnection(username, password).getProxyConnection();
  }

  /**
   * get/set 超时时间和打印器，来自CommonDataSource接口的方法
   */
  @Override
  public void setLoginTimeout(int loginTimeout) throws SQLException {
    DriverManager.setLoginTimeout(loginTimeout);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    DriverManager.setLogWriter(logWriter);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return DriverManager.getLogWriter();
  }

  /**
   * 字段的 set 方法，set完都要forceCloseAll 强制关闭所有连接池的连接
   */
  public void setDriver(String driver) {
    dataSource.setDriver(driver);
    forceCloseAll();
  }

  public void setUrl(String url) {
    dataSource.setUrl(url);
    forceCloseAll();
  }

  public void setUsername(String username) {
    dataSource.setUsername(username);
    forceCloseAll();
  }

  public void setPassword(String password) {
    dataSource.setPassword(password);
    forceCloseAll();
  }

  public void setDefaultAutoCommit(boolean defaultAutoCommit) {
    dataSource.setAutoCommit(defaultAutoCommit);
    forceCloseAll();
  }

  public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
    dataSource.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
    forceCloseAll();
  }

  public void setDriverProperties(Properties driverProps) {
    dataSource.setDriverProperties(driverProps);
    forceCloseAll();
  }

  public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
    this.poolMaximumActiveConnections = poolMaximumActiveConnections;
    forceCloseAll();
  }

  public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
    this.poolMaximumIdleConnections = poolMaximumIdleConnections;
    forceCloseAll();
  }

  public void setPoolMaximumLocalBadConnectionTolerance(
      int poolMaximumLocalBadConnectionTolerance) {
    this.poolMaximumLocalBadConnectionTolerance = poolMaximumLocalBadConnectionTolerance;
  }

  public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
    this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
    forceCloseAll();
  }

  public void setPoolTimeToWait(int poolTimeToWait) {
    this.poolTimeToWait = poolTimeToWait;
    forceCloseAll();
  }

  public void setPoolPingQuery(String poolPingQuery) {
    this.poolPingQuery = poolPingQuery;
    forceCloseAll();
  }

  public void setPoolPingEnabled(boolean poolPingEnabled) {
    this.poolPingEnabled = poolPingEnabled;
    forceCloseAll();
  }

  public void setPoolPingConnectionsNotUsedFor(int milliseconds) {
    this.poolPingConnectionsNotUsedFor = milliseconds;
    forceCloseAll();
  }

  /**
   * 字段的get方法
   */
  public String getDriver() {
    return dataSource.getDriver();
  }

  public String getUrl() {
    return dataSource.getUrl();
  }

  public String getUsername() {
    return dataSource.getUsername();
  }

  public String getPassword() {
    return dataSource.getPassword();
  }

  public boolean isAutoCommit() {
    return dataSource.isAutoCommit();
  }

  public Integer getDefaultTransactionIsolationLevel() {
    return dataSource.getDefaultTransactionIsolationLevel();
  }

  public Properties getDriverProperties() {
    return dataSource.getDriverProperties();
  }

  public int getPoolMaximumActiveConnections() {
    return poolMaximumActiveConnections;
  }

  public int getPoolMaximumIdleConnections() {
    return poolMaximumIdleConnections;
  }

  public int getPoolMaximumLocalBadConnectionTolerance() {
    return poolMaximumLocalBadConnectionTolerance;
  }

  public int getPoolMaximumCheckoutTime() {
    return poolMaximumCheckoutTime;
  }

  public int getPoolTimeToWait() {
    return poolTimeToWait;
  }

  public String getPoolPingQuery() {
    return poolPingQuery;
  }

  public boolean isPoolPingEnabled() {
    return poolPingEnabled;
  }

  public int getPoolPingConnectionsNotUsedFor() {
    return poolPingConnectionsNotUsedFor;
  }

  public PoolState getPoolState() {
    return state;
  }

  /**
   * 关闭所有的 activeConnections 和 idleConnections 的连接
   */
  public void forceCloseAll() {
    // 上锁 TODO 为毛上锁
    synchronized (state) {
      // 计算 expectedConnectionTypeCode
      expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
      // 遍历 activeConnections ，进行关闭，倒序，这样一边遍历一边删除就不会出事
      for (int i = state.activeConnections.size(); i > 0; i--) {
        try {
          // 从activeConnections中移除连接
          PooledConnection conn = state.activeConnections.remove(i - 1);
          // 将连接设置为失效
          conn.invalidate();

          // 回滚事务，不是自动提交的就回滚
          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          // 关闭真实的连接
          realConn.close();
        } catch (Exception e) {
          // 这阶段出错不刁
        }
      }
      // 遍历 idleConnections ，进行关闭， 实现代码上，和上面是一样的】
      for (int i = state.idleConnections.size(); i > 0; i--) {
        try {
          // 设置为失效
          PooledConnection conn = state.idleConnections.remove(i - 1);
          conn.invalidate();

          // 回滚事务，，不是自动提交的就回滚
          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          // 关闭真实的连接
          realConn.close();
        } catch (Exception e) {
          // 这阶段出错不刁
        }
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("PooledDataSource forcefully closed/removed all connections.");
    }
  }

  /**
   * 组装连接类型码，JVM同一个对象的hashCode肯定一致，string会重用对象
   */
  private int assembleConnectionTypeCode(String url, String username, String password) {
    return ("" + url + username + password).hashCode();
  }

  /**
   * 将使用完的连接，添加回连接池中
   */
  protected void pushConnection(PooledConnection conn) throws SQLException {
    // 上锁，确实要上锁
    synchronized (state) {
      // 从激活的连接集合中移除该连接
      state.activeConnections.remove(conn);
      // 首先判断连接是否有效
      if (conn.isValid()) {
        // 判断是否超过空闲连接上限，并且和当前连接池的标识匹配
        if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
          // 统计连接使用时长
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          // 回滚事务，避免适用方未提交或者回滚事务
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          // 创建新的 PooledConnection 对象，并添加到空闲的链接集合中；新的 PooledConnection 与当前相比就是重置了连接时间
          PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
          state.idleConnections.add(newConn);
          // 将这俩时间都设置回去了，创建新的 PooledConnection 的意义在哪里？见下面👇
          newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
          newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
          // 设置原连接失效，为什么这里要创建新的 PooledConnection 对象呢？避免使用方还在使用 conn（也就是调用pushConnection后还在用
          // 通过将它设置为失效，万一再次调用，会抛出异常
          conn.invalidate();
          if (log.isDebugEnabled()) {
            log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
          }
          // 唤醒正在等待连接的线程，用了锁这一步就不要忘
          state.notifyAll();
        } else {
          // 连接达到了上限，或者说就不归这个datasource管，就将连接关了完事
          // 统计连接使用时长
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          // 回滚事务，避免使用者未提交或者回滚事务
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          // 关闭真正的数据库连接！！！没有重复回收，相当于这个连接真正的GG了
          conn.getRealConnection().close();
          if (log.isDebugEnabled()) {
            log.debug("Closed connection " + conn.getRealHashCode() + ".");
          }
          // 设置原连接失效
          conn.invalidate();
        }
      } else {
        // 失效的连接，不刁它
        if (log.isDebugEnabled()) {
          log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
        }
        // 统计获取到坏的连接的次数
        state.badConnectionCount++;
      }
    }
  }

  /**
   * 从池中获取 PooledConnection 对象，代码略长，但逻辑很清晰的
   */
  private PooledConnection popConnection(String username, String password) throws SQLException {
    // 标记，获取连接时，是否进行了等待
    boolean countedWait = false;
    // 最终获取到的链接对象
    PooledConnection conn = null;
    // 记录当前时间
    long t = System.currentTimeMillis();
    // 记录当前方法，获取到坏连接的次数
    int localBadConnectionCount = 0;

    // 循环，获取可用的 Connection 连接
    while (conn == null) {
      synchronized (state) {
        // 空闲连接非空
        if (!state.idleConnections.isEmpty()) {
          // 通过移除的方式，获得首个空闲的连接
          conn = state.idleConnections.remove(0);
          if (log.isDebugEnabled()) {
            log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
          }
        } else {
          // 无空闲空闲连接
          // 激活的连接数小于 poolMaximumActiveConnections
          if (state.activeConnections.size() < poolMaximumActiveConnections) {
            // 创建新的 PooledConnection 连接对象
            conn = new PooledConnection(dataSource.getConnection(), this);
            if (log.isDebugEnabled()) {
              log.debug("Created connection " + conn.getRealHashCode() + ".");
            }
          } else {
            // 激活的连接达到上限，从头找超时的连接
            // 获得首个激活的 PooledConnection 对象
            PooledConnection oldestActiveConnection = state.activeConnections.get(0);
            // 检查该连接是否超时
            long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
            // 检查到超时
            if (longestCheckoutTime > poolMaximumCheckoutTime) {
              // 对连接超时的时间的统计
              state.claimedOverdueConnectionCount++;
              state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
              state.accumulatedCheckoutTime += longestCheckoutTime;
              // 从活跃的连接集合中移除
              state.activeConnections.remove(oldestActiveConnection);
              // 如果非自动提交的，需要进行回滚。即将原有执行中的事务，全部回滚。
              if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
                try {
                  oldestActiveConnection.getRealConnection().rollback();
                } catch (SQLException e) {
                  /*
                     Just log a message for debug and continue to execute the following
                     statement like nothing happened.
                     Wrap the bad connection with a new PooledConnection, this will help
                     to not interrupt current executing thread and give current thread a
                     chance to join the next competition for another valid/good database
                     connection. At the end of this loop, bad {@link @conn} will be set as null.
                   */
                  /**
                   * note 之前的rollback都没有catch，也就是说抛错了直接嗝屁；而就这里catch了但只是打个日志，然后就像啥也没发生一样继续往下走。
                   *  然后将坏掉的连接继续包在一个新的PooledConnection里面。这样做为了不打断当前正在进行的线程，并给当前线程一个机会，去参与
                   *  另外一个有效的连接的竞争。最后，再将坏掉的连接设为null
                   */
                  //
                  log.debug("Bad connection. Could not roll back");
                }  
              }
              // 创建新的 PooledConnection 连接对象
              conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
              conn.setCreatedTimestamp(oldestActiveConnection.getCreatedTimestamp());
              conn.setLastUsedTimestamp(oldestActiveConnection.getLastUsedTimestamp());
              // 设置 oldestActiveConnection 为无效
              oldestActiveConnection.invalidate();
              if (log.isDebugEnabled()) {
                log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
              }
            } else {
              // 检查到未超时，必须等待
              try {
                // 对等待连接进行统计。通过 countedWait 标识，在这个循环中，只记录一次！。
                if (!countedWait) {
                  state.hadToWaitCount++;
                  countedWait = true;
                }
                if (log.isDebugEnabled()) {
                  log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                }
                // 记录当前时间
                long wt = System.currentTimeMillis();
                // 等待，直到超时，或 pingConnection/pushConnection 方法中归还连接时的唤醒
                state.wait(poolTimeToWait);
                // 统计等待连接的时间
                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
              } catch (InterruptedException e) {
                break;
              }
            }
          }
        }
        // 获取到连接
        if (conn != null) {
          // 通过 ping 来测试连接是否有效
          if (conn.isValid()) {
            // 如果非自动提交的，需要进行回滚。即将原有执行中的事务，全部回滚。可能担心上一次适用方忘记提交或回滚事务
            if (!conn.getRealConnection().getAutoCommit()) {
              conn.getRealConnection().rollback();
            }
            // 设置获取连接的属性
            conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
            conn.setCheckoutTimestamp(System.currentTimeMillis());
            conn.setLastUsedTimestamp(System.currentTimeMillis());
            // 添加到活跃的连接集合
            state.activeConnections.add(conn);
            // 对获取成功连接的统计
            state.requestCount++;
            state.accumulatedRequestTime += System.currentTimeMillis() - t;
          } else {
            // 这种就是上面拿到的超时的连接，坏掉了的
            if (log.isDebugEnabled()) {
              log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
            }
            // 统计获取到坏的连接的次数
            state.badConnectionCount++;
            // 记录获取到坏的连接的次数【本方法】
            localBadConnectionCount++;
            // 将 conn 置空，那么可以继续获取
            conn = null;
            // 如果超过最大次数（将等待的连接试完再重试允许的最大次数），抛出 SQLException 异常
            // 为什么次数要包含 poolMaximumIdleConnections 呢？相当于把激活的连接，全部遍历一次。
            if (localBadConnectionCount > (poolMaximumIdleConnections + poolMaximumLocalBadConnectionTolerance)) {
              if (log.isDebugEnabled()) {
                log.debug("PooledDataSource: Could not get a good connection to the database.");
              }
              throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
            }
          }
        }
      }

    }

    // 获取不到连接，抛出 SQLException 异常
    if (conn == null) {
      if (log.isDebugEnabled()) {
        log.debug("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
      }
      throw new SQLException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
    }

    return conn;
  }

  /**
   * 通过向数据库发起 poolPingQuery 语句来发起“ping”操作，以判断数据库连接是否有效
   *
   */
  protected boolean pingConnection(PooledConnection conn) {
    // 记录是否 ping 成功
    boolean result = true;

    // 判断真实的连接是否已经关闭。若已关闭，就意味着 ping 肯定是失败的。
    try {
      result = !conn.getRealConnection().isClosed();
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
      }
      result = false;
    }

    if (result) {
      // 是否启用侦测查询
      if (poolPingEnabled) {
        // 判断是否长时间未使用。若是，才需要发起 ping
        if (poolPingConnectionsNotUsedFor >= 0 && conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
          try {
            if (log.isDebugEnabled()) {
              log.debug("Testing connection " + conn.getRealHashCode() + " ...");
            }
            // 通过执行 poolPingQuery 语句来发起 ping
            Connection realConn = conn.getRealConnection();
            try (Statement statement = realConn.createStatement()) {
              statement.executeQuery(poolPingQuery).close();
            }
            if (!realConn.getAutoCommit()) {
              realConn.rollback();
            }
            // 标记执行成功
            result = true;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is GOOD!");
            }
          } catch (Exception e) {
            // 关闭数据库真实的连接
            log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
            try {
              // ping失败了要将连接关闭
              conn.getRealConnection().close();
            } catch (Exception e2) {
              //ignore
            }
            // 标记执行失败
            result = false;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * 获取真实的数据库连接, 因为PooledConnection实际是个代理类
   * TODO JDK动态代理的DEMO
   */
  public static Connection unwrapConnection(Connection conn) {
    // 如果传入的是被代理的连接
    if (Proxy.isProxyClass(conn.getClass())) {
      // 获取 InvocationHandler 对象
      InvocationHandler handler = Proxy.getInvocationHandler(conn);
      // 如果是 PooledConnection 对象，则获取真实的连接
      if (handler instanceof PooledConnection) {
        return ((PooledConnection) handler).getRealConnection();
      }
    }
    return conn;
  }

  /**
   * gc释放资源
   */
  @Override
  protected void finalize() throws Throwable {
    // 关闭所有连接
    forceCloseAll();
    // 执行对象销毁
    super.finalize();
  }

  /**
   * 来自Wrapper接口，这里都没没有做实现，感兴趣可以看看：https://blog.csdn.net/u011179993/article/details/53976976
   */
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException(getClass().getName() + " is not a wrapper.");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  @Override
  public Logger getParentLogger() {
    // requires JDK version 1.6
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

}
