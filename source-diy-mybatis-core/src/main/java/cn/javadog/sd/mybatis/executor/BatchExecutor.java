package cn.javadog.sd.mybatis.executor;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.keygen.Jdbc3KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.NoKeyGenerator;
import cn.javadog.sd.mybatis.executor.statement.StatementHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.transaction.Transaction;

/**
 * @author 余勇
 * @date 2019-12-16 21:30
 * 支持批处理的 Executor 实现类。
 * 强烈建议看看：https://my.oschina.net/zudajun/blog/667214
 */
public class BatchExecutor extends BaseExecutor {

  /**
   * 批处理返回值的默认值，也就是所有语句的影响行数
   */
  public static final int BATCH_UPDATE_RETURN_VALUE = Integer.MIN_VALUE + 1002;

  /**
   * Statement 数组
   */
  private final List<Statement> statementList = new ArrayList<>();

  /**
   * BatchResult 数组
   *
   * 每一个 BatchResult 元素，对应一个 {@link #statementList} 的 Statement 元素
   */
  private final List<BatchResult> batchResultList = new ArrayList<>();

  /**
   * 当前 SQL
   */
  private String currentSql;

  /**
   * 当前 MappedStatement 对象
   */
  private MappedStatement currentStatement;

  /**
   * 构造函数
   */
  public BatchExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  /**
   * 增改删
   */
  @Override
  public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
    final Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT, null, null);
    final BoundSql boundSql = handler.getBoundSql();
    // 本次执行的SQL
    final String sql = boundSql.getSql();
    final Statement stmt;
    // 如果匹配最后一次 currentSql 和 currentStatement ，则聚合到 BatchResult 中。相当于重用 statement
    if (sql.equals(currentSql) && ms.equals(currentStatement)) {
      // 获得最后一次的 Statement 对象
      int last = statementList.size() - 1;
      stmt = statementList.get(last);
      // 设置事务超时时间
      applyTransactionTimeout(stmt);
      // 设置 SQL 上的参数，例如 PrepareStatement 对象上的占位符
      handler.parameterize(stmt);
      // 获得最后一次的 BatchResult 对象，并添加参数到其中
      BatchResult batchResult = batchResultList.get(last);
      batchResult.addParameterObject(parameterObject);
    }
    // 如果不匹配最后一次 currentSql 和 currentStatement ，则新建 BatchResult 对象
    else {
      // 获得 Connection
      Connection connection = getConnection(ms.getStatementLog());
      // 创建 Statement 或 PrepareStatement 对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 设置 SQL 上的参数，例如 PrepareStatement 对象上的占位符
      handler.parameterize(stmt);
      // 重新设置 currentSql 和 currentStatement
      currentSql = sql;
      currentStatement = ms;
      // 添加 Statement 到 statementList 中
      statementList.add(stmt);
      // 创建 BatchResult 对象，并添加到 batchResultList 中
      batchResultList.add(new BatchResult(ms, sql, parameterObject));
    }
    // 批处理
    handler.batch(stmt);
    return BATCH_UPDATE_RETURN_VALUE;
  }

  /**
   * 执行查询
   */
  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
      throws SQLException {
    Statement stmt = null;
    try {
      // 刷入批处理语句
      flushStatements();
      // 获取 configuration
      Configuration configuration = ms.getConfiguration();
      // 创建 StatementHandler 对象
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameterObject, rowBounds, resultHandler, boundSql);
      // 获得 Connection 对象
      Connection connection = getConnection(ms.getStatementLog());
      // 创建 Statement 或 PrepareStatement 对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 设置 SQL 上的参数，例如 PrepareStatement 对象上的占位符
      handler.parameterize(stmt);
      // 执行 StatementHandler  ，进行读操作
      return handler.query(stmt, resultHandler);
    } finally {
      // 关闭 StatementHandler 对象
      closeStatement(stmt);
    }
  }

  /**
   * 执行查询
   */
  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    // 刷入批处理语句
    flushStatements();
    Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    // 获得 Connection 对象
    Connection connection = getConnection(ms.getStatementLog());
    // 创建 Statement 或 PrepareStatement 对象
    Statement stmt = handler.prepare(connection, transaction.getTimeout());
    // 设置 Statement ，如果执行完成，则进行自动关闭
    stmt.closeOnCompletion();
    // 设置 SQL 上的参数，例如 PrepareStatement 对象上的占位符
    handler.parameterize(stmt);
    // 执行 StatementHandler  ，进行读操作
    return handler.<E>queryCursor(stmt);
  }

  /**
   * 刷入批处理
   */
  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    try {
      // 如果 isRollback 为 true ，返回空数组
      if (isRollback) {
        return Collections.emptyList();
      }
      // 遍历 statementList 和 batchResultList 数组，逐个提交批处理
      List<BatchResult> results = new ArrayList<>();
      for (int i = 0, n = statementList.size(); i < n; i++) {
        // 获得 Statement 对象
        Statement stmt = statementList.get(i);
        // 设置事务超时
        applyTransactionTimeout(stmt);
        // 拿到 BatchResult 对象
        BatchResult batchResult = batchResultList.get(i);
        try {
          // 批量执行
          batchResult.setUpdateCounts(stmt.executeBatch());
          // 处理主键生成
          MappedStatement ms = batchResult.getMappedStatement();
          List<Object> parameterObjects = batchResult.getParameterObjects();
          KeyGenerator keyGenerator = ms.getKeyGenerator();
          if (Jdbc3KeyGenerator.class.equals(keyGenerator.getClass())) {
            Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
            jdbc3KeyGenerator.processBatch(ms, stmt, parameterObjects);
          } else if (!NoKeyGenerator.class.equals(keyGenerator.getClass())) {
            for (Object parameter : parameterObjects) {
              keyGenerator.processAfter(this, ms, stmt, parameter);
            }
          }
          // 关闭 Statement 对象
          closeStatement(stmt);
        } catch (BatchUpdateException e) {
          // 如果发生异常，则抛出 BatchExecutorException 异常
          StringBuilder message = new StringBuilder();
          message.append(batchResult.getMappedStatement().getId())
                  .append(" (batch index #")
                  .append(i + 1)
                  .append(")")
                  .append(" failed.");
          if (i > 0) {
            message.append(" ")
                    .append(i)
                    .append(" prior sub executor(s) completed successfully, but will be rolled back.");
          }
          throw new BatchExecutorException(message.toString(), e, results, batchResult);
        }
        // 添加到结果集
        results.add(batchResult);
      }
      return results;
    } finally {
      // 关闭 Statement 们
      for (Statement stmt : statementList) {
        closeStatement(stmt);
      }
      // 置空 currentSql、statementList、batchResultList 属性
      currentSql = null;
      statementList.clear();
      batchResultList.clear();
    }
  }

}
