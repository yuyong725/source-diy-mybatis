package cn.javadog.sd.mybatis.executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.statement.StatementHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.transaction.Transaction;

/**
 * @author 余勇
 * @date 2019-12-16 21:03
 *
 * 可重用的 Executor 实现类
 * 每次开始读或写操作，优先从缓存中获取对应的 Statement 对象。如果不存在，才进行创建。
 * 执行完成后，不关闭该 Statement 对象。
 * 其它的，和 SimpleExecutor 是一致的。
 *
 */
public class ReuseExecutor extends BaseExecutor {

  /**
   * Statement 的缓存
   *
   * KEY ：SQL
   */
  private final Map<String, Statement> statementMap = new HashMap<>();

  /**
   * 构造函数
   */
  public ReuseExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  /**
   * 执行 增删改
   */
  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
    // 初始化 Statement 对象
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行 StatementHandler  ，进行写操作
    return handler.update(stmt);
  }

  /**
   * 执行查询操作
   */
  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    // 初始化 Statement 对象
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行 StatementHandler  ，进行读操作
    return handler.<E>query(stmt, resultHandler);
  }

  /**
   * 执行查询游标操作
   */
  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    // 初始化 Statement 对象
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行 StatementHandler  ，进行读操作
    return handler.<E>queryCursor(stmt);
  }

  /**
   * 批处理。此执行器也不支持批处理
   */
  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    // 关闭缓存的 Statement 对象们
    for (Statement stmt : statementMap.values()) {
      closeStatement(stmt);
    }
    statementMap.clear();
    // 返回空集合
    return Collections.emptyList();
  }

  /**
   * 初始化Statement，与其它执行器不同的也在这里
   */
  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    BoundSql boundSql = handler.getBoundSql();
    // 拿到SQL
    String sql = boundSql.getSql();
    // 存在
    if (hasStatementFor(sql)) {
      // 从缓存中获得 Statement 或 PrepareStatement 对象
      stmt = getStatement(sql);
      // 设置事务超时时间
      applyTransactionTimeout(stmt);
    }
    // 不存在
    else {
      // 获得 Connection 对象
      Connection connection = getConnection(statementLog);
      // 创建 Statement 或 PrepareStatement 对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 添加到缓存中
      putStatement(sql, stmt);
    }
    // 设置 SQL 上的参数，例如 PrepareStatement 对象上的占位符
    handler.parameterize(stmt);
    return stmt;
  }

  /**
   * 判断是否存在对应的 Statement 对象
   */
  private boolean hasStatementFor(String sql) {
    try {
      // 从 statementMap 拿指定key的 statement，并检验所属的连接没有关闭
      return statementMap.keySet().contains(sql) && !statementMap.get(sql).getConnection().isClosed();
    } catch (SQLException e) {
      return false;
    }
  }

  /**
   * 获得 Statement 对象
   */
  private Statement getStatement(String s) {
    return statementMap.get(s);
  }

  /**
   * 添加 Statement 对象到缓存中
   */
  private void putStatement(String sql, Statement stmt) {
    statementMap.put(sql, stmt);
  }

}
