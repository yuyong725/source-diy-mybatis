package cn.javadog.sd.mybatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import cn.javadog.sd.mybatis.executor.ErrorContext;
import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.executor.resultset.ResultSetHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author 余勇
 * @date 2019-12-16 20:29
 *
 * StatementHandler 基类，提供骨架方法，从而使子类只要实现指定的几个抽象方法即可
 */
public abstract class BaseStatementHandler implements StatementHandler {

  /**
   * 全局配置
   */
  protected final Configuration configuration;

  /**
   * 对象工厂
   */
  protected final ObjectFactory objectFactory;

  /**
   * 类型处理器注册表
   */
  protected final TypeHandlerRegistry typeHandlerRegistry;

  /**
   * ResultSet 处理器
   */
  protected final ResultSetHandler resultSetHandler;

  /**
   * 参数处理器
   */
  protected final ParameterHandler parameterHandler;

  /**
   * 执行器
   */
  protected final Executor executor;

  /**
   * mappedStatement 对象
   */
  protected final MappedStatement mappedStatement;

  /**
   * 分页信息
   */
  protected final RowBounds rowBounds;

  /**
   * SQL 信息
   */
  protected BoundSql boundSql;

  /**
   * 构造函数
   */
  protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    // 获得 Configuration 对象
    this.configuration = mappedStatement.getConfiguration();
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;

    // 获得 TypeHandlerRegistry 和 ObjectFactory 对象
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();

    // 如果 boundSql 为空，一般是写类操作，例如：insert、update、delete ，则先获得自增主键，然后再创建 BoundSql 对象
    if (boundSql == null) {
      // 获得自增主键，对于 Jdbc3KeyGenerator，这里啥也没干
      generateKeys(parameterObject);
      // 创建 BoundSql 对象
      boundSql = mappedStatement.getBoundSql(parameterObject);
    }

    this.boundSql = boundSql;

    // 创建 ParameterHandler 对象
    this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
    // 创建 ResultSetHandler 对象
    this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
  }

  /**
   * 获取 boundSql
   */
  @Override
  public BoundSql getBoundSql() {
    return boundSql;
  }

  /**
   * 获取 parameterHandler
   */
  @Override
  public ParameterHandler getParameterHandler() {
    return parameterHandler;
  }

  /**
   * 准备阶段
   */
  @Override
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
    ErrorContext.instance().sql(boundSql.getSql());
    Statement statement = null;
    try {
      // 创建 Statement 对象
      statement = instantiateStatement(connection);
      // 设置超时时间
      setStatementTimeout(statement, transactionTimeout);
      // 设置 fetchSize
      setFetchSize(statement);
      return statement;
    } catch (SQLException e) {
      // 发生异常，进行关闭
      closeStatement(statement);
      throw e;
    } catch (Exception e) {
      // 发生异常，进行关闭
      closeStatement(statement);
      throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
    }
  }

  /**
   * 初始化 Statement
   */
  protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

  /**
   * 设置 Statement 超时时间
   */
  protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
    // 获得 queryTimeout
    Integer queryTimeout = null;
    if (mappedStatement.getTimeout() != null) {
      queryTimeout = mappedStatement.getTimeout();
    } else if (configuration.getDefaultStatementTimeout() != null) {
      queryTimeout = configuration.getDefaultStatementTimeout();
    }
    // 设置查询超时时间
    if (queryTimeout != null) {
      stmt.setQueryTimeout(queryTimeout);
    }
    // 设置事务超时时间
    StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
  }

  /**
   * 设置 FetchSize
   */
  protected void setFetchSize(Statement stmt) throws SQLException {
    // 获得 fetchSize 。非空，则进行设置
    Integer fetchSize = mappedStatement.getFetchSize();
    if (fetchSize != null) {
      stmt.setFetchSize(fetchSize);
      return;
    }
    // 获得 defaultFetchSize 。非空，则进行设置
    Integer defaultFetchSize = configuration.getDefaultFetchSize();
    if (defaultFetchSize != null) {
      stmt.setFetchSize(defaultFetchSize);
    }
  }

  /**
   * 关闭 statement
   */
  protected void closeStatement(Statement statement) {
    try {
      if (statement != null) {
        statement.close();
      }
    } catch (SQLException e) {
      //ignore
    }
  }

  /**
   * 获得自增主键
   */
  protected void generateKeys(Object parameter) {
    // 获得 KeyGenerator 对象
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    // 异常上下文记录一笔
    ErrorContext.instance().store();
    // 前置处理，创建自增编号到 parameter 中。如果是 Jdbc3KeyGenerator，这里啥也不做
    keyGenerator.processBefore(executor, mappedStatement, null, parameter);
    // 异常上下文记录一笔
    ErrorContext.instance().recall();
  }

}
