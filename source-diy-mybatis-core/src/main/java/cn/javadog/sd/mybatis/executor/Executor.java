package cn.javadog.sd.mybatis.executor;

import java.sql.SQLException;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.cache.CacheKey;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.transaction.Transaction;

/**
 * @author Clinton Begin
 *
 * 执行器接口
 */
public interface Executor {

  // 空 ResultHandler 对象的枚举
  ResultHandler NO_RESULT_HANDLER = null;

  // 更新 or 插入 or 删除，由传入的 MappedStatement 的 SQL 所决定
  int update(MappedStatement ms, Object parameter) throws SQLException;

  // 查询，带 ResultHandler + CacheKey + BoundSql
  <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;
  // 查询，带 ResultHandler
  <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;
  // 查询，返回值为 Cursor
  <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;

  // 刷入批处理语句
  List<BatchResult> flushStatements() throws SQLException;

  // 提交事务
  void commit(boolean required) throws SQLException;
  // 回滚事务
  void rollback(boolean required) throws SQLException;

  // 创建 CacheKey 对象
  CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);
  // 判断是否缓存
  boolean isCached(MappedStatement ms, CacheKey key);
  // 清除本地缓存
  void clearLocalCache();

  // 延迟加载
  void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);

  // 获得事务
  Transaction getTransaction();
  // 关闭事务
  void close(boolean forceRollback);
  // 判断事务是否关闭
  boolean isClosed();

  // 设置包装的 Executor 对象
  void setExecutorWrapper(Executor executor);

}
