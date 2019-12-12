package cn.javadog.sd.mybatis.executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.statement.StatementHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.transaction.Transaction;

/**
 * @author Clinton Begin
 *
 * 继承 BaseExecutor 抽象类，简单的 Executor 实现类
 *
 * 每次开始读或写操作，都创建对应的 Statement 对象。
 * 执行完成后，关闭该 Statement 对象。
 */
public class SimpleExecutor extends BaseExecutor {

  public SimpleExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      // 创建 StatementHandler 对象
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      // 初始化 StatementHandler 对象
      stmt = prepareStatement(handler, ms.getStatementLog());
      // <3> 执行 StatementHandler ，进行写操作
      return handler.update(stmt);
    } finally {
      // 关闭 StatementHandler 对象
      closeStatement(stmt);
    }
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      // <1> 创建 StatementHandler 对象
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      // <2> 初始化 StatementHandler 对象
      stmt = prepareStatement(handler, ms.getStatementLog());
      // <3> 执行 StatementHandler  ，进行读操作
      return handler.query(stmt, resultHandler);
    } finally {
      // <4> 关闭 StatementHandler 对象
      closeStatement(stmt);
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    // 初始化 StatementHandler 对象
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
//    // 设置 Statement ，如果执行完成，则进行自动关闭 note  3.5.0的版本做了这件事
//    stmt.closeOnCompletion();
    // 执行 StatementHandler  ，进行读操作
    return handler.<E>queryCursor(stmt);
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    //不存在批量操作的情况，所以直接返回空数组。
    return Collections.emptyList();
  }

  /**
   * 初始化 StatementHandler 对象
   */
  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    // <2.1> 获得 Connection 对象
    Connection connection = getConnection(statementLog);
    // <2.2> 创建 Statement 或 PrepareStatement 对象
    stmt = handler.prepare(connection, transaction.getTimeout());
    // <2.3> 设置 SQL 上的参数，例如 PrepareStatement 对象上的占位符
    handler.parameterize(stmt);
    return stmt;
  }

}
