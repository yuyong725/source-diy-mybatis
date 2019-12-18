package cn.javadog.sd.mybatis.session;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.BatchResult;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.support.exceptions.SqlSessionException;
import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * @author 余勇
 * @date 2019-12-17 16:02
 * 实现 SqlSessionFactory、SqlSession 接口，SqlSession 管理器。
 * 所以，从这里已经可以看出，SqlSessionManager 是 SqlSessionFactory 和 SqlSession 的职能相加
 */
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

  /**
   * SqlSession 工厂
   */
  private final SqlSessionFactory sqlSessionFactory;

  /**
   * SqlSession 代理
   */
  private final SqlSession sqlSessionProxy;

  /**
   * 线程变量，当前线程的 SqlSession 对象
   */
  private final ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<>();

  /**
   * 构造函数
   */
  private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    // 创建 SqlSession 的代理对象。使用的是 SqlSessionInterceptor
    this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSession.class},
        new SqlSessionInterceptor());
  }

  /**
   * 使用 reader 创建 SqlSessionManager 实例
   */
  public static SqlSessionManager newInstance(Reader reader) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
  }

  /**
   * 使用 reader+environment 创建 SqlSessionManager 实例
   */
  public static SqlSessionManager newInstance(Reader reader, String environment) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, environment, null));
  }

  /**
   * 使用 reader+properties 创建 SqlSessionManager 实例
   */
  public static SqlSessionManager newInstance(Reader reader, Properties properties) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, properties));
  }

  /**
   * 使用 inputStream 创建 SqlSessionManager 实例
   */
  public static SqlSessionManager newInstance(InputStream inputStream) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, null));
  }

  /**
   * 使用 inputStream+environment 创建 SqlSessionManager 实例
   */
  public static SqlSessionManager newInstance(InputStream inputStream, String environment) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, environment, null));
  }

  /**
   * 使用 inputStream+properties 创建 SqlSessionManager 实例
   */
  public static SqlSessionManager newInstance(InputStream inputStream, Properties properties) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, properties));
  }

  /**
   * sqlSessionFactory，有这玩意为鸡儿不直接构造
   */
  public static SqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionManager(sqlSessionFactory);
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession() {
    this.localSqlSession.set(openSession());
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession(boolean autoCommit) {
    this.localSqlSession.set(openSession(autoCommit));
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession(Connection connection) {
    this.localSqlSession.set(openSession(connection));
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession(TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(level));
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession(ExecutorType execType) {
    this.localSqlSession.set(openSession(execType));
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession(ExecutorType execType, boolean autoCommit) {
    this.localSqlSession.set(openSession(execType, autoCommit));
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(execType, level));
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public void startManagedSession(ExecutorType execType, Connection connection) {
    this.localSqlSession.set(openSession(execType, connection));
  }

  /**
   * 发起一个可被管理的 SqlSession
   */
  public boolean isManagedSessionStarted() {
    return this.localSqlSession.get() != null;
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession() {
    return sqlSessionFactory.openSession();
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(boolean autoCommit) {
    return sqlSessionFactory.openSession(autoCommit);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(Connection connection) {
    return sqlSessionFactory.openSession(connection);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(TransactionIsolationLevel level) {
    return sqlSessionFactory.openSession(level);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType) {
    return sqlSessionFactory.openSession(execType);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return sqlSessionFactory.openSession(execType, autoCommit);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return sqlSessionFactory.openSession(execType, level);
  }

  /**
   * 开启会话
   */
  @Override
  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return sqlSessionFactory.openSession(execType, connection);
  }

  /**
   * 获取全局配置
   */
  @Override
  public Configuration getConfiguration() {
    return sqlSessionFactory.getConfiguration();
  }

  /**
   * 获取一条记录
   */
  @Override
  public <T> T selectOne(String statement) {
    return sqlSessionProxy.<T> selectOne(statement);
  }

  /**
   * 获取一条记录
   */
  @Override
  public <T> T selectOne(String statement, Object parameter) {
    return sqlSessionProxy.<T> selectOne(statement, parameter);
  }

  /**
   * 获取列表，并使用mapKey转成map
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return sqlSessionProxy.<K, V> selectMap(statement, mapKey);
  }

  /**
   * 获取列表，并使用mapKey转成map
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return sqlSessionProxy.<K, V> selectMap(statement, parameter, mapKey);
  }

  /**
   * 获取列表，并使用mapKey转成map
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return sqlSessionProxy.<K, V> selectMap(statement, parameter, mapKey, rowBounds);
  }

  /**
   * 查询游标
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement) {
    return sqlSessionProxy.selectCursor(statement);
  }

  /**
   * 查询游标
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return sqlSessionProxy.selectCursor(statement, parameter);
  }

  /**
   * 查询游标
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    return sqlSessionProxy.selectCursor(statement, parameter, rowBounds);
  }

  /**
   * 查询列表
   */
  @Override
  public <E> List<E> selectList(String statement) {
    return sqlSessionProxy.<E> selectList(statement);
  }

  /**
   * 查询列表
   */
  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return sqlSessionProxy.<E> selectList(statement, parameter);
  }

  /**
   * 查询列表
   */
  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return sqlSessionProxy.<E> selectList(statement, parameter, rowBounds);
  }

  /**
   * 查询，将结果交给 ResultHandler 处理
   */
  @Override
  public void select(String statement, ResultHandler handler) {
    sqlSessionProxy.select(statement, handler);
  }

  /**
   * 查询，将结果交给 ResultHandler 处理
   */
  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    sqlSessionProxy.select(statement, parameter, handler);
  }

  /**
   * 查询，将结果交给 ResultHandler 处理
   */
  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    sqlSessionProxy.select(statement, parameter, rowBounds, handler);
  }

  /**
   * 新增
   */
  @Override
  public int insert(String statement) {
    return sqlSessionProxy.insert(statement);
  }

  /**
   * 新增
   */
  @Override
  public int insert(String statement, Object parameter) {
    return sqlSessionProxy.insert(statement, parameter);
  }

  /**
   * 修改
   */
  @Override
  public int update(String statement) {
    return sqlSessionProxy.update(statement);
  }

  /**
   * 修改
   */
  @Override
  public int update(String statement, Object parameter) {
    return sqlSessionProxy.update(statement, parameter);
  }

  /**
   * 删除
   */
  @Override
  public int delete(String statement) {
    return sqlSessionProxy.delete(statement);
  }

  /**
   * 删除
   */
  @Override
  public int delete(String statement, Object parameter) {
    return sqlSessionProxy.delete(statement, parameter);
  }

  /**
   * 获取指定mapper接口的代理对象
   */
  @Override
  public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
  }

  /**
   * 获取连接
   */
  @Override
  public Connection getConnection() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
    }
    return sqlSession.getConnection();
  }

  /**
   * 清空一级缓存
   */
  @Override
  public void clearCache() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
    }
    sqlSession.clearCache();
  }

  /**
   * 提交事务
   */
  @Override
  public void commit() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit();
  }

  /**
   * 提交事务，支持 查询类型 的强制提交
   */
  @Override
  public void commit(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit(force);
  }

  /**
   * 回滚
   */
  @Override
  public void rollback() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback();
  }

  /**
   * 回滚，支持强制回滚
   */
  @Override
  public void rollback(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback(force);
  }

  /**
   * 刷入批处理
   */
  @Override
  public List<BatchResult> flushStatements() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    return sqlSession.flushStatements();
  }

  /**
   * 关闭会话
   */
  @Override
  public void close() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
    }
    try {
      sqlSession.close();
    } finally {
      localSqlSession.set(null);
    }
  }

  /**
   * SqlSessionManager 内部类，实现 InvocationHandler 接口，实现对 sqlSessionProxy 的调用的拦截
   */
  private class SqlSessionInterceptor implements InvocationHandler {
    public SqlSessionInterceptor() {
        // Prevent Synthetic Access
    }

    /**
     * 拦截方法
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // 情况一，如果 localSqlSession 中存在 SqlSession 对象，说明是自管理模式。TODO ？
      final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get();
      if (sqlSession != null) {
        try {
          // 直接执行方法
          return method.invoke(sqlSession, args);
        } catch (Throwable t) {
          throw ExceptionUtil.unwrapThrowable(t);
        }
      }
      // 情况二，如果没有 SqlSession 对象，则直接创建一个
      else {
        // 创建新的 SqlSession 对象
        try (SqlSession autoSqlSession = openSession()) { // 同时，通过 try 的语法糖，实现结束时，关闭 SqlSession 对象
          try {
            // 执行方法
            final Object result = method.invoke(autoSqlSession, args);
            // 提交 SqlSession 对象
            autoSqlSession.commit();
            return result;
          } catch (Throwable t) {
            // 发生异常时，回滚
            autoSqlSession.rollback();
            throw ExceptionUtil.unwrapThrowable(t);
          }
        }
      }
    }
  }

}
