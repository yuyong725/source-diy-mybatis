package cn.javadog.sd.mybatis.executor.statement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.executor.keygen.Jdbc3KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.SelectKeyGenerator;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ResultSetType;
import cn.javadog.sd.mybatis.session.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;


/**
 * @author Clinton Begin
 * java.sql.Statement 的 StatementHandler 实现类
 */
public class SimpleStatementHandler extends BaseStatementHandler {

  public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
  }

  @Override
  public int update(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    Object parameterObject = boundSql.getParameterObject();
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    int rows;
    // 如果是 Jdbc3KeyGenerator 类型
    if (keyGenerator instanceof Jdbc3KeyGenerator) {
      // <1.1> 执行写操作
      statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
      // <2.2> 获得更新数量
      rows = statement.getUpdateCount();
      // <1.3> 执行 keyGenerator 的后置处理逻辑
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
      // 如果是 SelectKeyGenerator 类型
    } else if (keyGenerator instanceof SelectKeyGenerator) {
      // <2.1> 执行写操作
      statement.execute(sql);
      // <2.2> 获得更新数量
      rows = statement.getUpdateCount();
      // <2.3> 执行 keyGenerator 的后置处理逻辑
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
    } else {
      // <3.1> 执行写操作
      statement.execute(sql);
      // <3.2> 获得更新数量
      rows = statement.getUpdateCount();
    }
    return rows;
  }

  @Override
  public void batch(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    // 添加到批处理
    statement.addBatch(sql);
  }

  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    String sql = boundSql.getSql();
    // <1> 执行查询
    statement.execute(sql);
    // <2> 处理返回结果
    return resultSetHandler.<E>handleResultSets(statement);
  }

  @Override
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    // <1> 执行查询
    statement.execute(sql);
    // <2> 处理返回的 Cursor 结果
    return resultSetHandler.<E>handleCursorResultSets(statement);
  }

  /**
   * 创建 java.sql.Statement 对象
   */
  @Override
  protected Statement instantiateStatement(Connection connection) throws SQLException {
    if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
      return connection.createStatement();
    } else {
      return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
    }
  }

  @Override
  public void parameterize(Statement statement) throws SQLException {
    // N/A
  }

}
