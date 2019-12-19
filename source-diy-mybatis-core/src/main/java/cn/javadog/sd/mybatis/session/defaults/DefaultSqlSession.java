package cn.javadog.sd.mybatis.session.defaults;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.BatchResult;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.executor.result.DefaultMapResultHandler;
import cn.javadog.sd.mybatis.executor.result.DefaultResultContext;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.support.exceptions.BindingException;
import cn.javadog.sd.mybatis.support.exceptions.TooManyResultsException;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * @author 余勇
 * @date 2019-12-17 16:17
 *
 * 默认的 SqlSession 实现类
 */
public class DefaultSqlSession implements SqlSession {

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * 执行器
   */
  private final Executor executor;

  /**
   * 是否自动提交事务
   */
  private final boolean autoCommit;

  /**
   * 是否发生数据变更，表示执行过写操作
   */
  private boolean dirty;

  /**
   * Cursor 数组
   */
  private List<Cursor<?>> cursorList;

  /**
   * 构造函数
   */
  public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
    this.configuration = configuration;
    this.executor = executor;
    this.dirty = false;
    this.autoCommit = autoCommit;
  }

  /**
   * 构造函数
   */
  public DefaultSqlSession(Configuration configuration, Executor executor) {
    this(configuration, executor, false);
  }

  /**
   * 查询单条记录
   */
  @Override
  public <T> T selectOne(String statement) {
    return this.<T>selectOne(statement, null);
  }

  /**
   * 查询单条记录
   */
  @Override
  public <T> T selectOne(String statement, Object parameter) {
    // 最受欢迎的方案就是，返回多条记录直接GG，一条都没有返回null，只有一条就返回
    List<T> list = this.<T>selectList(statement, parameter);
    if (list.size() == 1) {
      return list.get(0);
    } else if (list.size() > 1) {
      throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
    } else {
      return null;
    }
  }

  /**
   * 获取列表，使用mapKey转成map
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
  }

  /**
   * 获取列表，使用mapKey转成map
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
  }

  /**
   * 查询结果，并基于 Map 聚合结果
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    // 执行查询
    final List<? extends V> list = selectList(statement, parameter, rowBounds);
    // 创建 DefaultMapResultHandler 对象
    final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<>(mapKey,
            configuration.getObjectFactory(), configuration.getObjectWrapperFactory(), configuration.getReflectorFactory());
    // 创建 DefaultResultContext 对象
    final DefaultResultContext<V> context = new DefaultResultContext<>();
    // 遍历查询结果
    for (V o : list) {
      // 设置 DefaultResultContext 中
      context.nextResultObject(o);
      // 使用 DefaultMapResultHandler 处理结果的当前元素
      mapResultHandler.handleResult(context);
    }
    // 返回结果
    return mapResultHandler.getMappedResults();
  }

  /**
   * 查询
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement) {
    return selectCursor(statement, null);
  }

  /**
   * 查询
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return selectCursor(statement, parameter, RowBounds.DEFAULT);
  }

  /**
   * 查询
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    try {
      // 获得 MappedStatement 对象
      MappedStatement ms = configuration.getMappedStatement(statement);
      // 执行查询
      Cursor<T> cursor = executor.queryCursor(ms, wrapCollection(parameter), rowBounds);
      // 添加 cursor 到 cursorList 中
      registerCursor(cursor);
      return cursor;
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      // 重置 ErrorContext
      ErrorContext.instance().reset();
    }
  }

  /**
   * 查询列表
   */
  @Override
  public <E> List<E> selectList(String statement) {
    return this.selectList(statement, null);
  }

  /**
   * 查询列表
   */
  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return this.selectList(statement, parameter, RowBounds.DEFAULT);
  }

  /**
   * 查询列表
   */
  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
      // 获得 MappedStatement 对象
      MappedStatement ms = configuration.getMappedStatement(statement);
      // 执行查询
      return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      // 重置 ErrorContext
      ErrorContext.instance().reset();
    }
  }

  /**
   * 查询，结果交给ResultHandler处理
   */
  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    select(statement, parameter, RowBounds.DEFAULT, handler);
  }

  /**
   * 查询，结果交给ResultHandler处理
   */
  @Override
  public void select(String statement, ResultHandler handler) {
    select(statement, null, RowBounds.DEFAULT, handler);
  }

  /**
   * 执行查询，使用传入的 handler 方法参数，对结果进行处理
   */
  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    try {
      // 获得 MappedStatement 对象
      MappedStatement ms = configuration.getMappedStatement(statement);
      // 执行查询
      executor.query(ms, wrapCollection(parameter), rowBounds, handler);
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 执行插入
   */
  @Override
  public int insert(String statement) {
    return insert(statement, null);
  }

  /**
   * 执行插入
   */
  @Override
  public int insert(String statement, Object parameter) {
    return update(statement, parameter);
  }

  /**
   * 执行更新
   */
  @Override
  public int update(String statement) {
    return update(statement, null);
  }

  /**
   * 执行更新
   */
  @Override
  public int update(String statement, Object parameter) {
    try {
      // 标记 dirty ，表示执行过写操作
      dirty = true;
      // 获得 MappedStatement 对象
      MappedStatement ms = configuration.getMappedStatement(statement);
      // 执行更新操作
      return executor.update(ms, wrapCollection(parameter));
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error updating database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 删除
   */
  @Override
  public int delete(String statement) {
    return update(statement, null);
  }

  /**
   * 删除
   */
  @Override
  public int delete(String statement, Object parameter) {
    return update(statement, parameter);
  }

  /**
   * 提交事务
   */
  @Override
  public void commit() {
    commit(false);
  }

  /**
   * 提交事务
   */
  @Override
  public void commit(boolean force) {
    try {
      // 提交事务
      executor.commit(isCommitOrRollbackRequired(force));
      // 标记 dirty 为 false
      dirty = false;
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error committing transaction.  Cause: " + e, e);
    } finally {
      //重置 ErrorContext
      ErrorContext.instance().reset();
    }
  }

  /**
   * 回滚
   */
  @Override
  public void rollback() {
    rollback(false);
  }

  /**
   * 回滚
   */
  @Override
  public void rollback(boolean force) {
    try {
      // 回滚事务
      executor.rollback(isCommitOrRollbackRequired(force));
      // 标记 dirty 为 false
      dirty = false;
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error rolling back transaction.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 刷入批处理
   */
  @Override
  public List<BatchResult> flushStatements() {
    try {
      return executor.flushStatements();
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error flushing statements.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 关闭事务
   */
  @Override
  public void close() {
    try {
      // 关闭执行器
      executor.close(isCommitOrRollbackRequired(false));
      // 关闭所有游标
      closeCursors();
      // 重置 dirty 为 false
      dirty = false;
    } finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * 关闭所有游标
   */
  private void closeCursors() {
    if (cursorList != null && cursorList.size() != 0) {
      for (Cursor<?> cursor : cursorList) {
        try {
          cursor.close();
        } catch (IOException e) {
          throw ExceptionUtil.wrapException("Error closing cursor.  Cause: " + e, e);
        }
      }
      cursorList.clear();
    }
  }

  /**
   * 获取全局配置
   */
  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * 获取 mapper对象
   */
  @Override
  public <T> T getMapper(Class<T> type) {
    return configuration.<T>getMapper(type, this);
  }

  /**
   * 获取连接对象
   */
  @Override
  public Connection getConnection() {
    try {
      return executor.getTransaction().getConnection();
    } catch (SQLException e) {
      throw ExceptionUtil.wrapException("Error getting a new connection.  Cause: " + e, e);
    }
  }

  /**
   * 清空一级缓存
   */
  @Override
  public void clearCache() {
    executor.clearLocalCache();
  }

  /**
   * 注册游标
   */
  private <T> void registerCursor(Cursor<T> cursor) {
    if (cursorList == null) {
      cursorList = new ArrayList<>();
    }
    cursorList.add(cursor);
  }

  /**
   * 是否强制提交/回滚。
   * 因为默认查询是不会触发事务的提交的
   */
  private boolean isCommitOrRollbackRequired(boolean force) {
    // 不是自动提交又发生了更新操作，必须提交/回滚 || 强制
    return (!autoCommit && dirty) || force;
  }

  /**
   * 如果参数是集合类型，进行包装成 StrictMap
   */
  private Object wrapCollection(final Object object) {
    if (object instanceof Collection) {
      // 如果是集合，则添加到 collection 中
      StrictMap<Object> map = new StrictMap<>();
      map.put("collection", object);
      // 如果是 List ，则添加到 list 中
      if (object instanceof List) {
        map.put("list", object);
      }
      return map;
    } else if (object != null && object.getClass().isArray()) {
      // 如果是 Array ，则添加到 array 中
      StrictMap<Object> map = new StrictMap<>();
      map.put("array", object);
      return map;
    }
    return object;
  }

  /**
   * 众里寻她千百度，原来在这里
   */
  public static class StrictMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -5741767162221585340L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + this.keySet());
      }
      return super.get(key);
    }

  }

}
