package cn.javadog.sd.mybatis.support.datasource.unpooled;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author: 余勇
 * @date: 2019-12-02 23:11
 *
 * 实现 DataSource 接口，非池化的 DataSource 对象
 */
public class UnpooledDataSource implements DataSource {

  /**
   * Driver 类加载器
   */
  private ClassLoader driverClassLoader;

  /**
   * Driver 属性，可能还包含其他属性
   */
  private Properties driverProperties;

  /**
   * 已注册的 Driver 映射
   *
   * KEY：Driver 类名
   * VALUE：Driver 对象
   */
  private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<>();

  /**
   * Driver 类名
   */
  private String driver;

  /**
   * 数据库 URL
   */
  private String url;

  /**
   * 数据库用户名
   */
  private String username;

  /**
   * 数据库密码
   */
  private String password;

  /**
   * 是否自动提交事务
   */
  private Boolean autoCommit;

  /**
   * 默认事务隔离级别
   */
  private Integer defaultTransactionIsolationLevel;

  static {
    // 初始化 registeredDrivers，DriverManager 可以看看；https://www.jianshu.com/p/f5f677826715 了解一下
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      registeredDrivers.put(driver.getClass().getName(), driver);
    }
  }

  /**
   * 几个构造
   */
  public UnpooledDataSource() {
  }

  public UnpooledDataSource(String driver, String url, String username, String password) {
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public UnpooledDataSource(String driver, String url, Properties driverProperties) {
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }

  public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    this.driverClassLoader = driverClassLoader;
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    this.driverClassLoader = driverClassLoader;
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }

  /**
   * 获得 Connection 连接
   */
  @Override
  public Connection getConnection() throws SQLException {
    return doGetConnection(username, password);
  }

  /**
   * 获得 Connection 连接
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return doGetConnection(username, password);
  }

  /**
   * 设置登陆超时时长
   */
  @Override
  public void setLoginTimeout(int loginTimeout) throws SQLException {
    DriverManager.setLoginTimeout(loginTimeout);
  }

  /**
   * 获取登陆超时时长
   */
  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  /**
   * 设置日志打印器
   */
  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    DriverManager.setLogWriter(logWriter);
  }

  /**
   * 获取日志打印器
   */
  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return DriverManager.getLogWriter();
  }


  /**
   * 一些 get/set 方法
   */
  public ClassLoader getDriverClassLoader() {
    return driverClassLoader;
  }

  public void setDriverClassLoader(ClassLoader driverClassLoader) {
    this.driverClassLoader = driverClassLoader;
  }

  public Properties getDriverProperties() {
    return driverProperties;
  }

  public void setDriverProperties(Properties driverProperties) {
    this.driverProperties = driverProperties;
  }

  public String getDriver() {
    return driver;
  }

  public synchronized void setDriver(String driver) {
    this.driver = driver;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Boolean isAutoCommit() {
    return autoCommit;
  }

  public void setAutoCommit(Boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  public Integer getDefaultTransactionIsolationLevel() {
    return defaultTransactionIsolationLevel;
  }

  public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
    this.defaultTransactionIsolationLevel = defaultTransactionIsolationLevel;
  }

  /**
   * 获取 Connection 连接
   */
  private Connection doGetConnection(String username, String password) throws SQLException {
    // 创建 Properties 对象
    Properties props = new Properties();
    // 设置 driverProperties 到 props 中
    if (driverProperties != null) {
      props.putAll(driverProperties);
    }
    // 设置 user 和 password 到 props 中
    if (username != null) {
      props.setProperty("user", username);
    }
    if (password != null) {
      props.setProperty("password", password);
    }
    // 执行获得 Connection 连接
    return doGetConnection(props);
  }

  private Connection doGetConnection(Properties properties) throws SQLException {
    // 必须初始化 Driver，不然DriverManager拿不到驱动就拿不到连接
    initializeDriver();
    // 获得 Connection 对象
    Connection connection = DriverManager.getConnection(url, properties);
    // 配置 Connection 对象
    configureConnection(connection);
    return connection;
  }

  /**
   * 初始化 Drive
   */
  private synchronized void initializeDriver() throws SQLException {
    // 判断 registeredDrivers 是否已经存在该 driver ，若不存在，进行初始化
    if (!registeredDrivers.containsKey(driver)) {
      Class<?> driverType;
      try {
        // 获得 driver 类
        if (driverClassLoader != null) {
          // 使用指定的类加载器加载
          driverType = Class.forName(driver, true, driverClassLoader);
        } else {
          // TODO Resources是IO模块的类，后续再打开
          // driverType = Resources.classForName(driver);
          driverType = null;
        }
        // 创建 Driver 对象，TODO 注释提到DriverManager需要driver通过系统的类加载器加载？
        Driver driverInstance = (Driver)driverType.newInstance();
        // 创建 DriverProxy 对象，并注册到 DriverManager 中
        DriverManager.registerDriver(new DriverProxy(driverInstance));
        // 添加到 registeredDrivers 中
        registeredDrivers.put(driver, driverInstance);
      } catch (Exception e) {
        throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
      }
    }
  }

  /**
   * 配置 Connection 对象
   */
  private void configureConnection(Connection conn) throws SQLException {
    // 设置自动提交，覆盖Connection默认的自动提交
    if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
      conn.setAutoCommit(autoCommit);
    }
    // 设置事务隔离级别
    if (defaultTransactionIsolationLevel != null) {
      conn.setTransactionIsolation(defaultTransactionIsolationLevel);
    }
  }

  /**
   * Driver代理类
   * TODO 为什么注册时要包一个代理类
   */
  private static class DriverProxy implements Driver {
    private Driver driver;

    DriverProxy(Driver d) {
      this.driver = d;
    }

    @Override
    public boolean acceptsURL(String u) throws SQLException {
      return this.driver.acceptsURL(u);
    }

    @Override
    public Connection connect(String u, Properties p) throws SQLException {
      return this.driver.connect(u, p);
    }

    @Override
    public int getMajorVersion() {
      return this.driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
      return this.driver.getMinorVersion();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
      return this.driver.getPropertyInfo(u, p);
    }

    @Override
    public boolean jdbcCompliant() {
      return this.driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }
  }

  /**
   * 没有实现，作用我不知，直接报错
   */
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException(getClass().getName() + " is not a wrapper.");
  }

  /**
   * 没有实现
   */
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  /**
   * 获取上一级的logger
   * TODO 作用
   */
  @Override
  public Logger getParentLogger() {
    // requires JDK version 1.6
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

}
