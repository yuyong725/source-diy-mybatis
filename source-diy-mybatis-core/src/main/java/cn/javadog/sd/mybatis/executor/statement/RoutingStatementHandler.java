package cn.javadog.sd.mybatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;

/**
 * @author 余勇
 * @date 2019-12-16 20:45
 *
 * 路由的StatementHandler 对象，
 * 根据 Statement 类型，转发到对应的 StatementHandler 实现类中。
 */
public class RoutingStatementHandler implements StatementHandler {

  /**
   * 委托对象
   */
  private final StatementHandler delegate;

  /**
   * 构造函数
   */
  public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    // 根据不同的类型，创建对应的 StatementHandler 实现类
    switch (ms.getStatementType()) {
      case STATEMENT:
        delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      case PREPARED:
        delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      default:
        throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
    }

  }

  /**
   * 预处理，一般就是初始化 Statement
   */
  @Override
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
    return delegate.prepare(connection, transactionTimeout);
  }

  /**
   * 设置参数
   */
  @Override
  public void parameterize(Statement statement) throws SQLException {
    delegate.parameterize(statement);
  }

  /**
   * 执行批处理
   */
  @Override
  public void batch(Statement statement) throws SQLException {
    delegate.batch(statement);
  }

  /**
   * 执行 增改删
   */
  @Override
  public int update(Statement statement) throws SQLException {
    return delegate.update(statement);
  }

  /**
   * 执行查询
   */
  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    return delegate.<E>query(statement, resultHandler);
  }

  /**
   * 执行查询游标
   */
  @Override
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    return delegate.queryCursor(statement);
  }

  /**
   * 获取 BoundSql
   */
  @Override
  public BoundSql getBoundSql() {
    return delegate.getBoundSql();
  }

  /**
   * 获取参数处理器
   */
  @Override
  public ParameterHandler getParameterHandler() {
    return delegate.getParameterHandler();
  }
}
