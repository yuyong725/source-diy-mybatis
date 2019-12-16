package cn.javadog.sd.mybatis.executor.loader;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.executor.ResultExtractor;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.Environment;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.cache.CacheKey;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.transaction.TransactionFactory;

/**
 * @author 余勇
 * @date 2019-12-15 15:37
 *
 * 结果加载器
 */
public class ResultLoader {

  /**
   * 全局配置
   */
  protected final Configuration configuration;

  /**
   * 执行器
   */
  protected final Executor executor;

  /**
   * MappedStatement 对象
   */
  protected final MappedStatement mappedStatement;

  /**
   * 查询的参数对象
   */
  protected final Object parameterObject;

  /**
   * 结果的类型
   */
  protected final Class<?> targetType;

  /**
   * ObjectFactory 对象
   */
  protected final ObjectFactory objectFactory;

  /**
   * 缓存键 CacheKey
   */
  protected final CacheKey cacheKey;

  /**
   * BoundSql 对象
   */
  protected final BoundSql boundSql;

  /**
   * ResultExtractor 对象，解析结果
   */
  protected final ResultExtractor resultExtractor;

  /**
   * 创建 ResultLoader 对象时，所在的线程
   */
  protected final long creatorThreadId;

  /**
   * 是否已经加载
   */
  protected boolean loaded;

  /**
   * 查询的结果对象。
   */
  protected Object resultObject;

  /**
   * 构造函数
   */
  public ResultLoader(Configuration config, Executor executor, MappedStatement mappedStatement, Object parameterObject, Class<?> targetType, CacheKey cacheKey, BoundSql boundSql) {
    this.configuration = config;
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.parameterObject = parameterObject;
    this.targetType = targetType;
    this.objectFactory = configuration.getObjectFactory();
    this.cacheKey = cacheKey;
    this.boundSql = boundSql;
    // 初始化 resultExtractor
    this.resultExtractor = new ResultExtractor(configuration, objectFactory);
    // 初始化 creatorThreadId
    this.creatorThreadId = Thread.currentThread().getId();
  }

  /**
   * 加载结果
   */
  public Object loadResult() throws SQLException {
    // 查询结果
    List<Object> list = selectList();
    // 提取结果
    resultObject = resultExtractor.extractObjectFromList(list, targetType);
    // 返回结果
    return resultObject;
  }

  /**
   * 查询结果。
   * 构造时，会将嵌套查询相关的SQL，参数值都已经传过来了
   */
  private <E> List<E> selectList() throws SQLException {
    // 获得 Executor 对象
    Executor localExecutor = executor;
    if (Thread.currentThread().getId() != this.creatorThreadId || localExecutor.isClosed()) {
      // 检查线程不对的话，比如开了新线程进行懒加载相关属性的加载，或者原执行器已经关闭，那么就新开一个执行器。TODO 执行器与线程貌似是强关联的
      localExecutor = newExecutor();
    }
    // 执行查询
    try {
      return localExecutor.query(mappedStatement, parameterObject, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER, cacheKey, boundSql);
    } finally {
      // 关闭 Executor 对象
      if (localExecutor != executor) {
        localExecutor.close(false);
      }
    }
  }

  /**
   * 创建 Executor 对象，因为 Executor 是非线程安全的
   */
  private Executor newExecutor() {
    // 校验 environment
    final Environment environment = configuration.getEnvironment();
    if (environment == null) {
      throw new ExecutorException("ResultLoader could not load lazily.  Environment was not configured.");
    }
    // 校验 ds
    final DataSource ds = environment.getDataSource();
    if (ds == null) {
      throw new ExecutorException("ResultLoader could not load lazily.  DataSource was not configured.");
    }
    // 创建 Transaction 对象
    final TransactionFactory transactionFactory = environment.getTransactionFactory();
    final Transaction tx = transactionFactory.newTransaction(ds, null, false);
    // 创建 Executor 对象
    return configuration.newExecutor(tx, ExecutorType.SIMPLE);
  }

  /**
   * 是否结果为空
   */
  public boolean wasNull() {
    return resultObject == null;
  }

}
