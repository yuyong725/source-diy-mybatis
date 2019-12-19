package cn.javadog.sd.mybatis.session;

import java.io.Closeable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.BatchResult;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;

/**
 * @author 余勇
 * @date 2019-12-17 15:30
 *
 * SQL Session 接口。
 * MyBatis最核心的接口。通过这个接口，你可以执行sql，获取mapper，管理事务
 */
public interface SqlSession extends Closeable {

  /**
   * 执行指定的statement语句，获取一条记录
   *
   * @param <T> 返回结果的类型
   * @param statement
   * @return 返回的结果
   */
  <T> T selectOne(String statement);

  /**
   * 执行指定的statement语句，传入参数，获取一条记录
   */
  <T> T selectOne(String statement, Object parameter);

  /**
   * 查询列表
   */
  <E> List<E> selectList(String statement);

  /**
   * 查询列表，带参数
   */
  <E> List<E> selectList(String statement, Object parameter);

  /**
   * 查询列表，带参数，带分页
   */
  <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);

  /**
   * 查询列表，并使用某一属性作为key，转成map
   * 注意这个map的key，value并不是字段名与字段值。只是将获取到的列表转成了map解构而已
   */
  <K, V> Map<K, V> selectMap(String statement, String mapKey);

  /**
   * 查询列表，并使用某一属性作为key，转成map
   */
  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);

  /**
   * 查询列表，并使用某一属性作为key，转成map
   */
  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);

  /**
   * 查询列表。不过使用迭代器的方式懒加载数据
   */
  <T> Cursor<T> selectCursor(String statement);

  /**
   * 查询列表。不过使用迭代器的方式懒加载数据
   */
  <T> Cursor<T> selectCursor(String statement, Object parameter);

  /**
   * 查询列表。不过使用迭代器的方式懒加载数据
   */
  <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds);

  /**
   * 使用指定的 statement 和参数获取一条记录，并使用 ResultHandler 处理
   * TODO 一条记录？
   */
  void select(String statement, Object parameter, ResultHandler handler);

  /**
   * 使用指定的 statement查询，结果交给 ResultHandler 处理
   */
  void select(String statement, ResultHandler handler);

  /**
   * 使用指定的 statement + 参数 + 分页，结果交给 ResultHandler 处理
   */
  void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);

  /**
   * 执行插入操作
   */
  int insert(String statement);

  /**
   * 执行插入操作，使用给定的参数。
   * 任意 自增主键的值，或者@selectKey 将会更改 给定的参数值。
   * 最终返回影响的行数
   */
  int insert(String statement, Object parameter);

  /**
   * 执行更新操作
   */
  int update(String statement);

  /**
   * 执行更新操作，返回影响的行数
   */
  int update(String statement, Object parameter);

  /**
   * 执行删除操作
   */
  int delete(String statement);

  /**
   * 执行删除操作，返回影响的行数
   */
  int delete(String statement, Object parameter);

  /**
   * 输入批处理语句并提交连接。
   * 如果没有执行 增删改 操作时，并不会提交。必须调用此方法强制提交
   */
  void commit();

  /**
   * 输入批处理并提交连接
   * @param force 是否强制提交
   */
  void commit(boolean force);

  /**
   * 废弃批处理的语句，也就是不执行，然后关闭连接
   * note 如果没有 增改删 的操作，不会回滚。
   * 调用👇的方法 {@link SqlSession#rollback(boolean)} 强制回滚
   */
  void rollback();

  /**
   * 废弃批处理的语句，也就是不执行，然后关闭连接
   * @param force 是否强制回滚
   */
  void rollback(boolean force);

  /**
   * 刷入批处理语句
   * @return 更新的记录
   * @since 3.0.6
   */
  List<BatchResult> flushStatements();

  /**
   * 关闭会话
   */
  @Override
  void close();

  /**
   * 清除一级缓存
   */
  void clearCache();

  /**
   * 获取当前  Configuration
   */
  Configuration getConfiguration();

  /**
   * 获取指定mapper接口的实例
   */
  <T> T getMapper(Class<T> type);

  /**
   * 获取数据库连接
   */
  Connection getConnection();
}
