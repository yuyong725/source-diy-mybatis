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
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;


/**
 * @author 余勇
 * @date 2019-12-16 20:37
 *
 * java.sql.Statement 的 StatementHandler 实现类
 */
public class SimpleStatementHandler extends BaseStatementHandler {

  /**
   * 构造
   */
  public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
  }

  /**
   * 执行更新操作
   */
  @Override
  public int update(Statement statement) throws SQLException {
    // 获取SQL
    String sql = boundSql.getSql();
    // 获取参数
    Object parameterObject = boundSql.getParameterObject();
    // 获取主键生成器
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    int rows;
    // 如果是 Jdbc3KeyGenerator 类型
    if (keyGenerator instanceof Jdbc3KeyGenerator) {
      // 执行写操作
      statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
      // 获得更新数量
      rows = statement.getUpdateCount();
      // 执行 keyGenerator 的后置处理逻辑
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
    }
    // 如果是 SelectKeyGenerator 类型
    else if (keyGenerator instanceof SelectKeyGenerator) {
      // 执行写操作
      statement.execute(sql);
      // 获得更新数量
      rows = statement.getUpdateCount();
      // 执行 keyGenerator 的后置处理逻辑
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
    } else {
      // 执行写操作
      statement.execute(sql);
      // 获得更新数量
      rows = statement.getUpdateCount();
    }
    return rows;
  }

  /**
   * 批处理
   */
  @Override
  public void batch(Statement statement) throws SQLException {
    // 获取sql
    String sql = boundSql.getSql();
    // 添加到批处理
    statement.addBatch(sql);
  }

  /**
   * 查询
   */
  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    String sql = boundSql.getSql();
    // 执行查询
    statement.execute(sql);
    // 处理返回结果
    return resultSetHandler.<E>handleResultSets(statement);
  }

  /**
   * 查询游标
   */
  @Override
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    // 执行查询
    statement.execute(sql);
    // 处理返回的 Cursor 结果
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

  /**
   * 设置参数，这种statement执行的是静态语句，不需要处理占位符
   */
  @Override
  public void parameterize(Statement statement) throws SQLException {
    // N/A
  }

}
